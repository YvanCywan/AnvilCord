plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("anvilcord.maven-publish-conventions") apply false
}

group = "io.github.yvancywan"
description = "Gradle plugin for AnvilCord plugin host projects"
version = providers.gradleProperty("verison")
    .orElse(providers.gradleProperty("version"))
    .orElse(providers.gradleProperty("anvilCordVersion"))
    .orElse(providers.environmentVariable("ANVILCORD_VERSION"))
    .orElse("0.0.1-SNAPSHOT")
    .get()

apply(plugin = "anvilcord.maven-publish-conventions")

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


tasks.jar {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

