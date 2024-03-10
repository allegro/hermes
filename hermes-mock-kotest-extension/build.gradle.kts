plugins {
    kotlin("jvm") version "1.9.22"
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":hermes-mock"))
    implementation(libs.kotest.api)
    implementation(libs.kotlin.logging)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}
