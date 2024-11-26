import org.gradle.api.tasks.testing.logging.TestLogEvent
import pl.allegro.tech.build.axion.release.domain.PredefinedVersionCreator
import pl.allegro.tech.hermes.findIntProperty
import pl.allegro.tech.hermes.findLongProperty
import java.time.Duration

plugins {
    java
    signing
    `maven-publish`
    alias(libs.plugins.axion.release)
    alias(libs.plugins.publish.plugin)
}

scmVersion {
    tag {
        prefix = project.rootProject.name
        versionSeparator = "-"
    }
    versionCreator = PredefinedVersionCreator.VERSION_WITH_BRANCH.versionCreator
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

nexusPublishing {
    connectTimeout = Duration.ofMinutes(project.findLongProperty("publishingTimeoutInMin", 10))
    clientTimeout = Duration.ofMinutes(project.findLongProperty("publishingTimeoutInMin", 10))

    repositories {
        sonatype {
            stagingProfileId = "19d6feb4b1fb3" // id for group 'pl.allegro.tech.hermes'
            username = System.getenv("SONATYPE_USERNAME")
            password = System.getenv("SONATYPE_PASSWORD")
        }
    }
    transitionCheckOptions {
        maxRetries = project.findIntProperty("attemptsToCloseStagingRepository", 30)
        delayBetween = Duration.ofSeconds(project.findLongProperty("delayInSecBetweenCloseStagingRepositoryAttempts", 45))
    }
}

val versionValue: String = scmVersion.version

allprojects {
    apply(plugin = "java")
    apply(plugin = "groovy")

    group = "pl.allegro.tech.hermes"
    version = versionValue

    extra["versions"] = mapOf(
        "kafka" to "2.8.2",
        "guava" to "33.1.0-jre",
        "jackson" to "2.17.0",
        "jersey" to "3.1.6",
        "jetty" to "12.0.8",
        "curator" to "5.4.0",
        "dropwizard_metrics" to "4.2.25",
        "micrometer_metrics" to "1.12.5",
        "wiremock" to "3.9.0",
        "spock" to "2.4-M4-groovy-4.0",
        "groovy" to "4.0.21",
        "avro" to "1.11.3",
        "json2avro" to "0.2.14",
        // TODO: newest version requires subject alternative name in a certificate during host verification, current test cert does not have one
        "okhttp" to "3.9.1",
        "undertow" to "2.3.12.Final",
        "spring_web" to "6.1.6",
        "failsafe" to "2.4.4",
        "junit_jupiter" to "5.10.2",
        "testcontainers" to "1.19.8",
        "spring" to "3.2.4",
        "assertj" to "3.25.3",
        "allure" to "2.24.0"
    )

    // https://chronicle.software/chronicle-support-java-17/
    extra["chronicleMapJvmArgs"] = listOf(
        "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
        "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED"
    )

    val chronicleMapJvmArgs = (extra["chronicleMapJvmArgs"] as? List<String>) ?: emptyList()

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.slf4j:slf4j-api:2.0.4")
        implementation("org.apache.commons:commons-lang3:3.14.0")

        // Allure Spock adapter
        testImplementation(platform(rootProject.libs.allure.bom))
        testImplementation("io.qameta.allure:allure-spock2")
        testImplementation("io.qameta.allure:allure-junit-platform")

        // Spock framework
        testImplementation(platform(rootProject.libs.spock.bom))
        testImplementation("org.spockframework:spock-core")

        testImplementation("junit:junit:4.11")
        testImplementation("com.tngtech.java:junit-dataprovider:1.10.0")
        testImplementation("pl.pragmatists:JUnitParams:1.0.2")
        testImplementation("org.mockito:mockito-core:5.11.0")
        testImplementation(rootProject.libs.assertj.core)
        testImplementation("org.awaitility:awaitility:4.2.1")

        annotationProcessor(rootProject.libs.spring.boot.configuration.processor)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        val args = mutableListOf<String>()
        if (project.hasProperty("tests.timeout.multiplier")) {
            args += "-Dtests.timeout.multiplier=${project.property("tests.timeout.multiplier")}"
        }
        args += chronicleMapJvmArgs
        jvmArgs = args
    }
}

configure(subprojects.filter { it != project(":integration-tests") }) {
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    java {
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<Javadoc> {
        options {
            (this as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
        }
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = project.name
                from(components["java"])
                pom {
                    name = project.name
                    description = "Fast and reliable message broker built on top of Kafka."
                    url = "https://github.com/allegro/hermes"
                    inceptionYear = "2015"
                    licenses {
                        license {
                            name = "The Apache Software License, Version 2.0"
                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    developers {
                        developer {
                            id = "skyeden"
                            name = "Skylab Eden Team"
                        }
                    }
                    scm {
                        url = "https://github.com/allegro/hermes"
                        connection = "scm:git@github.com:allegro/hermes.git"
                        developerConnection = "scm:git@github.com:allegro/hermes.git"
                    }
                }
            }
        }
    }

    if (System.getenv("GPG_KEY_ID") != null) {
        signing {
            useInMemoryPgpKeys(
                System.getenv("GPG_KEY_ID"),
                System.getenv("GPG_PRIVATE_KEY"),
                System.getenv("GPG_PRIVATE_KEY_PASSWORD")
            )
            sign(publishing.publications["mavenJava"])
        }
    }
}

subprojects {
    configurations.all {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "log4j", module = "log4j")

        resolutionStrategy {
            force("org.jboss.logging:jboss-logging:3.2.1.Final")
            force("com.google.guava:guava:${rootProject.libs.versions.guava.get()}")
            force("com.fasterxml.jackson.core:jackson-databind:${rootProject.libs.versions.jackson.get()}")
            force("com.fasterxml.jackson.core:jackson-annotations:${rootProject.libs.versions.jackson.get()}")
            force("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:${rootProject.libs.versions.jackson.get()}")
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked,deprecation"))
    }

    tasks.withType<Test> {
        reports {
            html.required = false
            junitXml.required = true
            junitXml.outputLocation = layout.buildDirectory.dir("test-results/$name")
        }

        testLogging {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            events = setOf(
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.FAILED
            )
        }
    }
}
