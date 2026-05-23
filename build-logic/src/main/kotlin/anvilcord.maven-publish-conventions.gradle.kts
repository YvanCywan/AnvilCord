import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.plugins.signing.SigningExtension
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("com.vanniktech.maven.publish")
}

group = "io.github.yvancywan"
version = rootProject.version

plugins.withId("java") {
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

    extensions.configure<MavenPublishBaseExtension> {
        coordinates(project.group.toString(), project.name, project.version.toString())
        publishToMavenCentral()
        signAllPublications()

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

    extensions.configure<SigningExtension> {
        isRequired = gradle.startParameter.taskNames.any { taskName ->
            val task = taskName.substringAfterLast(':').lowercase()
            task.startsWith("publish") && !task.contains("mavenlocal")
        }
    }
}

extensions.configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
        versionMapping {
            usage("java-api") {
                fromResolutionOf("runtimeClasspath")
            }
            usage("java-runtime") {
                fromResolutionResult()
            }
        }
    }
}

