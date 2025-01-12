package com.github.vhdlivery.vhdlArtifacts

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.distribution.DistributionContainer
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
            it.group = "VHDL Artifacts Management"
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

        // Configuration
        val rtlModSrcConfig = createConfig(
            name = "rtlModSrc",
            description = "Custom Configuration for VHDL module source code artifacts used for RTL",
            transitive = true
        )

        val rtlModSrcCfg = VhdlConfiguration()
        project.afterEvaluate {
            rtlModSrcCfg.resolve(project, rtlModSrcConfig)
        }


        // Distribution
        val distributions = project.extensions.getByType(DistributionContainer::class.java)

        val modSrcDistribution = distributions.create("modSrc") { dist ->
            dist.distributionBaseName.set(project.name)
            dist.distributionClassifier.set("modSrc")
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

        val modSrcDistZip = project.tasks.named(modSrcDistribution.name + "DistZip", Zip::class.java) {
            it.archiveFileName.set("${modSrcDistribution.distributionBaseName.get()}-" +
                    "${project.version}-${modSrcDistribution.distributionClassifier.get()}.zip")
            it.destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
        }

        // Publication
        project.extensions.configure<PublishingExtension>("publishing") { publish ->
            publish.publications { publications ->
                publications.create("vhdlArtifacts", MavenPublication::class.java) { publication ->
                    project.afterEvaluate {
                        publication.groupId = project.group.toString()
                        publication.artifactId = project.name.lowercase()
                        publication.version = project.version.toString()
                        publication.artifact(modSrcDistZip)
                    }

                    publication.pom.withXml { xml ->
                        println("Adding transitive dependencies to pom file...")
                        val dependenciesNode = xml.asNode().appendNode("dependencies")
                        rtlModSrcCfg.dependencies.forEach { dependency ->
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
