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

    api(libs.reactor.core)

    testImplementation(libs.jakarta.servlet.api)
    testImplementation(libs.json.path)
    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
    testImplementation(libs.wiremock.standalone)

    testImplementation(libs.jersey.client)
    testImplementation(libs.jersey.hk2)
    testImplementation(libs.micrometer.core)
    testImplementation(libs.okhttp)
    testImplementation(libs.reactor.netty)
    testImplementation(libs.reactor.test)
    testImplementation(libs.spring.context)
    testImplementation(libs.spring.web)
    testImplementation(libs.spring.webflux)
}
