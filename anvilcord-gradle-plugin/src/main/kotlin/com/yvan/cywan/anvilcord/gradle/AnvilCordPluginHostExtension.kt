package com.yvan.cywan.anvilcord.gradle

import org.gradle.api.provider.Property

/**
 * Configuration for the AnvilCord plugin host Gradle plugin.
 */
abstract class AnvilCordPluginHostExtension {

    /**
     * AnvilCord artifact version used when the target build does not contain
     * local ':anvilcord-core' and ':anvilcord-starter' projects.
     */
    abstract val version: Property<String>
}

