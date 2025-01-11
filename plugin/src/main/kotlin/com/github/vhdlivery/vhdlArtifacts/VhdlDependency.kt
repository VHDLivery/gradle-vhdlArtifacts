package com.github.vhdlivery.vhdlArtifacts

import org.gradle.api.artifacts.ResolvedDependency

class VhdlDependency (
    val groupId: String,
    val artifactId: String,
    val version: String,
){
    constructor(dependency: ResolvedDependency) : this(
        groupId = dependency.moduleGroup,
        artifactId = dependency.moduleName,
        version = dependency.moduleVersion
    )
}