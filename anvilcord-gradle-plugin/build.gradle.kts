plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("anvilcord.maven-publish-conventions") apply false
}

group = "io.github.yvancywan"
description = "Gradle plugin for AnvilCord plugin host projects"
version = gradle.startParameter.projectProperties["version"]
    ?: gradle.startParameter.projectProperties["verison"]
    ?: gradle.startParameter.projectProperties["anvilCordVersion"]
    ?: providers.environmentVariable("ANVILCORD_VERSION").orNull
    ?: providers.gradleProperty("version").orNull?.takeUnless { it.isBlank() || it == "0.0.0" }
    ?: providers.gradleProperty("verison").orNull?.takeUnless { it.isBlank() }
    ?: providers.gradleProperty("anvilCordVersion").orNull?.takeUnless { it.isBlank() }
    ?: "0.0.1-SNAPSHOT"

apply(plugin = "anvilcord.maven-publish-conventions")

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("anvilCordPluginHost") {
            id = "io.github.yvancywan.anvilcord"
            implementationClass = "io.github.yvancywan.anvilcord.gradle.AnvilCordPluginHostPlugin"
            displayName = "AnvilCord Plugin Host"
            description = "Adds the AnvilCord core compile API, starter runtime, and generated plugin ServiceLoader metadata."
            tags = listOf("anvilcord", "discord", "spring-boot")
        }
    }
}


tasks.jar {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

