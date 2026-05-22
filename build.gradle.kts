plugins {
    id("org.springframework.boot") version "4.0.6" apply false
    id("org.graalvm.buildtools.native") version "0.11.5" apply false
}

group = "io.github.yvancywan"
version = providers.gradleProperty("verison")
    .orElse("undefined")
description = "AnvilCord"

