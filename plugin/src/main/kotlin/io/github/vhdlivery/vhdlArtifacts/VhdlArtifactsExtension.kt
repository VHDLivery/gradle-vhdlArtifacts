package io.github.vhdlivery.vhdlArtifacts

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class VhdlArtifactsExtension @Inject constructor(objects: ObjectFactory) {
    val moduleName: Property<String> = objects.property(String::class.java)
    val libraryName: Property<String> = objects.property(String::class.java)
}