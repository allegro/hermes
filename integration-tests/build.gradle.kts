import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    idea
}

val versions = rootProject.extra["versions"] as Map<*, *>

repositories {
    mavenCentral()
    maven {
        url = uri("https://repository.jboss.org/nexus/content/groups/public")
    }
}

val agent: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = true
}

dependencies {
    testImplementation(project(":hermes-client"))
    testImplementation(project(":hermes-common"))
    testImplementation(project(":hermes-consumers"))
    testImplementation(project(":hermes-frontend"))
    testImplementation(project(":hermes-management"))
    testImplementation(project(":hermes-test-helper"))

    testImplementation(group = "org.testcontainers", name = "testcontainers", version = versions["testcontainers"] as String)
    testImplementation(group = "org.testcontainers", name = "gcloud", version = versions["testcontainers"] as String)
    testImplementation(group = "com.squareup.okhttp3", name = "okhttp", version = versions["okhttp"] as String)
    testImplementation(group = "org.springframework", name = "spring-webflux", version = versions["spring_web"] as String)
    testImplementation(group = "org.springframework", name = "spring-test", version = versions["spring_web"] as String)
    testImplementation(group = "org.eclipse.jetty", name = "jetty-reactive-httpclient", version = "4.0.3")
    testImplementation(group = "org.awaitility", name = "awaitility", version = "4.2.0")
    testImplementation(group = "org.reactivestreams", name = "reactive-streams", version = "1.0.4")
    // TODO: can we update it? Which version of server do our clients use?
    testImplementation(group = "org.hornetq", name = "hornetq-jms-server", version = "2.4.1.Final") {
        exclude(module = "hornetq-native")
    }

    // Import allure-bom to ensure correct versions of all the dependencies are used
    testImplementation(platform("io.qameta.allure:allure-bom:${versions["allure"] as String}"))
    // Add necessary Allure dependencies to dependencies section
    testImplementation("io.qameta.allure:allure-junit5")

    agent("org.aspectj:aspectjweaver:1.9.21")

    testImplementation(group = "org.assertj", name = "assertj-core", version = versions["assertj"] as String)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = versions["junit_jupiter"] as String)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-params", version = versions["junit_jupiter"] as String)

    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = versions["junit_jupiter"] as String)
}

val chronicleMapJvmArgs = listOf<String>()

val common = sourceSets.create("common")
configurations[common.implementationConfigurationName].extendsFrom(configurations["testImplementation"])
configurations[common.runtimeOnlyConfigurationName].extendsFrom(configurations["testRuntimeOnly"])

fun registerIntegrationTestTask(name: String, common: SourceSet) {
    val integrationTest = sourceSets.create(name)

    integrationTest.compileClasspath += common.output
    integrationTest.runtimeClasspath += common.output

    configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations["testImplementation"])
    configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations["testRuntimeOnly"])

    tasks.register<Test>(name) {
        logging.captureStandardOutput(LogLevel.INFO)

        val args = mutableListOf<String>()
        if (project.hasProperty("tests.timeout.multiplier")) {
            args += "-Dtests.timeout.multiplier=${project.property("tests.timeout.multiplier")}"
        }

        if (project.hasProperty("confluentImagesTag")) {
            args += "-DconfluentImagesTag=${project.property("confluentImagesTag")}"
        }

        args += "-javaagent:${configurations["agent"].singleFile}"

        args += chronicleMapJvmArgs

        jvmArgs = args
        minHeapSize = "2000m"
        maxHeapSize = "3500m"

        group = "Verification"
        description = "Runs the integration tests."
        useJUnitPlatform()

        testClassesDirs = integrationTest.output.classesDirs
        classpath =
            configurations[integrationTest.runtimeClasspathConfigurationName] +
                    integrationTest.output +
                    common.output

        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events = setOf(
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.FAILED,
                TestLogEvent.STANDARD_ERROR,
                TestLogEvent.STANDARD_OUT
            )
        }
    }
}

registerIntegrationTestTask("integrationTest", common)
registerIntegrationTestTask("slowIntegrationTest", common)

idea {
    module {
        testSources.from(sourceSets["integrationTest"].allSource.srcDirs)
        testResources.from(sourceSets["integrationTest"].resources.srcDirs)

        testSources.from(sourceSets["slowIntegrationTest"].allSource.srcDirs)
        testResources.from(sourceSets["slowIntegrationTest"].resources.srcDirs)
    }
}
