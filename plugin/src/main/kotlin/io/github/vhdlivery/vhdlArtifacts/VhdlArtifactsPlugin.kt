package io.github.vhdlivery.vhdlArtifacts

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Zip

class VhdlArtifactsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("vhdlArtifacts", VhdlArtifactsExtension::class.java, project.objects)
        extension.moduleName.convention(project.name)
        extension.libraryName.convention(project.parent?.name ?: "work")
        project.pluginManager.apply("distribution")
        project.pluginManager.apply("maven-publish")

        val rtl = vhdlConfiguration(project, "rtl", true)
        val sim = vhdlConfiguration(project, "sim", false)
        val modules = rtl.moduleCoordinates().zip(sim.moduleCoordinates()) { a, b -> (a + b).distinct() }
        val conflicts = project.tasks.register("checkVhdlDependencyConflicts", CheckVhdlConflicts::class.java) {
            it.group = "VHDL Artifacts Plugin"
            it.description = "Check RTL and simulation dependency versions for conflicts"
            it.modules.set(modules)
        }
        // One writer owns the shared library/module tree. Both entry points use the
        // same union so neither task can delete the other configuration's sources.
        val extraction = project.tasks.register("unzipVhdlArtifacts", ExtractVhdlArtifacts::class.java) {
            it.group = "VHDL Artifacts Plugin"
            it.description = "Extract RTL and simulation dependencies into build/dependency"
            it.archives.from(rtl, sim)
            it.destination.set(project.layout.buildDirectory.dir("dependency"))
            it.dependsOn(conflicts)
        }
        listOf("unzipRtlArtifacts", "unzipSimArtifacts").forEach { name ->
            project.tasks.register(name) {
                it.group = "VHDL Artifacts Plugin"
                it.description = "Synchronize shared VHDL dependencies into build/dependency"
                it.dependsOn(extraction)
            }
        }

        val distributions = project.extensions.getByType(DistributionContainer::class.java)
        distributions.create("src") { distribution ->
            distribution.distributionBaseName.set(project.name)
            distribution.distributionClassifier.set("src")
            distribution.contents { content ->
                content.into(extension.libraryName.zip(extension.moduleName) { library, module -> "$library/$module" }) {
                    it.into("src") { sources -> sources.from(project.layout.projectDirectory.dir("src")) }
                    it.from(project.layout.projectDirectory.file("readme.md"))
                    it.from(project.layout.projectDirectory.file("README.md"))
                    it.includeEmptyDirs = false
                }
            }
        }
        val srcZip = project.tasks.named("srcDistZip", Zip::class.java)

        val releaseVersion = project.provider { project.version.toString() }
        project.tasks.register("releaseCheck", CheckVhdlRelease::class.java) {
            it.group = "VHDL Artifacts Plugin"
            it.description = "Require release versions for the project and all resolved dependencies"
            it.releaseVersion.set(releaseVersion)
            it.modules.set(modules)
            it.publishing.set(false)
            it.dependsOn(conflicts)
        }
        val publicationCheck = project.tasks.register("checkVhdlPublication", CheckVhdlRelease::class.java) {
            it.releaseVersion.set(releaseVersion)
            it.modules.set(modules)
            it.publishing.set(true)
            it.dependsOn(conflicts)
        }
        project.tasks.withType(PublishToMavenRepository::class.java).configureEach { it.dependsOn(publicationCheck) }
        project.tasks.withType(PublishToMavenLocal::class.java).configureEach { it.dependsOn(publicationCheck) }

        // Keep only dependency values in the POM action, never a Configuration or Project.
        val pomDependencies = project.objects.listProperty(VhdlDependency::class.java)
        pomDependencies.set(project.provider {
            rtl.allDependencies.withType(ExternalModuleDependency::class.java).flatMap { dependency ->
                val artifacts = dependency.artifacts.toList()
                val exclusions = if (!dependency.isTransitive) listOf("*" to "*") else
                    dependency.excludeRules.map { (it.group ?: "*") to (it.module ?: "*") }
                if (artifacts.isEmpty()) listOf(VhdlDependency(
                    dependency.group.orEmpty(), dependency.name, dependency.version.orEmpty(), "src", "zip", exclusions
                )) else artifacts.map { artifact ->
                    VhdlDependency(dependency.group.orEmpty(), dependency.name, dependency.version.orEmpty(),
                        artifact.classifier.orEmpty(), artifact.type, exclusions)
                }
            }
        })
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications.create("srcArtifact", MavenPublication::class.java) { publication ->
            publication.artifactId = project.name
            publication.artifact(srcZip)
            publication.pom.withXml { xml ->
                val dependencies = xml.asNode().appendNode("dependencies")
                pomDependencies.get().forEach { dependency ->
                    dependencies.appendNode("dependency").apply {
                        appendNode("groupId", dependency.groupId)
                        appendNode("artifactId", dependency.artifactId)
                        appendNode("version", dependency.version)
                        if (dependency.classifier.isNotEmpty()) appendNode("classifier", dependency.classifier)
                        appendNode("type", dependency.fileType)
                        if (dependency.exclusions.isNotEmpty()) {
                            val exclusions = appendNode("exclusions")
                            dependency.exclusions.forEach { (group, module) ->
                                exclusions.appendNode("exclusion").apply {
                                    appendNode("groupId", group)
                                    appendNode("artifactId", module)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
