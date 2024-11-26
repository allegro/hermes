import pl.allegro.tech.hermes.findBooleanProperty
import pl.allegro.tech.hermes.findIntProperty
import pl.allegro.tech.hermes.findListProperty
import pl.allegro.tech.hermes.findStringProperty

plugins {
    alias(libs.plugins.jmh)
}

configurations {
    jmh
}

val chronicleMapJvmArgs = listOf<String>()

jmh {
    includes = listOf("pl\\.allegro\\.tech\\.hermes\\.benchmark\\..*")
    humanOutputFile = null as File?
    jmhVersion = "1.36"
    zip64 = true
    verbosity = "NORMAL"
    iterations = project.findIntProperty("jmh.iterations", 4)
    timeOnIteration = project.findStringProperty("jmh.timeOnIteration", "80s")
    fork = project.findIntProperty("jmh.fork", 1)
    warmupIterations = project.findIntProperty("jmh.warmupIterations", 4)
    warmup = project.findStringProperty("jmh.timeOnWarmupIteration", "80s")
    jvmArgs = project.findListProperty("jmh.jvmArgs", listOf("-Xmx1g", "-Xms1g", "-XX:+UseG1GC") + chronicleMapJvmArgs)
    failOnError = project.findBooleanProperty("jmh.failOnError", true)
    threads = project.findIntProperty("jmh.threads", 4)
    synchronizeIterations = false
    forceGC = false
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
}

dependencies {
    jmh(group = "org.openjdk.jmh", name = "jmh-core", version = "1.37")
    jmh(group = "org.openjdk.jmh", name = "jmh-generator-annprocess", version = "1.37")
    jmh(group = "org.apache.httpcomponents", name = "httpasyncclient", version = "4.1.5")
    jmh(project(":hermes-frontend"))
    jmh(project(":hermes-test-helper"))
    jmh(project(":hermes-common"))
    jmh(project(":hermes-tracker"))
}

// Workaround for duplicated `BenchmarkList` and `CompilerHints` files from META-INF directory in jmh jar.
// Those duplications can prevent running benchmark tests.
// More info: https://github.com/melix/jmh-gradle-plugin/issues/6
tasks.named<Jar>("jmhJar") {
    doFirst {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks.named<Copy>("processJmhResources") {
    doFirst {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
