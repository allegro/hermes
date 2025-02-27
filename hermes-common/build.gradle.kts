plugins {
    `java-library`
    groovy
}

dependencies {
    api(project(":hermes-api"))
    api(project(":hermes-metrics"))
    api(project(":hermes-schema"))

    api(libs.curator.client) {
        exclude(module = "log4j")
        exclude(module = "slf4j-log4j12")
    }

    api(libs.curator.recipes) {
        exclude(module = "log4j")
        exclude(module = "slf4j-log4j12")
    }

    api(libs.kafka.clients) {
        exclude(group = "net.sf.jopt-simple")
    }

    api(libs.avro)
    api(libs.commons.collections4)
    api(libs.jackson.databind)
    api(libs.jakarta.inject)
    api(libs.jersey.bean.validation)
    api(libs.jersey.client)
    api(libs.jersey.media.json.jackson)
    api(libs.json.path)
    api(libs.json2avro.converter)
    api(libs.micrometer.core)
    api(libs.micrometer.registry.prometheus)

    implementation(libs.annotations)
    implementation(libs.commons.codec)
    implementation(libs.guava)
    implementation(libs.jersey.hk2)
    implementation(libs.metrics.core)

    implementation(libs.log4j.over.slf4j)
    implementation(libs.logback.classic)

    testImplementation(project(":hermes-test-helper"))

    testImplementation(libs.awaitility.groovy)
    testImplementation(libs.jakarta.servlet.api)
    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)

    testRuntimeOnly(libs.junit.vintage.engine)
}
