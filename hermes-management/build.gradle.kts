import com.github.gradle.node.yarn.task.YarnTask

plugins {
    `java-library`
    application
    alias(libs.plugins.node.gradle)
}

application {
    mainClass = "pl.allegro.tech.hermes.management.HermesManagement"
}

dependencies {
    api(project(":hermes-api"))
    api(project(":hermes-common"))
    api(project(":hermes-tracker"))
    implementation(project(":hermes-schema"))

    api(libs.spring.boot.starter.actuator)
    api(libs.spring.boot.starter.jersey)
    api(libs.spring.boot.starter.web)

    implementation(group = "net.sf.jopt-simple", name = "jopt-simple", version = "5.0.4")
    implementation(libs.jersey.mvc.freemarker)

    implementation(group = "io.swagger", name = "swagger-jersey2-jaxrs", version = "1.6.14") {
        exclude(group = "javax.validation", module = "validation-api")
    }

    implementation(libs.kafka.clients)

    implementation(group = "commons-codec", name = "commons-codec", version = "1.16.1")
    implementation(group = "com.github.java-json-tools", name = "json-schema-validator", version = "2.2.14")

    implementation(group = "commons-jxpath", name = "commons-jxpath", version = "1.3")
    implementation(group = "org.apache.httpcomponents.client5", name = "httpclient5", version = "5.3.1")

    api(group = "org.javers", name = "javers-core", version = "7.4.2")

    implementation(libs.jackson.datatype.jsr310)
    implementation(group = "commons-io", name = "commons-io", version = "2.16.1")

    testImplementation(project(":hermes-test-helper"))
    testImplementation(libs.spring.boot.starter.test)

    testImplementation(libs.groovy.json)
    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
    testImplementation(libs.spock.spring)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.spock)
}

node {
    version = "20.4.0"
    distBaseUrl = "https://nodejs.org/dist"
    download = true
    workDir = file("${project.buildDir}/nodejs")
    npmWorkDir = file("${project.buildDir}/npm")
    nodeProjectDir = file("${project.rootDir}/hermes-console")
}

tasks.named("yarnSetup") {
    dependsOn("nodeSetup")
}

tasks.named("yarn") {
    dependsOn("npmSetup")
}

tasks.register<YarnTask>("buildHermesConsole") {
    dependsOn("yarn")

    val tasksThatDontRequireConsole = listOf(
        "integrationTest",
        "slowIntegrationTest",
        "check"
    )

    onlyIf {
        tasksThatDontRequireConsole.intersect(gradle.startParameter.taskNames.toSet()).isEmpty()
    }

    args = listOf("build-only")
}

tasks.register<Copy>("attachHermesConsole") {
    dependsOn("buildHermesConsole")

    from("../hermes-console/dist")
    val staticDirectory = "${sourceSets["main"].output.resourcesDir}/static"
    // remove previous static dir if exists and start with clear setup
    delete(staticDirectory)
    into(staticDirectory)
}

tasks.register("prepareIndexTemplate") {
    doLast {
        val indexPath = "${sourceSets["main"].output.resourcesDir}/static/index.html"
        ant.withGroovyBuilder {
            "move"("file" to indexPath, "tofile" to "$indexPath.ftl")
        }
    }
}

tasks.named("compileTestGroovy") {
    dependsOn("attachHermesConsole")
}

tasks.named("javadoc") {
    dependsOn("attachHermesConsole")
}

tasks.named("jar") {
    dependsOn("attachHermesConsole", "prepareIndexTemplate")
}

tasks.named("run") {
    dependsOn("attachHermesConsole", "prepareIndexTemplate")
}
