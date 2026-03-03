// servista.testing -- Convention plugin for test dependencies.
// Provides: JUnit 5 + Testcontainers + MockK + Kotest assertions + Ktor test host + Koin test.
// Composes on top of servista.library (Kotlin/JVM 21, detekt, ktfmt).
// Main value is the comprehensive test dependency set with JUnit Platform configuration.

plugins {
    id("servista.library")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.2")
    testImplementation("io.mockk:mockk:1.14.7")
    testImplementation("io.kotest:kotest-assertions-core:6.1.4")
    testImplementation("org.testcontainers:testcontainers:2.0.3")
    testImplementation("org.testcontainers:postgresql:2.0.3")
    testImplementation("org.testcontainers:kafka:2.0.3")
    testImplementation("io.ktor:ktor-server-test-host:3.4.0")
    testImplementation("io.insert-koin:koin-test:4.1.1")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
