plugins {
    `java-library`
}

dependencies {
    api(libs.elasticsearch.transport)

    implementation(project(":hermes-common"))
    implementation(project(":hermes-tracker"))

    implementation(libs.slf4j.api)

    testImplementation(project(path = ":hermes-tracker", configuration = "testArtifacts"))
    testImplementation(project(path = ":hermes-test-helper"))

    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
    testImplementation(libs.testcontainers.elasticsearch)

    testRuntimeOnly(libs.junit.vintage.engine)
}
