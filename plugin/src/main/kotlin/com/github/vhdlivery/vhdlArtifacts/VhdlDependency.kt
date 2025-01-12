package com.github.vhdlivery.vhdlArtifacts

import org.gradle.api.artifacts.ResolvedDependency

class VhdlDependency (
    val groupId: String,
    val artifactId: String,
    val version: String,
    val classifier: String,
    val fileType: String,
){
    constructor(dependency: ResolvedDependency, classifier : String, fileType : String) : this(
        groupId = dependency.moduleGroup,
        artifactId = dependency.moduleName,
        version = dependency.moduleVersion,
        classifier = classifier,
        fileType = fileType
    )
}