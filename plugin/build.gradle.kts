plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "io.github.vhdlivery.vhdlArtifacts"
version = "1.2.0"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    // Use the Kotlin JUnit 5 integration.
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