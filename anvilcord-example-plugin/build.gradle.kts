plugins {
    id("anvilcord.java-conventions")
}

description = "Sample AnvilCord runtime plugin used to verify ServiceLoader-based plugin discovery."

dependencies {
    compileOnly(project(":anvilcord-discord"))

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


