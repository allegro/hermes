plugins {
    groovy
    java
    `java-library`
}

val versions = rootProject.extra["versions"] as Map<*, *>

dependencies {
    implementation(group = "junit", name = "junit", version = "4.13.2")
    api(group = "org.wiremock", name = "wiremock-standalone", version = versions["wiremock"] as String)
    implementation (group = "org.awaitility", name = "awaitility", version = "4.2.1")
    api(group = "org.apache.avro", name = "avro", version = versions["avro"] as String)
    implementation(group = "tech.allegro.schema.json2avro", name = "converter", version = versions["json2avro"] as String)
    implementation (group = "org.junit.jupiter", name = "junit-jupiter-api", version = versions["junit_jupiter"] as String)

    testImplementation(project(":hermes-test-helper"))
    testImplementation(group = "org.spockframework", name = "spock-core", version = versions["spock"] as String)
    testImplementation(group = "org.spockframework", name = "spock-junit4", version = versions["spock"] as String)
    testImplementation(group = "org.apache.groovy", name = "groovy-json", version = versions["groovy"] as String)
    testImplementation(group = "org.glassfish.jersey.core", name = "jersey-client", version = versions["jersey"] as String)
    testImplementation(group = "org.glassfish.jersey.inject", name = "jersey-hk2", version = versions["jersey"] as String)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = versions["junit_jupiter"] as String)
    testImplementation(group = "org.springframework", name = "spring-test", version = versions["spring_web"] as String)
    testRuntimeOnly(group = "org.junit.vintage", name = "junit-vintage-engine", version = versions["junit_jupiter"] as String)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
