# Phase 3: Shared Build Infrastructure - Context

**Gathered:** 2026-03-03
**Status:** Ready for planning

**IMPORTANT: Phase scope change.** This phase was originally "Monorepo Scaffold" but the user decided against a monorepo. Services stay in separate Forgejo repos (created in Phase 1). This phase creates shared build infrastructure that all service repos consume. ADR-017 must be amended to reflect the multi-repo decision.

<domain>
## Phase Boundary

Create a `gradle-platform` repository containing Gradle convention plugins and a version catalog, published to Forgejo's Maven package registry. All service repos consume these artifacts to get consistent dependency versions, build configuration, and code quality tooling. This phase does NOT create service repos or service code — it creates the shared build infrastructure that downstream phases depend on.

</domain>

<decisions>
## Implementation Decisions

### Multi-repo over monorepo
- **Decision: NO monorepo.** Services stay in separate Forgejo repos (Phase 1 repos remain the code homes).
- User rationale: Forgejo already provides an artifact registry. Prefer small, focused repos over one large monolith.
- Shared libraries published to Forgejo Maven package registry.
- Convention plugins published as Gradle plugin artifacts to Forgejo Maven registry.
- ADR-017 must be amended: replace "Gradle monorepo" with "multi-repo with shared gradle-platform" and update the monorepo structure section.

### Build infrastructure repo
- **Repo name:** `gradle-platform` (no type prefix — it's a unique cross-cutting repo)
- **Local filesystem:** `infrastructure/gradle-platform` (alongside `infra-gateway`)
- **Contents:** Convention plugins + version catalog, all in one repo
- **No separate repos** for plugins vs catalog — single repo, single publish pipeline, single version to track

### Convention plugins — composable set of 9
- **Dependencies-only scope:** Plugins pull in the right dependencies and set Kotlin/JVM targets. Runtime configuration (logging format, health endpoints, OTel wiring) is Phase 7 (Service Scaffold Template) territory.
- **Code quality included in base plugin:** detekt + ktfmt configured automatically for all projects applying any servista plugin.
- **Enforce standard directory structure:** Convention plugins validate expected directories exist (src/main/kotlin, src/main/resources, src/test/kotlin). Build fails if structure is wrong.

Plugin inventory:

| Plugin | Provides | Typical consumers |
|--------|----------|-------------------|
| `servista.library` | Kotlin 2.3 + JVM 21 target + detekt + ktfmt (base plugin) | All projects |
| `servista.api-service` | Ktor + HTTP concerns | API services |
| `servista.jooq` | jOOQ + HikariCP + Flyway + PostgreSQL JDBC driver | Services with database |
| `servista.kafka-producer` | Kafka producer client | Services emitting events |
| `servista.kafka-consumer` | Kafka consumer client | Event sinks, consumers |
| `servista.pipeline-service` | Kafka Streams | Silver streaming, pipeline services |
| `servista.avro` | Avro + Apicurio SerDes | Services using Avro serialization |
| `servista.testing` | JUnit 5 + Testcontainers + MockK + Kotest assertions | All projects needing tests |
| `servista.observability` | OpenTelemetry agent + Micrometer + kotlin-logging + Logback | Deployed services |

Typical compositions:
- **API service** (e.g., api-accounts): `api-service` + `jooq` + `kafka-producer` + `avro` + `testing` + `observability`
- **Event sink** (e.g., svc-audit-writer): `library` + `kafka-consumer` + `avro` + `testing` + `observability`
- **Pipeline** (e.g., silver-streaming): `pipeline-service` + `avro` + `testing` + `observability`
- **Shared lib** (e.g., servista-commons): `library` + `testing`

### Version catalog distribution
- **Published version catalog** to Forgejo Maven package registry
- Service repos import in `settings.gradle.kts` with `from("eu.servista:gradle-platform-catalog:x.y.z")`
- **Strict — no overrides:** All services use exactly the versions from the catalog. Version changes go through gradle-platform.
- **Versioning strategy:** Version ranges (e.g., `[1.0,)`) during development for velocity; pinned to specific versions (e.g., `1.2.0`) for production builds.
- **No BOM** — version catalog is sufficient for Gradle consumers. BOMs are only needed for Maven consumers (which don't exist).

### Plugin verification
- **Gradle TestKit** for integration testing — programmatic testing of convention plugins within CI
- No example/hello-world service repo needed — TestKit tests prove plugins work
- Phase 3 success criteria ("hello world service builds successfully") met by TestKit test that creates a minimal project, applies plugins, and verifies compilation

### Claude's Discretion
- Exact gradle-platform repo directory structure and module layout
- Convention plugin implementation details (which Gradle APIs to use)
- TestKit test structure and assertions
- How to bootstrap new service repos (template, init task, or documentation — Claude decides the most practical approach)
- detekt and ktfmt rule configuration details
- Gradle publication configuration for Forgejo Maven registry

</decisions>

<specifics>
## Specific Ideas

- User explicitly chose composable plugins over monolithic ones — the ability to swap Kafka for another broker was a motivating factor
- Code quality tools (detekt + ktfmt) must be part of the base plugin, not a separate opt-in — every project gets consistent formatting from day one
- Version ranges during dev / pinned for production was the user's idea — they want fast iteration during development without manual version bumps, but stability guarantees for production

</specifics>

<code_context>
## Existing Code Insights

### Reusable Assets
- ADR-017 (`architecture/decisions/017-backend-tech-stack.md`): Contains the complete version compatibility matrix that populates `libs.versions.toml`. Must be amended to replace monorepo with multi-repo approach.
- STACK.md (`.planning/research/STACK.md`): Comprehensive technology evaluation with version catalog template

### Established Patterns
- Governance repo is documentation-only — no source code
- Forgejo hosting with token `e512a722d8fb6cb697ea9a11c9583f9aff400a34` for API access
- Phase 1 repo naming: `<type>-<name>` convention with repos organized by type in filesystem (apis/, services/, pipelines/, integrations/, apps/)

### Integration Points
- All service repos created in Phase 1 will consume the published convention plugins and version catalog
- Phase 4 (Commons SDK) will be the first shared library published to Forgejo Maven registry, depending on the convention plugins from this phase
- Phase 7 (Service Scaffold Template) will add runtime configuration on top of the dependency-only convention plugins from this phase
- Forgejo Maven package registry is the artifact distribution channel

</code_context>

<deferred>
## Deferred Ideas

- **ADR-017 amendment:** Must be updated to replace "Gradle monorepo" with "multi-repo with shared gradle-platform." This is a prerequisite for this phase's planning but is a documentation update, not a new capability.
- **Roadmap update:** Phase name should change from "Monorepo Scaffold" to "Shared Build Infrastructure" to reflect the new scope.

</deferred>

---

*Phase: 03-shared-build-infrastructure*
*Context gathered: 2026-03-03*
