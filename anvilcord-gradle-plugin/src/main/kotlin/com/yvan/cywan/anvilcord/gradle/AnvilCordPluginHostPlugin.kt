package com.yvan.cywan.anvilcord.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for applications that host AnvilCord plugins and commands.
 *
 * <p>Local AnvilCord builds use project dependencies. Published consumers use
 * matching Maven coordinates, defaulting to the plugin version unless overridden
 * via {@code anvilCord.version} or the {@code anvilCordVersion} Gradle property.</p>
 */
@Suppress("unused")
class AnvilCordPluginHostPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("java-library")

        val extension = project.extensions.create(
            "anvilCord",
            AnvilCordPluginHostExtension::class.java
        )
        extension.version.convention(
            project.providers.gradleProperty("anvilCordVersion")
                .orElse(project.providers.provider { defaultAnvilCordVersion(project) })
        )

        project.afterEvaluate {
            val anvilCordVersion = extension.version.get()
            dependencies.add("implementation", anvilCordDependency(project, "anvilcord-core", anvilCordVersion))
            dependencies.add("runtimeOnly", anvilCordDependency(project, "anvilcord-starter", anvilCordVersion))
            dependencies.add("testImplementation", anvilCordDependency(project, "anvilcord-starter", anvilCordVersion))
        }
    }

    private fun anvilCordDependency(project: Project, artifactName: String, version: String): Any {
        val projectPath = ":$artifactName"
        return if (project.rootProject.findProject(projectPath) != null) {
            project.dependencies.project(mapOf("path" to projectPath))
        } else {
            "$ANVILCORD_GROUP:$artifactName:$version"
        }
    }

    private fun defaultAnvilCordVersion(project: Project): String {
        val pluginVersion = javaClass.`package`.implementationVersion
        if (!pluginVersion.isNullOrBlank()) {
            return pluginVersion
        }

        return project.rootProject.version.toString()
            .takeUnless { it.isBlank() || it == Project.DEFAULT_VERSION }
            ?: DEFAULT_ANVILCORD_VERSION
    }

    private companion object {
        const val ANVILCORD_GROUP = "com.yvan.cywan"
        const val DEFAULT_ANVILCORD_VERSION = "0.0.1-SNAPSHOT"
    }
}


