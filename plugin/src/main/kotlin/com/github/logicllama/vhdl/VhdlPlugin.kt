package com.github.logicllama.vhdl

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.tasks.bundling.Zip

/**
 * A simple 'hello world' plugin.
 */
class VhdlPlugin: Plugin<Project> {
    override fun apply(project: Project) {

        // Apply required plugins
        project.pluginManager.apply("distribution")
//        project.pluginManager.apply("maven-publish")

        // Register a task
        project.tasks.register("greeting") {
            it.group = "VHDL"
            it.description = "Greeting from this Plugin"
            it.doLast {
                println("Hello from plugin 'com.github.logicllama.vhdl'")
            }
        }

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
            // TODO: Add info using project.logger.lifecycle("Lorem Ipsum")
        }

        val libSrcDistribution = distributions.create("libSrc") {dist ->
            dist.distributionBaseName.set(project.name)
            dist.distributionClassifier.set("libSrc")
            dist.contents{ content ->
                // Into libName/modName
                content.into("${project.parent?.name ?: "work"}") {
                    // TODO: Add library sources
                    // Add the readme.md file to the root of the distribution
                    it.from("readme.md")
                    it.includeEmptyDirs = false
                }
            }
        }

        project.tasks.named(modSrcDistribution.name + "DistZip", Zip::class.java) {
            it.group = "VHDL"
            it.description = "Packages the sources code as a module into a ZIP file for publishing or distribution."
            it.archiveFileName.set("${modSrcDistribution.distributionBaseName.get()}-" +
                    "${project.version}-${modSrcDistribution.distributionClassifier.get()}.zip")
            it.destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
        }

        project.tasks.named(libSrcDistribution.name + "DistZip", Zip::class.java) {
            it.group = "VHDL"
            it.description = "Packages the sources code as a library into a ZIP file for publishing or distribution."
            it.archiveFileName.set("${libSrcDistribution.distributionBaseName.get()}" +
                    "-${project.version}-${libSrcDistribution.distributionClassifier.get()}.zip")
            it.destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
        }

        // Configure maven-publish
//        project.afterEvaluate {
//            project.extensions.configure<org.gradle.api.publish.PublishingExtension>("publishing") {
//                publications {
//                    create("mavenJava", org.gradle.api.publish.maven.MavenPublication::class.java) {
//                        from(project.components.getByName("java"))
//                    }
//                }
//            }
//        }
    }
}
