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
            it.group = "VHDL Artifacts"
            it.description = "Greeting from this Plugin"
            it.doLast {
                println("Hello from plugin 'com.github.vhdlivery.vhdlArtifacts'")
            }
        }

        // Custom Configurations
        var rtlModSrcConfig = project.configurations.create("rtlModSrc") {
            it.isCanBeResolved = true
            it.isCanBeConsumed = false
            it.isTransitive = true
            it.description = "Custom Configuration for VHDL module source code packages used for RTL"
        }
        fun resolveImplicitArtifacts(config: org.gradle.api.artifacts.Configuration) {
            val classifier = "src"
            val fileType = "zip"

            if (config.allDependencies.isEmpty()) {
                println("Configuration '${config.name}' has no dependencies to process.")
                return
            }

            // Collect the dependencies to add first
            val dependenciesToAdd = mutableListOf<org.gradle.api.artifacts.Dependency>()
            config.allDependencies.forEach { dependency ->
                dependenciesToAdd.add(project.dependencies.create(
                    "${dependency.group}:${dependency.name}:${dependency.version}:${classifier}@${fileType}"
                ))
            }

            // Add the dependencies to the configuration after the iteration
            dependenciesToAdd.forEach { dep ->
                config.dependencies.add(dep)
                println("Added implicit dependency: $dep")
            }

        }

        fun unzipResolvedArtifacts(config: org.gradle.api.artifacts.Configuration) {
            rtlModSrcConfig.resolve().forEach { artifact ->
                println("Unzipping $artifact")
                // Create a copy task to unzip the artifact contents
                project.copy {
                    // Specify the ZIP file to unzip
                    it.from(project.zipTree(artifact.path))
                    // Define the destination directory
                    it.into(project.layout.buildDirectory.dir("dependency"))
                    // Do not include empty directories
                    it.includeEmptyDirs = false
                }
            }
        }

        fun printArtifacts(config: org.gradle.api.artifacts.Configuration) {
            // Resolve and print resolved artifacts
            println("Resolved artifacts in configuration '${config.name}'...")
            try {
                config.resolve().forEach { artifact ->
                    println("- ${artifact}")
                }
            } catch (e: Exception) {
                println("Failed to resolve artifacts in configuration '${config.name}': ${e.message}")
            }
        }

        // Task to unzip all resolved artifacts and implicit artifacts of dependencies
        project.tasks.register("getRtlModSrcDependencies") {

            // Add task to tasks with description
            it.group = "VHDL Artifacts"
            it.description = "Retrieve all resolved artifacts of 'rtlModSrc' configuration"

            resolveImplicitArtifacts(rtlModSrcConfig)
            unzipResolvedArtifacts(rtlModSrcConfig)
            printArtifacts(rtlModSrcConfig)
        }

        project.tasks.register("printRtlSrcArtifacts") {
            it.group = "VHDL Artifacts"
            it.description = "Print resolved artifacts of config 'rtlModSrc'"

            printArtifacts(rtlModSrcConfig)
        }

        // Custom Distributions
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

        // Custom Distribution Packaging
        project.tasks.named(modSrcDistribution.name + "DistZip", Zip::class.java) {
            it.group = "VHDL Artifacts"
            it.description = "Packages the sources code as a module into a ZIP file for publishing or distribution."
            it.archiveFileName.set("${modSrcDistribution.distributionBaseName.get()}-" +
                    "${project.version}-${modSrcDistribution.distributionClassifier.get()}.zip")
            it.destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
        }

        val rtlModSrcConfigProvider = project.provider {
            rtlModSrcConfig
        }

        project.extensions.configure<PublishingExtension>("publishing") { publish ->
            publish.publications { publications ->
                publications.create("vhdlArtifacts", MavenPublication::class.java) { publication ->
                    publication.groupId = project.group.toString()
                    publication.artifactId = project.name.lowercase()
                    publication.version = project.version.toString()
                    publication.artifact(project.tasks.named("modSrcDistZip"))

                    var dependencies = VhdlConfiguration()
                    rtlModSrcConfig.dependencies.forEach() { dependency ->
                        //FIXME: No dependency found
                        println("Dependency: $dependency")
                        dependencies.addDependency(dependency)
                    }
                    println(dependencies)

                    publication.pom.withXml { xml ->
                        val dependenciesNode = xml.asNode().appendNode("dependencies")
                        dependencies.getAll().forEach { dependency ->
                            println("Added dependency $dependency")
                            dependenciesNode.appendNode("dependency").apply {
                                appendNode("groupId", dependency.groupId)
                                appendNode("artifactId", dependency.artifactId)
                                appendNode("version", dependency.version)
                                appendNode("classifier", "src")
                                appendNode("type", "zip")
                            }
                        }
                    }

                }
            }
        }

    }
}
