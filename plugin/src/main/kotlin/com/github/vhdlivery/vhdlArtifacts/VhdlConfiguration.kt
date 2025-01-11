package com.github.vhdlivery.vhdlArtifacts

data class VhdlConfiguration(
    val dependencies: MutableList<VhdlDependency> = mutableListOf()
) {

    // Add a dependency from a Gradle Dependency object
    fun add(dependency: VhdlDependency) {
        dependencies.add(dependency)
    }

    // Get all dependencies as a List
    fun getAll(): List<VhdlDependency> {
        return dependencies
    }
}
