// servista.pipeline-service -- Convention plugin for Kafka Streams pipeline services.
// Provides: Kafka Streams library.
// Composes on top of servista.library (Kotlin/JVM 21, detekt, ktfmt).

plugins {
    id("servista.library")
}

dependencies {
    implementation("org.apache.kafka:kafka-streams:4.1.1")
}
