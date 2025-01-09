plugins {
    application
    `java-library`
}

application {
    mainClass = "pl.allegro.tech.hermes.frontend.HermesFrontend"
}

dependencies {
    api(project(":hermes-tracker"))

    api(libs.spring.boot.starter)
    api(libs.undertow.core)

    api(libs.chronicle.map) {
        exclude(group = "net.openhft", module = "chronicle-analytics")
    }

    implementation(project(":hermes-common"))
    implementation(project(":hermes-metrics"))
    implementation(project(":hermes-schema"))

    implementation(libs.commons.io)
    implementation(libs.failsafe)

    testImplementation(project(":hermes-test-helper"))

    testImplementation(libs.awaitility)
    testImplementation(libs.awaitility.groovy)
    testImplementation(libs.groovy.json)
    testImplementation(libs.spock.core)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.spock)

    testRuntimeOnly(libs.junit.vintage.engine)
}
