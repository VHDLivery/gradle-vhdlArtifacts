package com.github.logicllama.vhdl

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'com.github.logicllama.vhdl' plugin.
 */
class VhdlPluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.logicllama.vhdl")

        // Verify the result
        assertNotNull(project.tasks.findByName("greeting"))
    }
}
