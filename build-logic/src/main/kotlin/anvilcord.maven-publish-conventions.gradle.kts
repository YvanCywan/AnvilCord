import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.plugins.signing.SigningExtension
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    `maven-publish`
    signing
}

group = "io.github.yvancywan"
version = rootProject.version

plugins.withId("java") {
    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    plugins.withId("org.springframework.boot") {
        tasks.named<Jar>("jar") {
            enabled = true
            archiveClassifier.set("")
        }

        tasks.withType<BootJar>().configureEach {
            archiveClassifier.set("boot")
        }
    }

    if (!plugins.hasPlugin("java-gradle-plugin")) {
        extensions.configure<PublishingExtension> {
            publications {
                register<MavenPublication>("mavenJava") {
                    from(components["java"])
                }
            }
        }
    }
}

extensions.configure<PublishingExtension> {
    repositories {
        mavenCentral {
            credentials(PasswordCredentials::class) {
                username = providers.gradleProperty("mavenCentralUsername")
                    .orElse(providers.environmentVariable("MAVEN_CENTRAL_USERNAME"))
                    .orNull
                password = providers.gradleProperty("mavenCentralPassword")
                    .orElse(providers.environmentVariable("MAVEN_CENTRAL_PASSWORD"))
                    .orNull
            }
        }

        publications.withType<MavenPublication>().configureEach {
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            pom {
                name.set(project.name)
                description.set(project.description.takeUnless { it.isNullOrBlank() } ?: rootProject.description)
                url.set("https://github.com/yvancywan/AnvilCord")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit/")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("yvancywan")
                        name.set("Yvan Cywan")
                        email.set("yvan@studio-hebi.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/yvancywan/AnvilCord.git")
                    developerConnection.set("scm:git:ssh://git@github.com/yvancywan/AnvilCord.git")
                    url.set("https://github.com/yvancywan/AnvilCord")
                }
            }
        }
    }

    extensions.configure<SigningExtension> {
        isRequired = gradle.startParameter.taskNames.any { taskName ->
            val task = taskName.substringAfterLast(':').lowercase()
            task.startsWith("publish") && !task.contains("mavenlocal")
        }

        val signingKey = providers.gradleProperty("signingInMemoryKey")
            .orElse(providers.environmentVariable("SIGNING_KEY"))
            .orNull
        val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword")
            .orElse(providers.environmentVariable("SIGNING_PASSWORD"))
            .orNull

        if (!signingKey.isNullOrBlank()) {
            useInMemoryPgpKeys(signingKey, signingPassword)
        }

        sign(extensions.getByType<PublishingExtension>().publications)
    }
}