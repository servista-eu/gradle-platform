# Gradle Platform

Shared Gradle build infrastructure for the Servista ecosystem. Provides composable convention plugins and a published version catalog that enforce consistent build configurations across all Kotlin/JVM service repositories.

Published to the Forgejo Maven registry as `eu.servista:gradle-platform:0.1.0`.

## Convention Plugins

All plugins are composable and build on top of the base `servista.library` plugin.

| Plugin | Purpose | Key Dependencies |
|--------|---------|------------------|
| `servista.library` | Base plugin — Kotlin/JVM 21, detekt, ktfmt | kotlinx-coroutines, kotlinx-datetime |
| `servista.api-service` | HTTP API services | Ktor 3.4, Koin 4.1, kotlinx-serialization |
| `servista.jooq` | Database access | jOOQ 3.20, HikariCP 7.0, Flyway 12.0, PostgreSQL |
| `servista.kafka-producer` | Kafka event producers | Kafka clients 4.1 |
| `servista.kafka-consumer` | Kafka event consumers | Kafka clients 4.1 |
| `servista.pipeline-service` | Kafka Streams pipelines | Kafka Streams 4.1 |
| `servista.avro` | Avro schema serialization | Avro 1.12, Apicurio Registry SerDes 3.0 |
| `servista.testing` | Test infrastructure | JUnit 5, Testcontainers, MockK, Kotest |
| `servista.observability` | Logging and metrics | kotlin-logging, Logback, Micrometer Prometheus |

### Usage

In a consuming project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from("eu.servista:gradle-platform-catalog:0.1.0")
        }
    }
}

pluginManagement {
    repositories {
        maven {
            url = uri("https://forgejo.servista.eu/api/packages/servista/maven")
        }
        gradlePluginPortal()
    }
}
```

In `build.gradle.kts`:

```kotlin
plugins {
    id("servista.api-service")
    id("servista.jooq")
    id("servista.testing")
    id("servista.observability")
}
```

## Version Catalog

The published version catalog (`catalog/libs.versions.toml`) contains 31 version definitions and 99 library entries covering the full Servista technology stack: Ktor, Koin, jOOQ, Flyway, Kafka, Avro, Apicurio, OpenFGA, Micrometer, Testcontainers, and more.

## Code Quality

The base `servista.library` plugin enforces:

- **detekt** — static analysis with zero-tolerance policy (`maxIssues: 0`)
- **ktfmt** — Kotlin official code formatting style

A shared detekt configuration is bundled at `src/main/resources/detekt/detekt.yml`.

## Building

```bash
./gradlew build
```

This runs compilation, detekt, and functional tests (Gradle TestKit).

## Publishing

```bash
./gradlew publish
```

Requires a Forgejo token configured via `~/.gradle/gradle.properties` (`forgejo.token`) or the `FORGEJO_TOKEN` environment variable.

Two artifacts are published:

- `eu.servista:gradle-platform:0.1.0` — convention plugins
- `eu.servista:gradle-platform-catalog:0.1.0` — version catalog
