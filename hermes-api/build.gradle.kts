plugins {
    groovy
    `java-library`
}

dependencies {
    api(group = "com.damnhandy", name = "handy-uri-templates", version = "2.1.8")
    api(group = "jakarta.ws.rs", name = "jakarta.ws.rs-api", version = "3.1.0")
    api(group = "jakarta.xml.bind", name = "jakarta.xml.bind-api", version = "4.0.0")
    api(group = "org.hibernate.validator", name = "hibernate-validator", version = "8.0.1.Final")
    api(libs.jackson.datatype.jsr310)
    api(libs.jackson.jakarta.rs.json.provider)

    implementation(group = "com.sun.xml.bind", name = "jaxb-core", version = "4.0.5")
    implementation(group = "com.sun.xml.bind", name = "jaxb-impl", version = "4.0.5")
    implementation(group = "jakarta.annotation", name = "jakarta.annotation-api", version = "3.0.0")
    implementation(libs.guava)
    implementation(libs.jackson.annotations)

    testImplementation(project(":hermes-test-helper"))
    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)

    testRuntimeOnly(libs.junit.vintage.engine)
}
