package com.github.logicllama.gradle.vhdl

import org.gradle.api.Project
import org.gradle.api.Plugin

/**
 * A simple 'hello world' plugin.
 */
class VhdlPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        // Register a task
        project.tasks.register("greeting") { task ->
            task.doLast {
                println("Hello from plugin 'com.github.logicllama.gradle.vhdl'")
            }
        }
    }
}
