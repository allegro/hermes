plugins {
    groovy
    java
    `java-library`
}

dependencies {
    api(libs.avro)
    api(libs.wiremock.standalone)

    implementation (group = "org.awaitility", name = "awaitility", version = "4.2.1")
    implementation(group = "junit", name = "junit", version = "4.13.2")
    implementation(libs.json2avro.converter)
    implementation(libs.junit.jupiter.api)

    testImplementation(project(":hermes-test-helper"))
    testImplementation(libs.groovy.json)
    testImplementation(libs.jersey.client)
    testImplementation(libs.jersey.hk2)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
    testImplementation(libs.spring.test)

    testRuntimeOnly(libs.junit.vintage.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
