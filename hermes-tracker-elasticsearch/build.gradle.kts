plugins {
    `java-library`
}

val versions = rootProject.extra["versions"] as Map<*, *>

dependencies {
    implementation(project(":hermes-common"))
    implementation(project(":hermes-tracker"))
    implementation("org.slf4j:slf4j-api:2.0.13")
    api(group = "org.elasticsearch.client", name = "transport", version = "7.10.2")

    testImplementation(project(path = ":hermes-tracker", configuration = "testArtifacts"))
    testImplementation(project(path = ":hermes-test-helper"))
    testImplementation(group = "org.spockframework", name = "spock-core", version = versions["spock"] as String)
    testImplementation(group = "org.spockframework", name = "spock-junit4", version = versions["spock"] as String)
    testImplementation("org.testcontainers:elasticsearch:1.20.3")
    testRuntimeOnly(group = "org.junit.vintage", name = "junit-vintage-engine", version = versions["junit_jupiter"] as String)
}
