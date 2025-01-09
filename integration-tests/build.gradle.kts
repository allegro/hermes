import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    idea
}

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

    testImplementation(libs.awaitility)
    testImplementation(libs.jetty.reactive.httpclient)
    testImplementation(libs.okhttp)
    testImplementation(libs.reactive.streams)
    testImplementation(libs.spring.test)
    testImplementation(libs.spring.webflux)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.gcloud)

    testImplementation(libs.hornetq.jms.server) {
        exclude(module = "hornetq-native")
    }

    // Import allure-bom to ensure correct versions of all the dependencies are used
    testImplementation(platform(libs.allure.bom))
    // Add necessary Allure dependencies to dependencies section
    testImplementation(libs.allure.junit5)

    agent(libs.aspectj.weaver)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)

    testRuntimeOnly(libs.junit.jupiter.engine)
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
