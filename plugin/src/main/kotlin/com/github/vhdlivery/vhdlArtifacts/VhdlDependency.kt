package com.github.vhdlivery.vhdlArtifacts

import org.gradle.api.artifacts.Dependency

class VhdlDependency (
    val groupId: String,
    val artifactId: String,
    val version: String,
){
    constructor(dependency: Dependency) : this(
        groupId = dependency.group.toString(),
        artifactId = dependency.name,
        version = dependency.version.toString()
    )
}