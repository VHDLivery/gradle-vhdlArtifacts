package io.github.vhdlivery.vhdlArtifacts

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import javax.inject.Inject

abstract class ExtractVhdlArtifacts : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val archives: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    @get:Inject abstract val archiveOperations: ArchiveOperations
    @get:Inject abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun extract() {
        // Sync also removes sources belonging to dependencies that have been removed.
        fileSystemOperations.sync { spec ->
            // ConfigurableFileCollection deduplicates an artifact present in both configurations.
            archives.files.sortedBy { it.name }.forEach { archive ->
                spec.from(archiveOperations.zipTree(archive)) { contents ->
                    contents.eachFile { file ->
                        file.path = file.path.substringAfter('/', "")
                    }
                }
            }
            spec.into(destination)
            spec.includeEmptyDirs = false
            spec.duplicatesStrategy = DuplicatesStrategy.FAIL
        }
    }
}

abstract class CheckVhdlConflicts : DefaultTask() {
    @get:Input abstract val modules: ListProperty<String>

    @TaskAction
    fun check() {
        val conflicts = modules.get().distinct().groupBy { it.substringBeforeLast(':') }
            .filterValues { it.size > 1 }
        if (conflicts.isNotEmpty()) {
            throw GradleException("Dependency version conflicts detected:\n" +
                conflicts.values.flatten().joinToString("\n"))
        }
    }
}

abstract class CheckVhdlRelease : DefaultTask() {
    @get:Input abstract val modules: ListProperty<String>
    @get:Input abstract val releaseVersion: Property<String>
    @get:Input abstract val publishing: Property<Boolean>

    @TaskAction
    fun check() {
        // Snapshot publishing remains available; explicit releaseCheck always validates.
        if (publishing.get() && !releaseVersion.get().endsWith("-RELEASE", ignoreCase = true)) return
        val errors = mutableListOf<String>()
        if (!releaseVersion.get().endsWith("-RELEASE", ignoreCase = true)) {
            errors.add("Project version must end with -RELEASE: ${releaseVersion.get()}")
        }
        modules.get().distinct().filterNot { it.endsWith("-RELEASE", ignoreCase = true) }
            .forEach { errors.add("Dependency version must end with -RELEASE: $it") }
        if (errors.isNotEmpty()) throw GradleException(errors.joinToString("\n"))
        logger.lifecycle("Project and dependency versions passed the release check.")
    }
}
