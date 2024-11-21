plugins {
    application
    `java-library`
}

val versions = rootProject.extra["versions"] as Map<*, *>

application {
    mainClass.set("pl.allegro.tech.hermes.frontend.HermesFrontend")
}

dependencies {
    implementation(project(":hermes-common"))
    api(project(":hermes-tracker"))
    implementation(project(":hermes-metrics"))
    implementation(project(":hermes-schema"))

    api(group = "org.springframework.boot", name = "spring-boot-starter", version = versions["spring"] as String)
    api(group = "io.undertow", name = "undertow-core", version = versions["undertow"] as String)
    // Did not update that as we're trying to abandon buffers
    api(group = "net.openhft", name = "chronicle-map", version = "3.25ea6") {
        exclude(group = "net.openhft", module = "chronicle-analytics")
    }
    implementation(group = "commons-io", name = "commons-io", version = "2.16.1")
    implementation(group = "net.jodah", name = "failsafe", version = versions["failsafe"] as String)

    testImplementation(project(":hermes-test-helper"))

    testImplementation(group = "org.spockframework", name = "spock-core", version = versions["spock"] as String)
    testImplementation(group = "org.apache.groovy", name = "groovy-json", version = versions["groovy"] as String)
    testImplementation(group = "org.awaitility", name = "awaitility-groovy", version = "4.2.1")
    testImplementation(group = "org.awaitility", name = "awaitility", version = "4.2.1")
    testImplementation(group = "org.testcontainers", name = "spock", version = versions["testcontainers"] as String)
    testImplementation(group = "org.testcontainers", name = "kafka", version = versions["testcontainers"] as String)
    testRuntimeOnly(group = "org.junit.vintage", name = "junit-vintage-engine", version = versions["junit_jupiter"] as String)
}
