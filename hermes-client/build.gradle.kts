plugins {
    `java-library`
}

val versions = rootProject.extra["versions"] as Map<*, *>

dependencies {
    compileOnly(group = "io.micrometer", name = "micrometer-core", version = versions["micrometer_metrics"] as String)
    compileOnly(group = "org.glassfish.jersey.core", name = "jersey-client", version = versions["jersey"] as String)
    compileOnly(group = "org.glassfish.jersey.inject", name = "jersey-hk2", version = versions["jersey"] as String)
    compileOnly(group = "org.springframework", name = "spring-web", version = versions["spring_web"] as String)
    compileOnly(group = "org.springframework", name = "spring-webflux", version = versions["spring_web"] as String)
    compileOnly(group = "com.squareup.okhttp3", name = "okhttp", version = versions["okhttp"] as String)

    implementation(group = "net.jodah", name = "failsafe", version = versions["failsafe"] as String)
    api(group = "io.projectreactor", name = "reactor-core", version = "3.6.5")

    testImplementation(group = "org.spockframework", name = "spock-core", version = versions["spock"] as String)
    testImplementation(group = "org.spockframework", name = "spock-junit4", version = versions["spock"] as String)
    testImplementation(group = "org.wiremock", name = "wiremock-standalone", version = versions["wiremock"] as String)
    testImplementation(group = "jakarta.servlet", name = "jakarta.servlet-api", version = "6.0.0")
    testImplementation(group = "com.jayway.jsonpath", name = "json-path", version = "2.9.0")

    testImplementation(group = "io.micrometer", name = "micrometer-core", version = versions["micrometer_metrics"] as String)
    testImplementation(group = "org.glassfish.jersey.core", name = "jersey-client", version = versions["jersey"] as String)
    testImplementation(group = "org.glassfish.jersey.inject", name = "jersey-hk2", version = versions["jersey"] as String)
    testImplementation(group = "org.springframework", name = "spring-web", version = versions["spring_web"] as String)
    testImplementation(group = "org.springframework", name = "spring-context", version = versions["spring_web"] as String)
    testImplementation(group = "org.springframework", name = "spring-webflux", version = versions["spring_web"] as String)
    testImplementation(group = "com.squareup.okhttp3", name = "okhttp", version = versions["okhttp"] as String)
    testImplementation(group = "io.projectreactor.netty", name = "reactor-netty", version = "1.1.18")
    testImplementation(group = "io.projectreactor", name = "reactor-test", version = "3.6.5")
}
