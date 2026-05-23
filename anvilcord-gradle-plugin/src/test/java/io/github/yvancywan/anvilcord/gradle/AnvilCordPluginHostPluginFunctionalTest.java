package io.github.yvancywan.anvilcord.gradle;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilCordPluginHostPluginFunctionalTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void generatesAnvilCordPluginServiceDescriptorInJar() throws IOException {
        Path repository = temporaryDirectory.resolve("repo");
        createAnvilCordStubRepository(repository);

        Path project = temporaryDirectory.resolve("consumer");
        Files.createDirectories(project.resolve("src/main/java/com/example"));
        Files.writeString(project.resolve("settings.gradle.kts"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        mavenCentral()
                    }
                }
                dependencyResolutionManagement {
                    repositories {
                        maven { url = uri(%s) }
                        mavenCentral()
                    }
                }
                rootProject.name = "consumer"
                """.formatted(quoted(repository)));
        Files.writeString(project.resolve("build.gradle.kts"), """
                plugins {
                    id("io.github.yvancywan.anvilcord")
                }

                anvilCord {
                    version.set("0.0.1-SNAPSHOT")
                }
                """);
        Files.writeString(project.resolve("src/main/java/com/example/GeneratedPlugin.java"), """
                package com.example;

                import io.github.yvancywan.anvilcord.core.plugin.AnvilCordPlugin;
                import io.github.yvancywan.anvilcord.core.plugin.AnvilCordPluginContext;

                public final class GeneratedPlugin implements AnvilCordPlugin {
                    @Override
                    public String id() {
                        return "generated";
                    }

                    @Override
                    public void initialize(AnvilCordPluginContext context) {
                    }
                }
                """);

        GradleRunner.create()
                .withProjectDir(project.toFile())
                .withPluginClasspath()
                .withArguments("jar", "--stacktrace")
                .build();

        Path jar = findJar(project.resolve("build/libs"));
        assertTrue(Files.isRegularFile(jar));
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry serviceEntry = zip.getEntry(GenerateAnvilCordPluginServiceFile.SERVICE_RESOURCE_PATH);
            assertTrue(serviceEntry != null, "generated ServiceLoader descriptor should be packaged");
            String descriptor = new String(zip.getInputStream(serviceEntry).readAllBytes()).trim();
            assertEquals("com.example.GeneratedPlugin", descriptor);
        }
    }

    private static void createAnvilCordStubRepository(Path repository) throws IOException {
        Path sourceRoot = repository.resolve("stub-sources");
        Path classesRoot = repository.resolve("stub-classes");
        Path packageRoot = sourceRoot.resolve("io/github/yvancywan/anvilcord/core/plugin");
        Files.createDirectories(packageRoot);
        Files.createDirectories(classesRoot);
        Path pluginSource = packageRoot.resolve("AnvilCordPlugin.java");
        Path contextSource = packageRoot.resolve("AnvilCordPluginContext.java");
        Files.writeString(pluginSource, """
                package io.github.yvancywan.anvilcord.core.plugin;

                public interface AnvilCordPlugin {
                    String id();
                    void initialize(AnvilCordPluginContext context);
                }
                """);
        Files.writeString(contextSource, """
                package io.github.yvancywan.anvilcord.core.plugin;

                public interface AnvilCordPluginContext {
                }
                """);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertTrue(compiler != null, "functional test requires a JDK");
        int exitCode = compiler.run(null, null, null,
                "-d", classesRoot.toString(),
                pluginSource.toString(),
                contextSource.toString());
        assertEquals(0, exitCode);

        createStubModule(repository, classesRoot, "anvilcord-core");
        createStubModule(repository, classesRoot, "anvilcord-starter");
    }

    private static Path findJar(Path libsDirectory) throws IOException {
        try (var jars = Files.list(libsDirectory)) {
            return jars
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("expected fixture jar in " + libsDirectory));
        }
    }

    private static void createStubModule(Path repository, Path classesRoot, String artifactId) throws IOException {
        Path moduleDirectory = repository.resolve("io/github/yvancywan/%s/0.0.1-SNAPSHOT".formatted(artifactId));
        Files.createDirectories(moduleDirectory);
        Path jar = moduleDirectory.resolve("%s-0.0.1-SNAPSHOT.jar".formatted(artifactId));
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            try (var classFiles = Files.walk(classesRoot)) {
                classFiles
                        .filter(Files::isRegularFile)
                        .forEach(path -> addJarEntry(output, classesRoot, path));
            }
        }
        Files.writeString(moduleDirectory.resolve("%s-0.0.1-SNAPSHOT.pom".formatted(artifactId)), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>io.github.yvancywan</groupId>
                  <artifactId>%s</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                </project>
                """.formatted(artifactId));
    }

    private static void addJarEntry(JarOutputStream output, Path classesRoot, Path classFile) {
        try {
            JarEntry entry = new JarEntry(classesRoot.relativize(classFile).toString().replace('\\', '/'));
            output.putNextEntry(entry);
            Files.copy(classFile, output);
            output.closeEntry();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String quoted(Path path) {
        return "\"" + path.toUri() + "\"";
    }
}


