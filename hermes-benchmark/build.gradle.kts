plugins {
    id("me.champeau.jmh") version "0.7.2"
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
    iterations = intProperty("jmh.iterations", 4)
    timeOnIteration = stringProperty("jmh.timeOnIteration", "80s")
    fork = intProperty("jmh.fork", 1)
    warmupIterations = intProperty("jmh.warmupIterations", 4)
    warmup = stringProperty("jmh.timeOnWarmupIteration", "80s")
    jvmArgs = listProperty("jmh.jvmArgs", listOf("-Xmx1g", "-Xms1g", "-XX:+UseG1GC") + chronicleMapJvmArgs)
    failOnError = booleanProperty("jmh.failOnError", true)
    threads = intProperty("jmh.threads", 4)
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

fun stringProperty(property: String, defaultValue: String): String =
    (project.findProperty(property) as? String) ?: defaultValue

fun listProperty(property: String, defaultValue: List<String>): List<String> =
    (project.findProperty(property) as? String)?.split(' ') ?: defaultValue

fun intProperty(property: String, defaultValue: Int): Int =
    (project.findProperty(property) as? String)?.toIntOrNull() ?: defaultValue

fun booleanProperty(property: String, defaultValue: Boolean): Boolean =
    (project.findProperty(property) as? String)?.toBooleanStrictOrNull() ?: defaultValue
