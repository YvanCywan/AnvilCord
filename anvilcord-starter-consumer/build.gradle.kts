import org.gradle.api.tasks.bundling.Jar

plugins {
    id("anvilcord.java-conventions")
    id("com.yvan.cywan.anvilcord")
}

description = "Non-publishable consumer used to verify AnvilCord starter integration."

dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Jar>().configureEach {
    enabled = false
}

tasks.matching { it.name == "publish" || it.name.startsWith("publish") }.configureEach {
    enabled = false
}

