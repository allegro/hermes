dependencies {
    implementation(project(":hermes-api"))

    implementation(libs.avro)
    implementation(libs.guava)

    testImplementation(project(path = ":hermes-test-helper"))

    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)

}
