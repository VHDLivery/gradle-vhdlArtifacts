plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "io.github.vhdlivery.vhdlArtifacts"
version = "1.2.1"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    // Use the Kotlin JUnit 5 integration.
    testImplementation(gradleTestKit())
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    // Define the plugin
    website = "https://github.com/VHDLivery/gradle-vhdlArtifacts"
    vcsUrl = "https://github.com/VHDLivery/gradle-vhdlArtifacts"
    plugins {
        create("VhdlArtifactsPlugin") {
            id = "${group}"
            displayName = "VHDL Artifacts Plugin"
            description = "Plugin to manage artifacts of VHDL source code for FPGA development"
            tags = listOf("vhdl", "fpga", "embedded")
            implementationClass = "${group}.VhdlArtifactsPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/VHDLivery/${rootProject.name}")
            credentials {
                username = System.getenv("GITHUB_USER")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "512m"
}

// Reject accidental publication of a tag that does not match the plugin version.
tasks.register("verifyReleaseTag") {
    group = "verification"
    description = "Check that the release tag matches the plugin version"
    val expectedTag = "v${project.version}"
    val suppliedTag = providers.gradleProperty("releaseTag")
        .orElse(providers.environmentVariable("GITHUB_REF_NAME"))
        .orElse("")
    inputs.property("expectedTag", expectedTag)
    inputs.property("releaseTag", suppliedTag)
    doLast {
        val actual = suppliedTag.get()
        check(actual == expectedTag) {
            "Release tag '$actual' must match plugin version: $expectedTag"
        }
    }
}
