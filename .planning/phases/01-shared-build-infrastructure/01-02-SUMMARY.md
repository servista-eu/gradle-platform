---
phase: 01-shared-build-infrastructure
plan: 02
subsystem: infra
tags: [gradle, convention-plugins, ktor, jooq, kafka, avro, testcontainers, observability, kotlin-dsl]

# Dependency graph
requires:
  - phase: 03-shared-build-infrastructure
    provides: gradle-platform skeleton with servista.library base plugin and version catalog
provides:
  - 8 domain convention plugins composing on servista.library base
  - servista.api-service (Ktor + Koin + kotlinx.serialization)
  - servista.jooq (jOOQ + HikariCP + Flyway + PostgreSQL)
  - servista.kafka-producer and kafka-consumer (Kafka clients)
  - servista.pipeline-service (Kafka Streams)
  - servista.avro (Avro + Apicurio SerDes)
  - servista.testing (JUnit 5 + Testcontainers + MockK + Kotest + Ktor test host)
  - servista.observability (kotlin-logging + Logback + Micrometer)
  - Complete 9-plugin inventory registered in gradlePlugin block
affects: [03-shared-build-infrastructure, 04-commons-sdk, 07-service-scaffold-template]

# Tech tracking
tech-stack:
  added: [ktor-3.4.0, koin-4.1.1, kotlinx-serialization-1.10.0, jooq-3.20.11, hikaricp-7.0.2, flyway-12.0.3, postgresql-jdbc-42.7.10, kafka-clients-4.1.1, kafka-streams-4.1.1, avro-1.12.1, apicurio-serdes-3.0.6, junit5-5.14.2, testcontainers-2.0.3, mockk-1.14.7, kotest-6.1.4, kotlin-logging-7.0.3, logback-1.5.32, micrometer-1.16.3]
  patterns: [composable-convention-plugins, dependency-only-plugins, semantic-plugin-separation]

key-files:
  created:
    - infrastructure/gradle-platform/src/main/kotlin/servista.api-service.gradle.kts
    - infrastructure/gradle-platform/src/main/kotlin/servista.jooq.gradle.kts
    - infrastructure/gradle-platform/src/main/kotlin/servista.kafka-producer.gradle.kts
    - infrastructure/gradle-platform/src/main/kotlin/servista.kafka-consumer.gradle.kts
    - infrastructure/gradle-platform/src/main/kotlin/servista.pipeline-service.gradle.kts
    - infrastructure/gradle-platform/src/main/kotlin/servista.avro.gradle.kts
    - infrastructure/gradle-platform/src/main/kotlin/servista.testing.gradle.kts
    - infrastructure/gradle-platform/src/main/kotlin/servista.observability.gradle.kts
  modified:
    - infrastructure/gradle-platform/build.gradle.kts

key-decisions:
  - "Precompiled script plugin naming: Gradle uses camelCase for hyphenated names (e.g., Servista_apiServicePlugin, not Servista_api_servicePlugin)"
  - "OTel agent excluded from observability plugin -- runtime JVM agent, not compile dependency (Phase 7 territory)"
  - "kafka-producer and kafka-consumer are separate plugins for semantic clarity despite identical dependencies"

patterns-established:
  - "Domain convention plugins apply servista.library as base then add domain-specific dependencies"
  - "All dependency versions hard-coded matching ADR-017 compatibility matrix and published catalog"
  - "Testing plugin uses testImplementation scope; all other domain plugins use implementation scope"

requirements-completed: [FOUND-02]

# Metrics
duration: 5min
completed: 2026-03-03
---

# Phase 3 Plan 02: Domain Convention Plugins Summary

**8 composable domain convention plugins (api-service, jooq, kafka-producer, kafka-consumer, pipeline-service, avro, testing, observability) all compiling on servista.library base with ADR-017 version-matched dependencies**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-03T12:12:01Z
- **Completed:** 2026-03-03T12:16:54Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments
- All 9 convention plugins compile via `./gradlew classes` without errors
- 9 plugin descriptors generated via `./gradlew pluginDescriptors`
- All 8 domain plugins compose on top of servista.library (Kotlin 2.3/JVM 21, detekt, ktfmt)
- Every dependency version matches ADR-017 compatibility matrix exactly
- Complete plugin inventory registered in gradlePlugin block with verified implementation class names

## Task Commits

Each task was committed atomically:

1. **Task 1: Create API-oriented convention plugins (api-service, jooq, kafka-producer, kafka-consumer)** - `d5aba3c` (feat)
2. **Task 2: Create infrastructure convention plugins (pipeline-service, avro, testing, observability)** - `0cb9a2a` (feat)

## Files Created/Modified
- `infrastructure/gradle-platform/src/main/kotlin/servista.api-service.gradle.kts` - Ktor + Koin + kotlinx.serialization + serialization compiler plugin
- `infrastructure/gradle-platform/src/main/kotlin/servista.jooq.gradle.kts` - jOOQ + HikariCP + Flyway + PostgreSQL JDBC
- `infrastructure/gradle-platform/src/main/kotlin/servista.kafka-producer.gradle.kts` - Kafka producer client
- `infrastructure/gradle-platform/src/main/kotlin/servista.kafka-consumer.gradle.kts` - Kafka consumer client
- `infrastructure/gradle-platform/src/main/kotlin/servista.pipeline-service.gradle.kts` - Kafka Streams
- `infrastructure/gradle-platform/src/main/kotlin/servista.avro.gradle.kts` - Avro + Apicurio Registry SerDes
- `infrastructure/gradle-platform/src/main/kotlin/servista.testing.gradle.kts` - JUnit 5 + Testcontainers + MockK + Kotest + Ktor test host + Koin test
- `infrastructure/gradle-platform/src/main/kotlin/servista.observability.gradle.kts` - kotlin-logging + Logback + Micrometer
- `infrastructure/gradle-platform/build.gradle.kts` - All 9 plugin registrations in gradlePlugin block

## Decisions Made
- Gradle precompiled script plugins use camelCase for hyphenated names (e.g., `Servista_apiServicePlugin` not `Servista_api_servicePlugin`) -- verified by inspecting generated class files
- OpenTelemetry Java Agent excluded from observability plugin as it is a runtime JVM agent, not a compile dependency; OTel agent attachment is Phase 7 (Service Scaffold Template) territory per user decision
- kafka-producer and kafka-consumer share identical `kafka-clients` dependency but remain separate plugins for semantic clarity and future differentiation

## Deviations from Plan

None - plan executed exactly as written.

Note: The plan predicted underscore-based class names (e.g., `Servista_api_servicePlugin`) but Gradle actually generates camelCase (e.g., `Servista_apiServicePlugin`). The plan's CRITICAL instruction to verify generated class names prevented this from becoming an issue -- class names were verified before registration.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All 9 convention plugins ready for Plan 03 (TestKit verification tests)
- Full plugin inventory available for downstream service scaffolding (Phase 7)
- Typical compositions documented: API service = api-service + jooq + kafka-producer + avro + testing + observability

## Self-Check: PASSED

---
*Phase: 03-shared-build-infrastructure*
*Completed: 2026-03-03*
