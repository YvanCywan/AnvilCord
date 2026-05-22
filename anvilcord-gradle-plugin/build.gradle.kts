plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.yvan.cywan"
version = "0.0.1-SNAPSHOT"
description = "Gradle plugin for AnvilCord plugin host projects"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("anvilCordPluginHost") {
            id = "com.yvan.cywan.anvilcord"
            implementationClass = "com.yvan.cywan.anvilcord.gradle.AnvilCordPluginHostPlugin"
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

