package io.github.vhdlivery.vhdlArtifacts

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import java.io.File

class VhdlConfiguration (
   val name : String
) {

    val dependencies : MutableList<VhdlDependency> = mutableListOf()
    val artifacts : MutableList<File> = mutableListOf()
    val classifier = "src"
    val fileType = "zip"
    var transitive = false

    fun dependencies() : MutableList<VhdlDependency> {
        return dependencies
    }

    fun artifacts() : MutableList<File> {
        return artifacts
    }

    fun transitive() : Boolean {
        return transitive
    }

    fun resolve(project: Project, config: Configuration) {

        transitive = config.isTransitive()

        // Collect the artifacts to add
        val dependenciesToAdd = mutableListOf<org.gradle.api.artifacts.Dependency>()
        config.allDependencies.forEach { dependency ->
            dependenciesToAdd.add(project.dependencies.create(
                "${dependency.group}:${dependency.name}:${dependency.version}:${classifier}@${fileType}"
            ))
        }

        // Add the artifacts to the configuration after the iteration
        //println("Adding implicit artifacts to configuration '${config.name}'")
        dependenciesToAdd.forEach { dep ->
            config.dependencies.add(dep)
            //println("- ${dep}:${classifier}@${fileType}")
        }

        // Resolve artifacts
        config.resolve().forEach { artifact ->
            artifacts.add(artifact)
        }

        // Resolve dependencies
        config.resolvedConfiguration.firstLevelModuleDependencies.forEach { dependency ->
            dependencies.add(VhdlDependency(dependency, classifier, fileType))
        }
    }

}
