package io.github.vhdlivery.vhdlArtifacts

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

class VhdlArtifactsPlugin: Plugin<Project> {

    override fun apply(project: Project) {

        val extension = project.extensions.create("vhdlArtifacts", VhdlArtifactsExtension::class.java, project.objects)

        // Set default values for properties if not provided
        extension.moduleName.convention(project.name) // Default to project name
        extension.libraryName.convention(project.parent?.name ?: "work") // Default to "work" if parent is null

        project.afterEvaluate {
            project.logger.lifecycle("Library = ${extension.libraryName.get()}")
        }

        // Apply required plugins
        project.pluginManager.apply("distribution")
        project.pluginManager.apply("maven-publish")

        fun createConfig(name : String, description : String, transitive : Boolean) : org.gradle.api.artifacts.Configuration {
            return project.configurations.create(name) {
                it.isCanBeResolved = true
                it.isCanBeConsumed = false
                it.isTransitive = transitive
                it.description = description
            }
        }

        // Configurations
        val rtlConfig = createConfig(
            name = "rtl",
            description = "Custom Configuration for VHDL module source code artifacts used for RTL",
            transitive = true
        )

        val simConfig = createConfig(
            name = "sim",
            description = "Custom Configuration for VHDL module source code artifacts used for RTL",
            transitive = false
        )

        fun checkDependencyConflicts() {
            val rtlDeps = rtlConfig.resolvedConfiguration.resolvedArtifacts
            val simDeps = simConfig.resolvedConfiguration.resolvedArtifacts

            val allDeps = rtlDeps + simDeps
            val groupedByModule = allDeps.groupBy { "${it.moduleVersion.id.group}:${it.moduleVersion.id.name}" }

            val conflictingDeps = groupedByModule.filter { (_, versions) ->
                versions.map { it.moduleVersion.id.version }.toSet().size > 1
            }

            if (conflictingDeps.isNotEmpty()) {
                project.logger.lifecycle("Dependency version conflicts detected:")
                conflictingDeps.forEach { (module, versions) ->
                    project.logger.lifecycle("Module: $module")
                    versions.forEach { artifact ->
                        project.logger.lifecycle(
                            " - Version: ${artifact.moduleVersion.id.version}"
                        )
                    }
                }
                throw RuntimeException("Dependency conflicts detected. Resolve them to proceed.")
            }
        }

        val resolvedRtlConfig = VhdlConfiguration("rtl")
        val resolvedSimConfig = VhdlConfiguration("sim")
        project.afterEvaluate {
            resolvedRtlConfig.resolve(project, rtlConfig)
            resolvedSimConfig.resolve(project, simConfig)
            checkDependencyConflicts()
        }

        fun unzipArtifacts(config: VhdlConfiguration, outputDir : Provider<Directory>) {
            println("Unzipping ${config.name} dependencies into 'build/dependency'")
            config.artifacts.forEach { artifact ->
                project.copy {
                    it.from(project.zipTree(artifact.path)) { copySpec ->
                        copySpec.eachFile { file ->
                            file.path = file.path.split("/").drop(1).joinToString("/")
                        }
                    }
                    it.into(outputDir)
                    it.includeEmptyDirs = false
                }
            }
        }

        val unzipRtlArtifacts = project.tasks.register("unzipRtlArtifacts") {
            it.group = "VHDL Artifacts Plugin"
            it.description = "Unzip artifacts of config 'rtl' into 'build/dependency'"

            val outputDir = project.layout.buildDirectory.dir("dependency")

            it.inputs.files(resolvedRtlConfig.artifacts)
            it.outputs.dir(outputDir)

            unzipArtifacts(resolvedRtlConfig, outputDir)
        }

        val unzipSimArtifacts = project.tasks.register("unzipSimArtifacts") {
            it.group = "VHDL Artifacts Plugin"
            it.description = "Unzip artifacts of config 'sim' into 'build/dependency'"

            val outputDir = project.layout.buildDirectory.dir("dependency")

            it.inputs.files(resolvedSimConfig.artifacts)
            it.outputs.dir(outputDir)

            unzipArtifacts(resolvedSimConfig, outputDir)
        }

        project.afterEvaluate {
            // Distribution
            val distributions = project.extensions.getByType(DistributionContainer::class.java)

            val srcDistribution = distributions.create("src") { dist ->
                dist.distributionBaseName.set(project.name)
                dist.distributionClassifier.set("src")
                dist.contents{ content ->
                    val destPath = "${extension.libraryName.get()}/${extension.moduleName.get()}"
                    content.into(destPath) {
                        // Place the entire 'src' directory into the 'src' folder within the distribution
                        it.into("src") {
                            it.from("src")
                        }
                        // Add the readme.md file to the root of the distribution
                        it.from("readme.md")
                        it.includeEmptyDirs = false
                    }
                }
            }

            val srcDistZip = project.tasks.named(srcDistribution.name + "DistZip", Zip::class.java) {
                it.archiveFileName.set("${srcDistribution.distributionBaseName.get()}-" +
                        "${project.version}-${srcDistribution.distributionClassifier.get()}.zip")
                it.destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
            }

            // Publication
            project.extensions.configure<PublishingExtension>("publishing") { publish ->
                publish.publications { publications ->
                    publications.create("srcArtifact", MavenPublication::class.java) { publication ->
                        publication.groupId = project.group.toString()
                        publication.artifactId = project.name
                        publication.version = project.version.toString()
                        publication.artifact(srcDistZip)

                        publication.pom.withXml { xml ->
                            println("Adding transitive dependencies to pom file...")
                            val dependenciesNode = xml.asNode().appendNode("dependencies")
                            resolvedRtlConfig.dependencies.forEach { dependency ->
                                println("- ${dependency.groupId}:${dependency.artifactId}:${dependency.version}:src@zip")
                                dependenciesNode.appendNode("dependency").apply {
                                    appendNode("groupId", dependency.groupId)
                                    appendNode("artifactId", dependency.artifactId)
                                    appendNode("version", dependency.version)
                                    appendNode("classifier", dependency.classifier)
                                    appendNode("type", dependency.fileType)
                                }
                            }
                        }

                    }
                }
            }
        }

    }
}
