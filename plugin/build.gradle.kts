plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "io.github.vhdlivery.vhdlArtifacts"
version = "1.0.0"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    //implementation("org.gradle:gradle-maven-publish")
    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    // Define the plugin
    website = "https://github.com/VHDLivery"
    vcsUrl = "https://github.com/VHDLivery/gradle-vhdlArtifacts"
    plugins {
        create("VhdlArtifactsPlugin") {
            id = "${group}"
            displayName = "VHDL Artifacts Plugin"
            description = "Plugin to manage artifacts of VHDL source code for FPGA development"
            tags = listOf("hdl", "vhdl", "src", "source code", "artifacts")
            implementationClass = "${group}.VhdlArtifactsPlugin"
        }
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
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
    // To publish officially check https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html
}