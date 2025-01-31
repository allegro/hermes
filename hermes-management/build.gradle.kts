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

    api(libs.javers.core)
    api(libs.spring.boot.starter.actuator)
    api(libs.spring.boot.starter.jersey)
    api(libs.spring.boot.starter.web)

    implementation(project(":hermes-schema"))

    implementation(libs.commons.codec)
    implementation(libs.commons.io)
    implementation(libs.commons.jxpath)
    implementation(libs.httpclient5)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jersey.mvc.freemarker)
    implementation(libs.jopt.simple)
    implementation(libs.json.schema.validator)
    implementation(libs.kafka.clients)
    implementation(libs.swagger.jersey2.jaxrs) {
        exclude(group = "javax.validation", module = "validation-api")
    }

    testImplementation(project(":hermes-test-helper"))

    testImplementation(libs.groovy.json)
    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
    testImplementation(libs.spock.spring)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.spock)
}

node {
    version = "20.4.0"
    distBaseUrl = "https://nodejs.org/dist"
    download = true
    workDir = layout.buildDirectory.dir("nodejs")
    npmWorkDir = layout.buildDirectory.dir("npm")
    nodeProjectDir = rootProject.layout.projectDirectory.dir("hermes-console")
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
