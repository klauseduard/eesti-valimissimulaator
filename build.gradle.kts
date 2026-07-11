plugins {
    kotlin("jvm") version "1.8.0"
    id("org.openjfx.javafxplugin") version "0.0.13"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    // JavaFX 17 nõuab vähemalt Java 11
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}

javafx {
    version = "17.0.2"
    modules = listOf("javafx.controls", "javafx.graphics")
}
