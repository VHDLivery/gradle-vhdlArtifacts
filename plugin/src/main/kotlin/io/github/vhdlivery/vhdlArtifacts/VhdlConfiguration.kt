package io.github.vhdlivery.vhdlArtifacts

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.provider.Provider

internal fun vhdlConfiguration(project: Project, name: String, transitive: Boolean): Configuration =
    project.configurations.create(name) { configuration ->
        configuration.isCanBeResolved = true
        configuration.isCanBeConsumed = false
        configuration.isTransitive = transitive
        configuration.description = "VHDL source ZIP dependencies for $name"
        // Configure the existing dependency, preserving exclusions and transitivity.
        configuration.withDependencies { dependencies ->
            dependencies.withType(ExternalModuleDependency::class.java).forEach { dependency ->
                if (dependency.artifacts.isEmpty()) {
                    dependency.artifact { artifact ->
                        artifact.classifier = "src"
                        artifact.type = "zip"
                        artifact.extension = "zip"
                    }
                }
            }
        }
    }

internal fun Configuration.moduleCoordinates(): Provider<List<String>> =
    incoming.resolutionResult.rootComponent.map { root ->
        val modules = linkedSetOf<String>()
        val visited = mutableSetOf<org.gradle.api.artifacts.component.ComponentIdentifier>()
        fun visit(component: org.gradle.api.artifacts.result.ResolvedComponentResult) {
            if (!visited.add(component.id)) return
            (component.id as? ModuleComponentIdentifier)?.let {
                modules.add("${it.group}:${it.module}:${it.version}")
            }
            component.dependencies.forEach { dependency ->
                when (dependency) {
                    is org.gradle.api.artifacts.result.ResolvedDependencyResult -> visit(dependency.selected)
                    is org.gradle.api.artifacts.result.UnresolvedDependencyResult -> throw dependency.failure
                }
            }
        }
        visit(root)
        modules.toList()
    }
