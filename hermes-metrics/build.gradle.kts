plugins {
    `java-library`
}

val versions = rootProject.extra["versions"] as Map<*, *>

dependencies {
    api(group = "io.dropwizard.metrics", name = "metrics-core", version = versions["dropwizard_metrics"] as String)
    api(group = "org.apache.commons", name = "commons-text", version = "1.12.0")
    api(group = "io.micrometer", name = "micrometer-core", version = versions["micrometer_metrics"] as String)

    testImplementation(group = "org.spockframework", name = "spock-core", version = versions["spock"] as String)
    testImplementation(group = "org.spockframework", name = "spock-junit4", version = versions["spock"] as String)
}
