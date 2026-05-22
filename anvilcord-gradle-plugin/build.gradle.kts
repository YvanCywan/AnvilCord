plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("anvilcord.maven-publish-conventions")
}

group = "io.github.yvancywan"
description = "Gradle plugin for AnvilCord plugin host projects"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("anvilCordPluginHost") {
            id = "io.github.yvancywan.anvilcord"
            implementationClass = "io.github.yvancywan.anvilcord.gradle.AnvilCordPluginHostPlugin"
            displayName = "AnvilCord Plugin Host"
            description = "Adds the AnvilCord core compile API and starter runtime needed by plugin host applications."
            tags = listOf("anvilcord", "discord", "spring-boot")
        }
    }
}

version = providers.gradleProperty("anvilCordVersion")
    .orElse(providers.environmentVariable("ANVILCORD_VERSION"))
    .orElse("0.0.1-SNAPSHOT")
    .get()

tasks.jar {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

