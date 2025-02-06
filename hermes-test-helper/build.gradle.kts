plugins {
    `java-library`
}

dependencies {
    api(libs.commons.io)
    api(libs.json.unit.fluent)
    api(libs.wiremock.standalone)

    api(libs.curator.test) {
        exclude(module = "slf4j-log4j12")
        exclude(module = "log4j")
    }

    implementation(project(":hermes-api"))
    implementation(project(":hermes-common"))
    implementation(project(":hermes-consumers"))

    implementation(libs.assertj.core)
    implementation(libs.awaitility)
    implementation(libs.awaitility.groovy)
    implementation(libs.httpclient5)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jersey.client)
    implementation(libs.jersey.hk2)
    implementation(libs.jersey.proxy.client)
    implementation(libs.json.path)
    implementation(libs.junit.jupiter.api)
    implementation(libs.spotbugs.annotations)
    implementation(libs.spring.test)
    implementation(libs.spring.webflux)
    implementation(libs.testcontainers)
    implementation(libs.testcontainers.gcloud)
    implementation(libs.testcontainers.toxiproxy)

    implementation(libs.curator.client) {
        exclude(module = "slf4j-log4j12")
        exclude(module = "log4j")
    }
    implementation(libs.curator.recipes) {
        exclude(module = "slf4j-log4j12")
        exclude(module = "log4j")
    }

    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
}
