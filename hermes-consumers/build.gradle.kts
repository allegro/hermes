plugins {
    application
    `java-library`
}

application {
    mainClass = "pl.allegro.tech.hermes.consumers.HermesConsumers"
}

val sbeClasspath: Configuration by configurations.creating

dependencies {
    api(project(":hermes-tracker"))

    api(libs.google.cloud.pubsub)
    api(libs.httpcore5)
    api(libs.jakarta.jms.api)
    api(libs.jetty.alpn.java.client)
    api(libs.jetty.http2.client.transport)
    api(libs.spring.boot.starter)

    implementation(project(":hermes-common"))
    implementation(project(":hermes-metrics"))
    implementation(project(":hermes-schema"))

    implementation(libs.agrona)
    implementation(libs.guava.retrying) {
        exclude(module = "guava")
    }
    implementation(libs.hornetq.jms.client) {
        exclude(module = "hornetq-native")
    }
    implementation(libs.jctools.core)
    implementation(libs.joda.time)

    testImplementation(project(":hermes-common"))
    testImplementation(project(":hermes-test-helper"))

    testImplementation(libs.awaitility.groovy)
    testImplementation(libs.curator.test)
    testImplementation(libs.jakarta.servlet.api)
    testImplementation(libs.json2avro.converter)
    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)

    testRuntimeOnly(libs.junit.vintage.engine)

    sbeClasspath(libs.sbe.all)
}

val generatedPath = layout.buildDirectory.dir("generated/java").get()

tasks.register<JavaExec>("generateMaxRateSbeStubs") {
    description = "Generate SBE stubs for max-rate"
    classpath = sbeClasspath
    mainClass = "uk.co.real_logic.sbe.SbeTool"
    systemProperties["sbe.output.dir"] = generatedPath
    systemProperties["sbe.xinclude.aware"] = "true"
    args = listOf("src/main/resources/sbe/max-rate.xml")
}

tasks.register<JavaExec>("generateWorkloadSbeStubs") {
    description = "Generate SBE stubs for workload"
    classpath = sbeClasspath
    mainClass = "uk.co.real_logic.sbe.SbeTool"
    systemProperties["sbe.output.dir"] = generatedPath
    systemProperties["sbe.xinclude.aware"] = "true"
    args = listOf("src/main/resources/sbe/workload.xml")
}

tasks.register("generateSbeStubs") {
    description = "Generate all SBE stubs from provided schemas"
    dependsOn("generateMaxRateSbeStubs", "generateWorkloadSbeStubs")
}

sourceSets {
    val main by getting {
        java.srcDir(generatedPath)
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("generateSbeStubs")
}
