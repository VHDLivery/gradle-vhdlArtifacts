package com.github.vhdlivery.vhdlArtifacts

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

        // Apply required plugins
        project.pluginManager.apply("distribution")
        project.pluginManager.apply("maven-publish")

        // Register a task
        project.tasks.register("greeting") {
            it.group = "VHDL Artifacts Plugin"
            it.description = "Greeting from this Plugin"
            it.doLast {
                println("Hello from plugin 'com.github.vhdlivery.vhdlArtifacts'")
            }
        }

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

        val resolvedRtlConfig = VhdlConfiguration()
        val resolvedSimConfig = VhdlConfiguration()
        project.afterEvaluate {
            resolvedRtlConfig.resolve(project, rtlConfig)
            resolvedSimConfig.resolve(project, simConfig)
        }

        fun unzipArtifacts(config: VhdlConfiguration, outputDir : Provider<Directory>) {
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

            println("Unzipping rtl dependencies into 'build/dependency'")
            unzipArtifacts(resolvedRtlConfig, outputDir)
        }

        val unzipSimArtifacts = project.tasks.register("unzipSimArtifacts") {
            it.group = "VHDL Artifacts Plugin"
            it.description = "Unzip artifacts of config 'sim' into 'build/dependency'"

            val outputDir = project.layout.buildDirectory.dir("dependency")

            it.inputs.files(resolvedSimConfig.artifacts)
            it.outputs.dir(outputDir)

            println("Unzipping sim dependencies into 'build/dependency'")
            unzipArtifacts(resolvedSimConfig, outputDir)
        }

        // Distribution
        val distributions = project.extensions.getByType(DistributionContainer::class.java)

        val srcDistribution = distributions.create("src") { dist ->
            dist.distributionBaseName.set(project.name)
            dist.distributionClassifier.set("src")
            dist.contents{ content ->
                // Into libName/modName
                content.into("${project.parent?.name ?: "work"}/${project.name}") {
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
                    project.afterEvaluate {
                        publication.groupId = project.group.toString()
                        publication.artifactId = project.name.lowercase()
                        publication.version = project.version.toString()
                        publication.artifact(srcDistZip)
                    }

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
