plugins {
    application
    `java-library`
}

application {
    mainClass.set("pl.allegro.tech.hermes.consumers.HermesConsumers")
}

val versions = rootProject.extra["versions"] as Map<*, *>
val sbeClasspath: Configuration by configurations.creating

dependencies {
    implementation(project(":hermes-common"))
    api(project(":hermes-tracker"))
    implementation(project(":hermes-metrics"))
    implementation(project(":hermes-schema"))

    api(group = "org.springframework.boot", name = "spring-boot-starter", version = versions["spring"] as String)
    api(group = "org.eclipse.jetty", name = "jetty-alpn-java-client", version = versions["jetty"] as String)
    api(group = "org.eclipse.jetty.http2", name = "jetty-http2-client-transport", version = versions["jetty"] as String)
    implementation(group = "org.jctools", name = "jctools-core", version = "4.0.3")
    api(group = "jakarta.jms", name = "jakarta.jms-api", version = "3.1.0")
    implementation(group = "joda-time", name = "joda-time", version = "2.12.7")
    implementation(group = "com.github.rholder", name = "guava-retrying", version = "2.0.0") {
        exclude(module = "guava")
    }
    implementation(group = "org.agrona", name = "agrona", version = "1.21.1")
    // TODO: can we update it? Which version of server do our clients use?
    implementation(group = "org.hornetq", name = "hornetq-jms-client", version = "2.4.1.Final") {
        exclude(module = "hornetq-native")
    }
    api(group = "com.google.cloud", name = "google-cloud-pubsub", version = "1.128.1")
    api(group = "org.apache.httpcomponents.core5", name = "httpcore5", version = "5.2.4")

    testImplementation(project(":hermes-test-helper"))
    testImplementation(group = "org.apache.curator", name = "curator-test", version = versions["curator"] as String)
    testImplementation(group = "jakarta.servlet", name = "jakarta.servlet-api", version = "6.0.0")

    testImplementation(project(":hermes-common"))

    testImplementation(group = "org.awaitility", name = "awaitility-groovy", version = "4.2.1")
    testImplementation(group = "tech.allegro.schema.json2avro", name = "converter", version = versions["json2avro"] as String)

    testImplementation(group = "org.spockframework", name = "spock-core", version = versions["spock"] as String)
    testImplementation(group = "org.spockframework", name = "spock-junit4", version = versions["spock"] as String)
    testRuntimeOnly(group = "org.junit.vintage", name = "junit-vintage-engine", version = versions["junit_jupiter"] as String)

    sbeClasspath(group = "uk.co.real-logic", name = "sbe-all", version = "1.31.1")
}

val generatedPath = "$buildDir/generated/java/"

tasks.register<JavaExec>("generateMaxRateSbeStubs") {
    description = "Generate SBE stubs for max-rate"
    classpath = sbeClasspath
    mainClass.set("uk.co.real_logic.sbe.SbeTool")
    systemProperties["sbe.output.dir"] = generatedPath
    systemProperties["sbe.xinclude.aware"] = "true"
    args = listOf("src/main/resources/sbe/max-rate.xml")
}

tasks.register<JavaExec>("generateWorkloadSbeStubs") {
    description = "Generate SBE stubs for workload"
    classpath = sbeClasspath
    mainClass.set("uk.co.real_logic.sbe.SbeTool")
    systemProperties["sbe.output.dir"] = generatedPath
    systemProperties["sbe.xinclude.aware"] = "true"
    args = listOf("src/main/resources/sbe/workload.xml")
}

tasks.register("generateSbeStubs") {
    description = "Generate all SBE stubs from provided schemas"
    dependsOn("generateMaxRateSbeStubs", "generateWorkloadSbeStubs")
}

sourceSets {
    val main by getting {
        java.srcDir(generatedPath)
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("generateSbeStubs")
}
