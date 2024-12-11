val versions = rootProject.extra["versions"] as Map<*, *>

dependencies {
    implementation(project(":hermes-api"))
    implementation(project(":hermes-metrics"))
    testImplementation(project(path = ":hermes-test-helper"))
    testRuntimeOnly(libs.junit.vintage.engine)
}

val testArtifacts: Configuration by configurations.creating

tasks.register<Jar>("testJar") {
    archiveClassifier = "tests"
    from(sourceSets["test"].output)
}

artifacts {
    add("testArtifacts", tasks.named("testJar"))
}
