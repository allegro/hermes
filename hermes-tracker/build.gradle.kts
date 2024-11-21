val versions = rootProject.extra["versions"] as Map<*, *>

dependencies {
    implementation(project(":hermes-api"))
    implementation(project(":hermes-metrics"))
    testImplementation(project(path = ":hermes-test-helper"))
    testRuntimeOnly(group = "org.junit.vintage", name = "junit-vintage-engine", version = versions["junit_jupiter"] as String)
}

val testArtifacts: Configuration by configurations.creating

tasks.register<Jar>("testJar") {
    archiveClassifier.set("tests")
    from(sourceSets["test"].output)
}

artifacts {
    add("testArtifacts", tasks.named("testJar"))
}
