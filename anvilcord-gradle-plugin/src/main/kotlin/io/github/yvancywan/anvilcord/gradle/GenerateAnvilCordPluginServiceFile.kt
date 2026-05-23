package io.github.yvancywan.anvilcord.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.DataInputStream
import java.io.File
import java.util.TreeSet

private const val TARGET_PLUGIN_CLASS_NAME = "io.github.yvancywan.anvilcord.core.plugin.AnvilCordPlugin"
private const val CLASS_FILE_MAGIC = -889275714
private const val ACC_PUBLIC = 0x0001
private const val ACC_INTERFACE = 0x0200
private const val ACC_ABSTRACT = 0x0400

private fun Int.hasFlag(flag: Int): Boolean = this and flag != 0

/**
 * Generates the Java ServiceLoader descriptor used by AnvilCord runtime plugin discovery.
 */
@CacheableTask
abstract class GenerateAnvilCordPluginServiceFile : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val classesDirs: ConfigurableFileCollection = project.objects.fileCollection()

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

        val classInfos = classDirectories
            .asSequence()
            .flatMap { classDirectory -> classFilesIn(classDirectory) }
            .mapNotNull { classFile -> readClassInfo(classFile) }
            .associateBy { it.name }

        return classInfos.values
            .asSequence()
            .filter { it.name != ANVILCORD_PLUGIN_CLASS_NAME }
            .filter { it.isPublicConcreteClass() }
            .filter { it.hasPublicNoArgConstructor() }
            .filter { it.implementsPlugin(classInfos) }
            .map { it.name }
            .toSet()
    }

    private fun classFilesIn(classDirectory: File): Sequence<File> = classDirectory
        .walkTopDown()
        .asSequence()
        .filter { it.isFile && it.extension == "class" }
        .filterNot { classFile -> shouldSkipClassFile(classDirectory, classFile) }

    private fun shouldSkipClassFile(classDirectory: File, classFile: File): Boolean {
        val relativePath = classFile.relativeTo(classDirectory).invariantSeparatorsPath
        return relativePath == "module-info.class" ||
                relativePath.endsWith("/package-info.class") ||
                relativePath.contains('$')
    }

    private fun readClassInfo(classFile: File): ClassInfo? = try {
        DataInputStream(classFile.inputStream().buffered()).use { input ->
            if (input.readInt() != CLASS_FILE_MAGIC) {
                return null
            }
            input.readUnsignedShort() // minor_version
            input.readUnsignedShort() // major_version
            val constantPool = readConstantPool(input)
            val accessFlags = input.readUnsignedShort()
            val thisClass = input.readUnsignedShort()
            val superClass = input.readUnsignedShort()
            val interfaceCount = input.readUnsignedShort()
            val interfaces = (0 until interfaceCount)
                .map { constantPool.className(input.readUnsignedShort()) }
                .map { it.toBinaryName() }
            skipMembers(input, constantPool) // fields
            val methods = readMethods(input, constantPool)

            ClassInfo(
                name = constantPool.className(thisClass).toBinaryName(),
                accessFlags = accessFlags,
                superName = superClass.takeUnless { it == 0 }?.let { constantPool.className(it).toBinaryName() },
                interfaces = interfaces,
                methods = methods
            )
        }
    } catch (ex: Exception) {
        logger.warn("Could not inspect class file '$classFile' while generating AnvilCord plugin ServiceLoader descriptor", ex)
        null
    }

    private fun readConstantPool(input: DataInputStream): ConstantPool {
        val entries = arrayOfNulls<Any>(input.readUnsignedShort())
        var index = 1
        while (index < entries.size) {
            when (input.readUnsignedByte()) {
                1 -> entries[index] = input.readUTF()
                3, 4 -> input.skipBytes(4)
                5, 6 -> {
                    input.skipBytes(8)
                    index++
                }
                7 -> entries[index] = ClassReference(input.readUnsignedShort())
                8, 16, 19, 20 -> input.skipBytes(2)
                9, 10, 11, 12, 17, 18 -> input.skipBytes(4)
                15 -> input.skipBytes(3)
                else -> throw IllegalArgumentException("Unsupported class-file constant pool entry")
            }
            index++
        }
        return ConstantPool(entries)
    }

    private fun skipMembers(input: DataInputStream, constantPool: ConstantPool) {
        repeat(input.readUnsignedShort()) {
            input.readUnsignedShort() // access_flags
            input.readUnsignedShort() // name_index
            input.readUnsignedShort() // descriptor_index
            skipAttributes(input)
        }
    }

    private fun readMethods(input: DataInputStream, constantPool: ConstantPool): List<MethodInfo> =
        (0 until input.readUnsignedShort()).map {
            val accessFlags = input.readUnsignedShort()
            val name = constantPool.utf8(input.readUnsignedShort())
            val descriptor = constantPool.utf8(input.readUnsignedShort())
            skipAttributes(input)
            MethodInfo(accessFlags, name, descriptor)
        }

    private fun skipAttributes(input: DataInputStream) {
        repeat(input.readUnsignedShort()) {
            input.readUnsignedShort() // attribute_name_index
            val length = input.readInt()
            input.skipFully(length)
        }
    }

    private fun DataInputStream.skipFully(length: Int) {
        var remaining = length
        while (remaining > 0) {
            val skipped = skipBytes(remaining)
            if (skipped <= 0) {
                throw IllegalArgumentException("Unexpected end of class-file attribute")
            }
            remaining -= skipped
        }
    }

    private fun String.toBinaryName(): String = replace('/', '.')

    private data class ClassReference(val nameIndex: Int)

    private class ConstantPool(private val entries: Array<Any?>) {
        fun utf8(index: Int): String = entries[index] as String

        fun className(index: Int): String = utf8((entries[index] as ClassReference).nameIndex)
    }

    private data class MethodInfo(
        val accessFlags: Int,
        val name: String,
        val descriptor: String
    ) {
        fun isPublicNoArgConstructor(): Boolean =
            name == "<init>" && descriptor == "()V" && accessFlags.hasFlag(ACC_PUBLIC)
    }

    private data class ClassInfo(
        val name: String,
        val accessFlags: Int,
        val superName: String?,
        val interfaces: List<String>,
        val methods: List<MethodInfo>
    ) {
        fun isPublicConcreteClass(): Boolean =
            accessFlags.hasFlag(ACC_PUBLIC) &&
                    !accessFlags.hasFlag(ACC_INTERFACE) &&
                    !accessFlags.hasFlag(ACC_ABSTRACT)

        fun hasPublicNoArgConstructor(): Boolean = methods.any { it.isPublicNoArgConstructor() }

        fun implementsPlugin(classInfos: Map<String, ClassInfo>, visited: Set<String> = emptySet()): Boolean {
            if (name in visited) {
                return false
            }
            if (TARGET_PLUGIN_CLASS_NAME in interfaces) {
                return true
            }
            val nextVisited = visited + name
            return interfaces.any { classInfos[it]?.implementsPlugin(classInfos, nextVisited) == true } ||
                    classInfos[superName]?.implementsPlugin(classInfos, nextVisited) == true
        }
    }

    companion object {
        const val ANVILCORD_PLUGIN_CLASS_NAME = TARGET_PLUGIN_CLASS_NAME
        const val SERVICE_RESOURCE_PATH = "META-INF/services/$ANVILCORD_PLUGIN_CLASS_NAME"
    }
}




