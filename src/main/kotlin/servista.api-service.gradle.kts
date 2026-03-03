// servista.api-service -- Convention plugin for HTTP API services.
// Provides: Ktor server + Koin DI + kotlinx.serialization.
// Composes on top of servista.library (Kotlin/JVM 21, detekt, ktfmt).

plugins {
    id("servista.library")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.4.0")
    implementation("io.ktor:ktor-server-netty:3.4.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.0")
    implementation("io.ktor:ktor-server-status-pages:3.4.0")
    implementation("io.ktor:ktor-server-auth:3.4.0")
    implementation("io.ktor:ktor-server-auth-jwt:3.4.0")
    implementation("io.ktor:ktor-server-call-logging:3.4.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.0")
    implementation("io.insert-koin:koin-ktor:4.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}
