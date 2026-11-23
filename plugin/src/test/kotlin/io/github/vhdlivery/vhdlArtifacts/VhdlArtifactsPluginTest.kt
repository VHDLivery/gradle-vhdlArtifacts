package io.github.vhdlivery.vhdlArtifacts

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VhdlArtifactsPluginTest {
    @TempDir lateinit var directory: Path
    private val root get() = directory.toFile()

    private fun project(dependencies: String = "", version: String = "1.0-RELEASE") {
        root.resolve("settings.gradle").writeText("rootProject.name = 'consumer'")
        root.resolve("build.gradle").writeText("""
            plugins { id 'io.github.vhdlivery.vhdlArtifacts' }
            group = 'example'
            version = '$version'
            repositories { maven { url = uri('repo') } }
            dependencies { $dependencies }
            publishing { repositories { maven { url = uri('published') } } }
        """.trimIndent())
    }

    private fun artifact(name: String, version: String = "1.0-RELEASE", dependencies: String = "", source: String = "$name.vhd") {
        val folder = root.resolve("repo/example/$name/$version").apply { mkdirs() }
        folder.resolve("$name-$version.pom").writeText("""
            <project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>
            <artifactId>$name</artifactId><version>$version</version><packaging>pom</packaging>
            <dependencies>$dependencies</dependencies></project>
        """.trimIndent())
        ZipOutputStream(folder.resolve("$name-$version-src.zip").outputStream()).use {
            it.putNextEntry(ZipEntry("$name-$version-src/work/$name/src/$source"))
            it.write("-- $name".toByteArray())
            it.closeEntry()
        }
    }

    private fun dependency(name: String, version: String = "1.0-RELEASE") = """
        <dependency><groupId>example</groupId><artifactId>$name</artifactId>
        <version>$version</version><classifier>src</classifier><type>zip</type></dependency>
    """.trimIndent()

    private fun runner(vararg tasks: String) = GradleRunner.create()
        .withProjectDir(root).withPluginClasspath()
        .withArguments("--configuration-cache", *tasks, "--stacktrace")

    @Test fun `help and dry run neither resolve nor extract dependencies`() {
        project("rtl 'example:missing:1.0-RELEASE'")
        runner("help").build()
        // Configuration cache serialization may resolve task input providers even in a dry run.
        runner("unzipRtlArtifacts", "--dry-run", "--no-configuration-cache").build()
        assertFalse(root.resolve("build/dependency").exists())
    }

    @Test fun `classified ZIPs resolve transitively and extraction removes stale sources`() {
        artifact("child")
        artifact("parent", dependencies = dependency("child"))
        project("rtl 'example:parent:1.0-RELEASE'")
        runner("unzipRtlArtifacts").build()
        assertTrue(root.resolve("build/dependency/work/child/src/child.vhd").isFile)
        assertTrue(root.resolve("build/dependency/work/parent/src/parent.vhd").isFile)
        project("rtl 'example:child:1.0-RELEASE'")
        runner("unzipRtlArtifacts").build()
        assertFalse(root.resolve("build/dependency/work/parent").exists())
        root.resolve("build/dependency").deleteRecursively()
        val result = runner("unzipRtlArtifacts").build()
        assertTrue(result.output.contains("Reusing configuration cache"))
        assertTrue(root.resolve("build/dependency/work/child/src/child.vhd").isFile)
    }

    @Test fun `simulation dependencies remain nontransitive`() {
        artifact("child")
        artifact("parent", dependencies = dependency("child"))
        project("sim 'example:parent:1.0-RELEASE'")
        runner("unzipSimArtifacts").build()
        assertTrue(root.resolve("build/dependency/work/parent/src/parent.vhd").exists())
        assertFalse(root.resolve("build/dependency/work/child").exists())
    }

    @Test fun `RTL and simulation share one library path without duplicate sources`() {
        artifact("shared")
        artifact("rtlOnly")
        artifact("simOnly")
        project("rtl 'example:shared:1.0-RELEASE'; sim 'example:shared:1.0-RELEASE'; " +
            "rtl 'example:rtlOnly:1.0-RELEASE'; sim 'example:simOnly:1.0-RELEASE'")
        runner("unzipSimArtifacts", "unzipRtlArtifacts", "--parallel").build()
        val sources = root.resolve("build/dependency").walkTopDown().filter { it.extension == "vhd" }.toList()
        kotlin.test.assertEquals(setOf("shared.vhd", "rtlOnly.vhd", "simOnly.vhd"), sources.map { it.name }.toSet())
        kotlin.test.assertEquals(3, sources.size)
        assertTrue(root.resolve("build/dependency/work/shared/src/shared.vhd").isFile)
        assertFalse(root.resolve("build/dependency/rtl").exists())
        assertFalse(root.resolve("build/dependency/sim").exists())

        // Moving a dependency between configurations keeps its established compile path.
        project("sim 'example:shared:1.0-RELEASE'; rtl 'example:rtlOnly:1.0-RELEASE'")
        runner("unzipRtlArtifacts").build()
        assertTrue(root.resolve("build/dependency/work/shared/src/shared.vhd").isFile)
        assertTrue(root.resolve("build/dependency/work/rtlOnly/src/rtlOnly.vhd").isFile)
        assertFalse(root.resolve("build/dependency/work/simOnly").exists())
        assertTrue(runner("unzipRtlArtifacts").build().output.contains("Reusing configuration cache"))
    }

    @Test fun `release check rejects transitive prereleases and publication is gated`() {
        artifact("child", "1.0-RC1")
        artifact("parent", dependencies = dependency("child", "1.0-RC1"))
        project("rtl 'example:parent:1.0-RELEASE'")
        val result = runner("publish").buildAndFail()
        assertTrue(result.output.contains("example:child:1.0-RC1"))
        assertFalse(root.resolve("published/example/consumer").exists())
    }

    @Test fun `snapshot publication remains available`() {
        project(version = "1.0-SNAPSHOT")
        root.resolve("src").mkdirs()
        root.resolve("src/module.vhd").writeText("-- module")
        runner("publish").build()
        assertTrue(root.resolve("published/example/consumer/1.0-SNAPSHOT").isDirectory)
        runner("releaseCheck").buildAndFail()
    }

    @Test fun `publication round trips source ZIP and dependency metadata`() {
        artifact("child")
        project("rtl('example:child:1.0-RELEASE') { exclude group: 'example', module: 'excluded' }")
        root.resolve("src").mkdirs()
        root.resolve("src/module.vhd").writeText("-- module")
        root.resolve("README.md").writeText("Module documentation")
        runner("publish").build()
        assertTrue(runner("publish").build().output.contains("Reusing configuration cache"))
        val published = root.resolve("published/example/consumer/1.0-RELEASE")
        val pom = published.resolve("consumer-1.0-RELEASE.pom").readText()
        assertTrue(pom.contains("<classifier>src</classifier>"))
        assertTrue(pom.contains("<exclusions>"))
        val consumer = root.resolve("downstream").apply { mkdirs() }
        consumer.resolve("settings.gradle").writeText("rootProject.name = 'downstream'")
        consumer.resolve("build.gradle").writeText("""
            plugins { id 'io.github.vhdlivery.vhdlArtifacts' }
            repositories { maven { url = uri('../published') }; maven { url = uri('../repo') } }
            dependencies { rtl 'example:consumer:1.0-RELEASE' }
        """.trimIndent())
        GradleRunner.create().withProjectDir(consumer).withPluginClasspath()
            .withArguments("unzipRtlArtifacts", "--configuration-cache", "--stacktrace").build()
        assertTrue(consumer.resolve("build/dependency/work/consumer/src/module.vhd").exists())
        assertTrue(consumer.resolve("build/dependency/work/consumer/README.md").exists())
        assertTrue(consumer.resolve("build/dependency/work/child/src/child.vhd").exists())
    }

    @Test fun `removing all dependencies empties extraction output`() {
        artifact("child")
        project("rtl 'example:child:1.0-RELEASE'")
        runner("unzipRtlArtifacts").build()
        project()
        runner("unzipRtlArtifacts").build()
        assertFalse(root.resolve("build/dependency/work/child").exists())
    }

    @Test fun `dependency exclusions survive ZIP selection`() {
        artifact("parent", dependencies = dependency("missing"))
        project("rtl('example:parent:1.0-RELEASE') { exclude group: 'example', module: 'missing' }")
        runner("unzipRtlArtifacts").build()
        assertTrue(root.resolve("build/dependency/work/parent/src/parent.vhd").exists())
    }

    @Test fun `source packaging works without resolving missing dependencies`() {
        project("rtl 'example:missing:1.0-RELEASE'")
        root.resolve("src").mkdirs()
        root.resolve("src/module.vhd").writeText("-- module")
        runner("srcDistZip").build()
        assertTrue(root.resolve("build/distributions/consumer-1.0-RELEASE-src.zip").isFile)
    }

    @Test fun `conflicting RTL and simulation versions fail before extraction`() {
        artifact("child", "1.0-RELEASE")
        artifact("child", "2.0-RELEASE")
        project("rtl 'example:child:1.0-RELEASE'; sim 'example:child:2.0-RELEASE'")
        val result = runner("unzipRtlArtifacts").buildAndFail()
        assertTrue(result.output.contains("Dependency version conflicts"))
        assertFalse(root.resolve("build/dependency/work").exists())
    }
}
