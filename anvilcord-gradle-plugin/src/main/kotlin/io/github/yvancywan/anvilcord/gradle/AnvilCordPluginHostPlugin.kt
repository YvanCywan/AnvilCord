package io.github.yvancywan.anvilcord.gradle

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources

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
        project.pluginManager.apply("application")

        project.extensions.configure(JavaApplication::class.java) {
            mainClass.convention(ANVILCORD_MAIN_CLASS)
        }

        val extension = project.extensions.create(
            "anvilCord",
            AnvilCordPluginHostExtension::class.java
        )
        extension.version.convention(
            project.providers.gradleProperty("anvilCordVersion")
                .orElse(project.providers.provider { defaultAnvilCordVersion(project) })
        )
        configurePluginServiceGeneration(project)

        project.afterEvaluate {
            val anvilCordVersion = extension.version.get()
            dependencies.add("implementation", anvilCordDependency(project, "anvilcord-core", anvilCordVersion))
            dependencies.add("runtimeOnly", anvilCordDependency(project, "anvilcord-starter", anvilCordVersion))
            dependencies.add("testImplementation", anvilCordDependency(project, "anvilcord-starter", anvilCordVersion))
        }
    }

    private fun configurePluginServiceGeneration(project: Project) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME)
        val generatedServices = project.tasks.register(
            "generateAnvilCordPluginServiceFile",
            GenerateAnvilCordPluginServiceFile::class.java,
            object : Action<GenerateAnvilCordPluginServiceFile> {
                override fun execute(task: GenerateAnvilCordPluginServiceFile) {
                    val main = mainSourceSet.get()
                    task.description = "Generates the AnvilCordPlugin ServiceLoader descriptor."
                    task.group = "build"
                    task.classesDirs.from(main.output.classesDirs)
                    task.existingServiceFiles.from(main.resources.sourceDirectories.asFileTree.matching {
                        include(GenerateAnvilCordPluginServiceFile.SERVICE_RESOURCE_PATH)
                    })
                    task.outputDirectory.convention(project.layout.buildDirectory.dir("generated/anvilcord/pluginServices"))
                    main.resources.exclude(GenerateAnvilCordPluginServiceFile.SERVICE_RESOURCE_PATH)
                }
            }
        )

        project.tasks.named(
            mainSourceSet.get().processResourcesTaskName,
            ProcessResources::class.java,
            object : Action<ProcessResources> {
                override fun execute(task: ProcessResources) {
                    task.from(generatedServices.flatMap { it.outputDirectory })
                    task.dependsOn(generatedServices)
                }
            }
        )
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
        const val ANVILCORD_GROUP = "io.github.yvancywan"
        const val ANVILCORD_MAIN_CLASS = "io.github.yvancywan.anvilcord.AnvilCordApplication"
        const val DEFAULT_ANVILCORD_VERSION = "0.0.1-SNAPSHOT"
    }
}


