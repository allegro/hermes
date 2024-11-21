plugins {
    `java-library`
    groovy
}

dependencies {
    api(project(":hermes-api"))
    api(project(":hermes-metrics"))
    api(project(":hermes-schema"))

    api(libs.curator.client) {
        exclude(module = "slf4j-log4j12")
        exclude(module = "log4j")
    }
    api(libs.curator.recipes) {
        exclude(module = "slf4j-log4j12")
        exclude(module = "log4j")
    }

    api(libs.jersey.client)
    implementation(libs.jersey.hk2)
    api(libs.jersey.media.json.jackson)
    api(libs.jersey.bean.validation)

    api(libs.json2avro.converter)

    api(group = "org.apache.commons", name = "commons-collections4", version = "4.4")
    implementation(group = "commons-codec", name = "commons-codec", version = "1.16.1")
    implementation(libs.guava)

    api(libs.jackson.databind)
    api(libs.avro)
    api(group = "com.jayway.jsonpath", name = "json-path", version = "2.9.0")

    implementation(libs.metrics.core)

    implementation(group = "com.google.code.findbugs", name = "annotations", version = "3.0.1")
    api(libs.micrometer.core)
    api(libs.micrometer.registry.prometheus)

    implementation(group = "org.slf4j", name = "log4j-over-slf4j", version = "2.0.13")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.4.14")
    api(libs.kafka.clients) {
        exclude(group = "net.sf.jopt-simple")
    }

    api(group = "jakarta.inject", name = "jakarta.inject-api", version = "2.0.1")

    testImplementation(project(":hermes-test-helper"))

    testImplementation(group = "jakarta.servlet", name = "jakarta.servlet-api", version = "6.0.0")

    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
    testImplementation(group = "org.awaitility", name = "awaitility-groovy", version = "4.2.1")
    testRuntimeOnly(libs.junit.vintage.engine)
}
