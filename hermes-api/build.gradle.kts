plugins {
    groovy
    `java-library`
}

dependencies {
    api(libs.handy.uri.templates)
    api(libs.hibernate.validator)
    api(libs.jackson.datatype.jsr310)
    api(libs.jackson.jakarta.rs.json.provider)
    api(libs.jakarta.ws.rs.api)
    api(libs.jakarta.xml.bind.api)

    implementation(libs.guava)
    implementation(libs.jackson.annotations)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jaxb.core)
    implementation(libs.jaxb.impl)

    testImplementation(project(":hermes-test-helper"))
    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)

    testRuntimeOnly(libs.junit.vintage.engine)
}
