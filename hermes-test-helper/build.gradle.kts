plugins {
    `java-library`
}

val versions = rootProject.extra["versions"] as Map<*, *>

dependencies {
    implementation(project(":hermes-api"))
    implementation(project(":hermes-common"))
    implementation(project(":hermes-consumers"))

    implementation(group = "org.glassfish.jersey.core", name = "jersey-client", version = versions["jersey"] as String)
    implementation(group = "org.glassfish.jersey.inject", name = "jersey-hk2", version = versions["jersey"] as String)
    implementation(group = "org.glassfish.jersey.ext", name = "jersey-proxy-client", version = versions["jersey"] as String)
    api(group = "commons-io", name = "commons-io", version = "2.16.1")
    api(group = "org.wiremock", name = "wiremock-standalone", version = versions["wiremock"] as String)
    api(group = "org.apache.curator", name = "curator-test", version = versions["curator"] as String) {
        exclude(module = "slf4j-log4j12")
        exclude(module = "log4j")
    }
    implementation(group = "org.apache.curator", name = "curator-client", version = versions["curator"] as String) {
        exclude(module = "slf4j-log4j12")
        exclude(module = "log4j")
    }
    implementation(group = "org.apache.curator", name = "curator-recipes", version = versions["curator"] as String) {
        exclude(module = "slf4j-log4j12")
        exclude(module = "log4j")
    }
    implementation(group = "com.github.spotbugs", name = "spotbugs-annotations", version = "4.8.4")
    implementation(group = "org.awaitility", name = "awaitility-groovy", version = "4.2.1")
    implementation(group = "org.assertj", name = "assertj-core", version = versions["assertj"] as String)
    api(group = "net.javacrumbs.json-unit", name = "json-unit-fluent", version = "3.2.7")
    implementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.10.2")
    implementation(group = "org.apache.httpcomponents.client5", name = "httpclient5", version = "5.3.1")
    implementation(group = "com.jayway.jsonpath", name = "json-path", version = "2.9.0")
    implementation(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jsr310", version = versions["jackson"] as String)
    implementation(group = "org.springframework", name = "spring-test", version = versions["spring_web"] as String)
    implementation(group = "org.springframework", name = "spring-webflux", version = versions["spring_web"] as String)
    implementation(group = "org.awaitility", name = "awaitility", version = "4.2.0")
    testImplementation(group = "org.spockframework", name = "spock-core", version = versions["spock"] as String)
    testImplementation(group = "org.spockframework", name = "spock-junit4", version = versions["spock"] as String)

    implementation(group = "org.testcontainers", name = "testcontainers", version = versions["testcontainers"] as String)
    implementation(group = "org.testcontainers", name = "toxiproxy", version = versions["testcontainers"] as String)
    implementation(group = "org.testcontainers", name = "gcloud", version = versions["testcontainers"] as String)
}
