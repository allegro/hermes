plugins {
    `java-library`
}

dependencies {
    api(group = "org.apache.commons", name = "commons-text", version = "1.12.0")
    api(libs.metrics.core)
    api(libs.micrometer.core)

    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
}
