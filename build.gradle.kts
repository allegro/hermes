import org.gradle.api.tasks.testing.logging.TestExceptionFormat
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
        implementation(rootProject.libs.commons.lang3)
        implementation(rootProject.libs.slf4j.api)

        // Allure Spock adapter
        testImplementation(platform(rootProject.libs.allure.bom))
        testImplementation(rootProject.libs.allure.junit.platform)
        testImplementation(rootProject.libs.allure.spock2)

        // Spock framework
        testImplementation(platform(rootProject.libs.spock.bom))
        testImplementation(rootProject.libs.spock.core)

        testImplementation(rootProject.libs.assertj.core)
        testImplementation(rootProject.libs.awaitility)
        testImplementation(rootProject.libs.junit)
        testImplementation(rootProject.libs.junit.dataprovider)
        testImplementation(rootProject.libs.junit.params)
        testImplementation(rootProject.libs.mockito.core)

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
            force("org.jboss.logging:jboss-logging:${rootProject.libs.versions.jboss.logging.get()}")
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
            exceptionFormat = TestExceptionFormat.FULL
            events = setOf(
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.FAILED
            )
        }
    }
}
