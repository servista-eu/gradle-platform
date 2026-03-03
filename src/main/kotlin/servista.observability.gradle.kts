// servista.observability -- Convention plugin for observability.
// Provides: kotlin-logging + Logback + Micrometer Prometheus registry.
// Composes on top of servista.library (Kotlin/JVM 21, detekt, ktfmt).
// Note: OpenTelemetry Java Agent is a JVM agent attached at runtime (Phase 7),
// not a compile-time dependency. This plugin covers the logging and metrics libraries.

plugins {
    id("servista.library")
}

dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.3")
}
