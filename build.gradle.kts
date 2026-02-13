plugins {
    id("java")
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.brightcast"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}


tasks.register<Exec>("buildAngular") {
    workingDir = file("${project.projectDir}/brightcast-web")
    commandLine(if (System.getProperty("os.name").lowercase().contains("win")) "ng.cmd" else "ng", "build")
}

tasks.register<Copy>("copyFrontend") {
    dependsOn("buildAngular")
    from("${project.projectDir}/brightcast-web/dist/brightcast-web/browser")
    into("${project.projectDir}/src/main/resources/static")
}

tasks.named("bootJar") {
    dependsOn("copyFrontend")
}