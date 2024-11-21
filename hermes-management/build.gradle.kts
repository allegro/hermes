import com.github.gradle.node.yarn.task.YarnTask

plugins {
    `java-library`
    application
    id("com.github.node-gradle.node") version "7.0.2"
}

val versions = rootProject.extra["versions"] as Map<*, *>

application {
    mainClass.set("pl.allegro.tech.hermes.management.HermesManagement")
}

dependencies {
    api(project(":hermes-api"))
    api(project(":hermes-common"))
    api(project(":hermes-tracker"))
    implementation(project(":hermes-schema"))

    api(group = "org.springframework.boot", name = "spring-boot-starter-web", version = versions["spring"] as String)
    api(group = "org.springframework.boot", name = "spring-boot-starter-actuator", version = versions["spring"] as String)
    api(group = "org.springframework.boot", name = "spring-boot-starter-jersey", version = versions["spring"] as String)
    implementation(group = "net.sf.jopt-simple", name = "jopt-simple", version = "5.0.4")
    implementation(group = "org.glassfish.jersey.ext", name = "jersey-mvc-freemarker", version = versions["jersey"] as String)

    implementation(group = "io.swagger", name = "swagger-jersey2-jaxrs", version = "1.6.14") {
        exclude(group = "javax.validation", module = "validation-api")
    }

    implementation(group = "org.apache.kafka", name = "kafka-clients", version = versions["kafka"] as String)

    implementation(group = "commons-codec", name = "commons-codec", version = "1.16.1")
    implementation(group = "com.github.java-json-tools", name = "json-schema-validator", version = "2.2.14")

    implementation(group = "commons-jxpath", name = "commons-jxpath", version = "1.3")
    implementation(group = "org.apache.httpcomponents.client5", name = "httpclient5", version = "5.3.1")

    api(group = "org.javers", name = "javers-core", version = "7.4.2")

    implementation(group = "com.fasterxml.jackson.datatype", name = "jackson-datatype-jsr310", version = versions["jackson"] as String)
    implementation(group = "commons-io", name = "commons-io", version = "2.16.1")

    testImplementation(project(":hermes-test-helper"))
    testImplementation(group = "org.springframework.boot", name = "spring-boot-starter-test", version = versions["spring"] as String)

    testImplementation(group = "org.spockframework", name = "spock-core", version = versions["spock"] as String)
    testImplementation(group = "org.spockframework", name = "spock-junit4", version = versions["spock"] as String)
    testImplementation(group = "org.spockframework", name = "spock-spring", version = versions["spock"] as String)
    testImplementation(group = "org.apache.groovy", name = "groovy-json", version = versions["groovy"] as String)

    testImplementation(group = "org.testcontainers", name = "spock", version = versions["testcontainers"] as String)
    testImplementation(group = "org.testcontainers", name = "kafka", version = versions["testcontainers"] as String)
}

node {
    version = "20.4.0"
    distBaseUrl = "https://nodejs.org/dist"
    download = true
    workDir.set(file("${project.buildDir}/nodejs"))
    npmWorkDir.set(file("${project.buildDir}/npm"))
    nodeProjectDir.set(file("${project.rootDir}/hermes-console"))
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

    args.set(listOf("build-only"))
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
