plugins {
    id("org.springframework.boot")
    id("org.graalvm.buildtools.native")
}

description = "AnvilCord starter application"

dependencies {
    implementation(project(":anvilcord-core"))
    implementation(project(":anvilcord-discord"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

