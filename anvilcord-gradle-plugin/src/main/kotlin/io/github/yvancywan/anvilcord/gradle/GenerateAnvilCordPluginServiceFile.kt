package io.github.yvancywan.anvilcord.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.util.TreeSet

/**
 * Generates the Java ServiceLoader descriptor used by AnvilCord runtime plugin discovery.
 */
@CacheableTask
abstract class GenerateAnvilCordPluginServiceFile : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val classesDirs: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Classpath
    val classpath: ConfigurableFileCollection = project.objects.fileCollection()

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val existingServiceFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val providers = TreeSet<String>()
        providers.addAll(readExistingProviders())
        providers.addAll(discoverProviders())

        val outputRoot = outputDirectory.get().asFile
        project.delete(outputRoot)
        if (providers.isEmpty()) {
            logger.info("No AnvilCordPlugin implementations found; no ServiceLoader descriptor generated")
            return
        }

        val serviceFile = outputRoot.resolve(SERVICE_RESOURCE_PATH)
        serviceFile.parentFile.mkdirs()
        serviceFile.writeText(providers.joinToString(separator = System.lineSeparator(), postfix = System.lineSeparator()))
        logger.lifecycle("Generated AnvilCord plugin ServiceLoader descriptor with ${providers.size} provider(s)")
    }

    private fun readExistingProviders(): Set<String> = existingServiceFiles.files
        .asSequence()
        .filter { it.isFile }
        .flatMap { serviceFile -> serviceFile.readLines().asSequence() }
        .map { line -> line.substringBefore('#').trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    private fun discoverProviders(): Set<String> {
        val classDirectories = classesDirs.files.filter { it.isDirectory }
        if (classDirectories.isEmpty()) {
            return emptySet()
        }

        val urls = (classDirectories + classpath.files)
            .filter { it.exists() }
            .distinctBy { it.canonicalFile }
            .map { it.toURI().toURL() }
            .toTypedArray()

        URLClassLoader(urls, null).use { loader ->
            val pluginType = try {
                loader.loadClass(ANVILCORD_PLUGIN_CLASS_NAME)
            } catch (_: ClassNotFoundException) {
                logger.info("AnvilCordPlugin is not on the compile classpath; no ServiceLoader descriptor generated")
                return emptySet()
            }

            return classDirectories
                .asSequence()
                .flatMap { classDirectory -> classNamesIn(classDirectory) }
                .filterNot { it == ANVILCORD_PLUGIN_CLASS_NAME }
                .mapNotNull { className -> loadProviderClassName(loader, pluginType, className) }
                .toSet()
        }
    }

    private fun classNamesIn(classDirectory: File): Sequence<String> = classDirectory
        .walkTopDown()
        .asSequence()
        .filter { it.isFile && it.extension == "class" }
        .mapNotNull { classFile -> className(classDirectory, classFile) }

    private fun className(classDirectory: File, classFile: File): String? {
        val relativePath = classFile.relativeTo(classDirectory).invariantSeparatorsPath
        if (relativePath == "module-info.class" || relativePath.endsWith("/package-info.class")) {
            return null
        }
        return relativePath
            .removeSuffix(".class")
            .replace('/', '.')
            .takeUnless { it.contains('$') }
    }

    private fun loadProviderClassName(loader: ClassLoader, pluginType: Class<*>, className: String): String? {
        val candidate = try {
            loader.loadClass(className)
        } catch (ex: ReflectiveOperationException) {
            logger.warn("Could not inspect class '$className' while generating AnvilCord plugin ServiceLoader descriptor", ex)
            return null
        } catch (ex: LinkageError) {
            logger.warn("Could not inspect class '$className' while generating AnvilCord plugin ServiceLoader descriptor", ex)
            return null
        }

        val modifiers = candidate.modifiers
        if (!pluginType.isAssignableFrom(candidate) || candidate.isInterface || Modifier.isAbstract(modifiers)) {
            return null
        }
        if (!Modifier.isPublic(modifiers)) {
            logger.warn("Skipping AnvilCord plugin provider '$className' because ServiceLoader providers must be public")
            return null
        }
        if (candidate.constructors.none { it.parameterCount == 0 && Modifier.isPublic(it.modifiers) }) {
            logger.warn("Skipping AnvilCord plugin provider '$className' because ServiceLoader providers need a public no-arg constructor")
            return null
        }
        return candidate.name
    }

    companion object {
        const val ANVILCORD_PLUGIN_CLASS_NAME = "io.github.yvancywan.anvilcord.core.plugin.AnvilCordPlugin"
        const val SERVICE_RESOURCE_PATH = "META-INF/services/$ANVILCORD_PLUGIN_CLASS_NAME"
    }
}
