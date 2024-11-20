plugins {
    groovy
    `java-library`
}

val versions = rootProject.extra["versions"] as Map<*, *>

dependencies {
    api(group = "org.hibernate.validator", name = "hibernate-validator", version = "8.0.1.Final")

    api(group = "jakarta.ws.rs", name = "jakarta.ws.rs-api", version = "3.1.0")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = versions["jackson"] as String)
    api(group = "com.fasterxml.jackson.jakarta.rs", name = "jackson-jakarta-rs-json-provider", version = versions["jackson"] as String)
    api(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jsr310", version = versions["jackson"] as String)
    implementation(group = "com.google.guava", name = "guava", version = versions["guava"] as String)
    api(group = "com.damnhandy", name = "handy-uri-templates", version = "2.1.8")
    api(group = "jakarta.xml.bind", name = "jakarta.xml.bind-api", version = "4.0.0")

    implementation(group = "com.sun.xml.bind", name = "jaxb-core", version = "4.0.5")
    implementation(group = "com.sun.xml.bind", name = "jaxb-impl", version = "4.0.5")
    implementation(group = "jakarta.annotation", name = "jakarta.annotation-api", version = "3.0.0")

    testImplementation(group = "org.spockframework", name = "spock-core", version = versions["spock"] as String)
    testImplementation(group = "org.spockframework", name = "spock-junit4", version = versions["spock"] as String)
    testImplementation(project(":hermes-test-helper"))
    testRuntimeOnly(group = "org.junit.vintage", name = "junit-vintage-engine", version = versions["junit_jupiter"] as String)
}
