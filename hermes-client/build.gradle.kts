plugins {
    `java-library`
}

dependencies {
    compileOnly(libs.jersey.client)
    compileOnly(libs.jersey.hk2)
    compileOnly(libs.micrometer.core)
    compileOnly(libs.okhttp)
    compileOnly(libs.spring.web)
    compileOnly(libs.spring.webflux)

    implementation(libs.failsafe)

    api(group = "io.projectreactor", name = "reactor-core", version = "3.6.5")

    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
    testImplementation(libs.wiremock.standalone)
    testImplementation(group = "jakarta.servlet", name = "jakarta.servlet-api", version = "6.0.0")
    testImplementation(group = "com.jayway.jsonpath", name = "json-path", version = "2.9.0")

    testImplementation(libs.jersey.client)
    testImplementation(libs.jersey.hk2)
    testImplementation(libs.micrometer.core)
    testImplementation(libs.okhttp)
    testImplementation(libs.spring.context)
    testImplementation(libs.spring.web)
    testImplementation(libs.spring.webflux)
    testImplementation(group = "io.projectreactor.netty", name = "reactor-netty", version = "1.1.18")
    testImplementation(group = "io.projectreactor", name = "reactor-test", version = "3.6.5")
}
