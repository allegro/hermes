plugins {
    application
    `java-library`
}

application {
    mainClass = "pl.allegro.tech.hermes.frontend.HermesFrontend"
}

dependencies {
    implementation(project(":hermes-common"))
    api(project(":hermes-tracker"))
    implementation(project(":hermes-metrics"))
    implementation(project(":hermes-schema"))

    api(libs.spring.boot.starter)
    api(libs.undertow.core)
    // Did not update that as we're trying to abandon buffers
    api(group = "net.openhft", name = "chronicle-map", version = "3.25ea6") {
        exclude(group = "net.openhft", module = "chronicle-analytics")
    }
    implementation(group = "commons-io", name = "commons-io", version = "2.16.1")
    implementation(libs.failsafe)

    testImplementation(project(":hermes-test-helper"))

    testImplementation(group = "org.awaitility", name = "awaitility", version = "4.2.1")
    testImplementation(group = "org.awaitility", name = "awaitility-groovy", version = "4.2.1")
    testImplementation(libs.groovy.json)
    testImplementation(libs.spock.core)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.spock)

    testRuntimeOnly(libs.junit.vintage.engine)
}
