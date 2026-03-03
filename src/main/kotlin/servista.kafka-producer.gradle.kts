// servista.kafka-producer -- Convention plugin for services that produce Kafka events.
// Provides: Kafka producer client.
// Composes on top of servista.library (Kotlin/JVM 21, detekt, ktfmt).
// Separate from kafka-consumer for semantic clarity and future differentiation.

plugins {
    id("servista.library")
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:4.1.1")
}
