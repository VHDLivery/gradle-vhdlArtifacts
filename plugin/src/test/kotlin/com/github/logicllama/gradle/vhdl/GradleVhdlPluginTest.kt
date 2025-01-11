package com.github.vhdlivery.vhdlArtifacts

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'com.github.vhdlivery.vhdlArtifacts' plugin.
 */
class VhdlArtifactsPluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.vhdlivery.vhdlArtifacts")

        // Verify the result
        assertNotNull(project.tasks.findByName("greeting"))
    }
}
