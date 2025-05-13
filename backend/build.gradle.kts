plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.spring") version "2.0.0"
    kotlin("plugin.jpa") version "2.0.0"
}

group = "com.github.cosminchr"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

springBoot {
    mainClass.set("com.github.cosminchr.osint.OsintApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Docker Java Client
    implementation("com.github.docker-java:docker-java-core:3.3.3")
    implementation("com.github.docker-java:docker-java-api:3.3.3")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.3")

    // Add the Apache HttpClient 5 HTTP/2 dependency
    implementation("org.apache.httpcomponents.client5:httpclient5-fluent:5.2.1")
    implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.2.1")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")

    // Jakarta XML Bind
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.2")

    // WebSocket support
    implementation("org.springframework.boot:spring-boot-starter-websocket")


    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// Replace deprecated kotlinOptions with compilerOptions using the new syntax
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
