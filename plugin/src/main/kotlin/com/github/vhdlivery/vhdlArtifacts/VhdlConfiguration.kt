package com.github.vhdlivery.vhdlArtifacts

data class VhdlConfiguration(
    val dependencies: List<VhdlDependency> = mutableListOf()
) {
    // Add a dependency to the configuration
    fun addDependency(dependency: VhdlDependency) {
        (dependencies as MutableList).add(dependency)
    }

    // Add a dependency from a Gradle Dependency object
    fun addDependency(dependency: org.gradle.api.artifacts.Dependency) {
        addDependency(VhdlDependency(dependency))
    }

    // Get all dependencies as a List
    fun getAll(): List<VhdlDependency> {
        return dependencies
    }
}
