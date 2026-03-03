// servista.kafka-consumer -- Convention plugin for services that consume Kafka events.
// Provides: Kafka consumer client.
// Composes on top of servista.library (Kotlin/JVM 21, detekt, ktfmt).
// Separate from kafka-producer for semantic clarity and future differentiation.

plugins {
    id("servista.library")
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:4.1.1")
}
