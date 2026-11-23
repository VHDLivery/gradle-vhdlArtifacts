package io.github.vhdlivery.vhdlArtifacts

import java.io.Serializable

internal data class VhdlDependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val classifier: String,
    val fileType: String,
    val exclusions: List<Pair<String, String>>
) : Serializable
