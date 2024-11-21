val versions = rootProject.extra["versions"] as Map<*, *>

dependencies {
    implementation(project(":hermes-api"))

    implementation(group = "org.apache.avro", name = "avro", version = versions["avro"] as String)
    implementation(group = "com.google.guava", name = "guava", version = versions["guava"] as String)

    testImplementation(project(path = ":hermes-test-helper"))

    testImplementation(group = "org.spockframework", name = "spock-core", version = versions["spock"] as String)
    testImplementation(group = "org.spockframework", name = "spock-junit4", version = versions["spock"] as String)

}
