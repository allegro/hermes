plugins {
    `java-library`
}

dependencies {
    api(libs.commons.text)
    api(libs.metrics.core)
    api(libs.micrometer.core)

    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
}
