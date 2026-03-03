// servista.avro -- Convention plugin for Avro serialization.
// Provides: Avro + Apicurio Registry SerDes for Kafka schema integration.
// Composes on top of servista.library (Kotlin/JVM 21, detekt, ktfmt).

plugins {
    id("servista.library")
}

dependencies {
    implementation("org.apache.avro:avro:1.12.1")
    implementation("io.apicurio:apicurio-registry-serdes-avro-serde:3.0.0.M4")
}
