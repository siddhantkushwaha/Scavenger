import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.5.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.5.10"
    kotlin("plugin.spring") version "1.5.10"
}

group = "com.siddhantkushwaha"
version = "1.0"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springframework.boot:spring-boot-starter-web-services")

    implementation("org.apache.lucene:lucene-core:7.5.0")
    implementation("org.apache.lucene:lucene-analyzers-common:7.5.0")
    implementation("org.apache.lucene:lucene-queryparser:7.5.0")
    implementation("org.apache.lucene:lucene-highlighter:7.5.0")

    implementation("com.google.code.gson:gson:2.8.2")
    implementation("org.apache.commons:commons-text:1.9")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
