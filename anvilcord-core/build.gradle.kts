plugins {
    id("anvilcord.java-conventions")
    id("anvilcord.maven-publish-conventions")
}

dependencies {
    api("org.springframework.boot:spring-boot-autoconfigure")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

