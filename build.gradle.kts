plugins {
    id("org.springframework.boot") version "4.0.6" apply false
    id("org.graalvm.buildtools.native") version "0.11.5" apply false
}

group = "io.github.yvancywan"
version = gradle.startParameter.projectProperties["version"]
    ?: gradle.startParameter.projectProperties["verison"]
    ?: gradle.startParameter.projectProperties["anvilCordVersion"]
    ?: providers.environmentVariable("ANVILCORD_VERSION").orNull
    ?: providers.gradleProperty("version").orNull?.takeUnless { it.isBlank() || it == "0.0.0" }
    ?: providers.gradleProperty("verison").orNull?.takeUnless { it.isBlank() }
    ?: providers.gradleProperty("anvilCordVersion").orNull?.takeUnless { it.isBlank() }
    ?: "0.0.1-SNAPSHOT"
description = "AnvilCord"

