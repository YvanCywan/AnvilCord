dependencies {
    api(project(":anvilcord-core"))
    api("com.discord4j:discord4j-core:3.2.6")

    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework:spring-context")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

