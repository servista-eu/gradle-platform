# Phase 3: Shared Build Infrastructure - Research

**Researched:** 2026-03-03
**Domain:** Gradle convention plugins, version catalog publishing, Forgejo Maven registry
**Confidence:** HIGH

## Summary

This phase creates a standalone `gradle-platform` repository on Forgejo containing 9 composable convention plugins and a published version catalog. All downstream service repos consume these artifacts via the Forgejo Maven package registry for consistent builds, dependency versions, and code quality enforcement.

The core technical challenge is structuring a Gradle project that publishes both convention plugins (as Gradle plugin artifacts with marker artifacts) and a version catalog (as a separate Maven publication) to the Forgejo Maven registry. Consumers then configure `pluginManagement` and `dependencyResolutionManagement` in their `settings.gradle.kts` to resolve plugins and catalog from Forgejo. The approach is well-supported by Gradle 9.x with the `java-gradle-plugin`, `maven-publish`, and `version-catalog` plugins working together.

A critical finding is the detekt/Kotlin 2.3 compatibility gap: the stable detekt 1.23.8 cannot analyze Kotlin 2.3 metadata (produces false positives). Only detekt 2.0.0-alpha.2 supports Kotlin 2.3.0, but it is an alpha release. This is a known trade-off that must be accepted for this phase. The ktfmt Gradle plugin (0.25.0) works independently of Kotlin version since it operates on source text, not compiled metadata.

**Primary recommendation:** Use precompiled script plugins in a `kotlin-dsl` project with `java-gradle-plugin` + `maven-publish` for convention plugins, and `version-catalog` + `maven-publish` for the catalog, both publishing to Forgejo Maven registry at `https://forgejo.servista.eu/api/packages/servista/maven`. Use detekt 2.0.0-alpha.2 (the only Kotlin 2.3 compatible version) and ktfmt 0.25.0 for code quality.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **Multi-repo over monorepo.** NO monorepo. Services stay in separate Forgejo repos (Phase 1 repos remain the code homes). Shared libraries published to Forgejo Maven package registry. Convention plugins published as Gradle plugin artifacts to Forgejo Maven registry. ADR-017 must be amended: replace "Gradle monorepo" with "multi-repo with shared gradle-platform" and update the monorepo structure section.
- **Build infrastructure repo.** Repo name: `gradle-platform` (no type prefix). Local filesystem: `infrastructure/gradle-platform`. Contents: Convention plugins + version catalog, all in one repo. No separate repos for plugins vs catalog -- single repo, single publish pipeline, single version to track.
- **Convention plugins -- composable set of 9.** Dependencies-only scope: Plugins pull in the right dependencies and set Kotlin/JVM targets. Runtime configuration (logging format, health endpoints, OTel wiring) is Phase 7 territory. Code quality included in base plugin: detekt + ktfmt configured automatically. Enforce standard directory structure: Convention plugins validate expected directories exist (src/main/kotlin, src/main/resources, src/test/kotlin). Build fails if structure is wrong.
- **Plugin inventory:**
  - `servista.library` -- Kotlin 2.3 + JVM 21 target + detekt + ktfmt (base plugin)
  - `servista.api-service` -- Ktor + HTTP concerns
  - `servista.jooq` -- jOOQ + HikariCP + Flyway + PostgreSQL JDBC driver
  - `servista.kafka-producer` -- Kafka producer client
  - `servista.kafka-consumer` -- Kafka consumer client
  - `servista.pipeline-service` -- Kafka Streams
  - `servista.avro` -- Avro + Apicurio SerDes
  - `servista.testing` -- JUnit 5 + Testcontainers + MockK + Kotest assertions
  - `servista.observability` -- OpenTelemetry agent + Micrometer + kotlin-logging + Logback
- **Version catalog distribution.** Published version catalog to Forgejo Maven package registry. Service repos import in `settings.gradle.kts` with `from("eu.servista:gradle-platform-catalog:x.y.z")`. Strict -- no overrides: All services use exactly the versions from the catalog. Versioning strategy: Version ranges during development, pinned for production.
- **Plugin verification.** Gradle TestKit for integration testing. No example/hello-world service repo needed. TestKit test that creates a minimal project, applies plugins, and verifies compilation.

### Claude's Discretion
- Exact gradle-platform repo directory structure and module layout
- Convention plugin implementation details (which Gradle APIs to use)
- TestKit test structure and assertions
- How to bootstrap new service repos (template, init task, or documentation)
- detekt and ktfmt rule configuration details
- Gradle publication configuration for Forgejo Maven registry

### Deferred Ideas (OUT OF SCOPE)
- ADR-017 amendment -- documented as a deliverable of this phase but is a documentation update, not a new capability
- Roadmap update -- Phase name change from "Monorepo Scaffold" to "Shared Build Infrastructure"
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| FOUND-02 | Shared build infrastructure -- `gradle-platform` repo with 9 composable convention plugins and published version catalog on Forgejo Maven registry | Full research coverage: convention plugin architecture, version catalog publishing, Forgejo Maven registry authentication, TestKit verification, detekt/ktfmt integration, all version compatibility verified |
</phase_requirements>

## Standard Stack

### Core (Convention Plugin Build Infrastructure)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Gradle | 9.3.1 | Build system | Kotlin DSL, version catalogs, convention plugins, composite builds; required by Kotlin 2.3 |
| `kotlin-dsl` plugin | (bundled with Gradle 9.3.1) | Enables precompiled script plugins | Official Gradle mechanism for convention plugins in Kotlin |
| `java-gradle-plugin` | (bundled with Gradle 9.3.1) | Plugin development plugin | Generates plugin descriptors, marker artifacts, integrates with maven-publish |
| `maven-publish` | (bundled with Gradle 9.3.1) | Publishing to Maven repositories | Standard for publishing to any Maven-compatible registry |
| `version-catalog` plugin | (bundled with Gradle 9.3.1) | Publishing version catalogs | Creates the `versionCatalog` component for maven-publish |

### Code Quality Tooling

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| detekt | 2.0.0-alpha.2 | Static code analysis for Kotlin | Only version supporting Kotlin 2.3.0 metadata; stable 1.23.8 produces false positives with Kotlin 2.3 |
| ktfmt Gradle plugin | 0.25.0 | Kotlin code formatting | Facebook-maintained, works on source text (no Kotlin version coupling), Gradle plugin by cortinico |

### Testing

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Gradle TestKit | (bundled with Gradle 9.3.1) | Functional testing of convention plugins | Official Gradle testing framework; `GradleRunner` executes real builds |
| JUnit 5 | 5.14.2 | Test framework for TestKit tests | Standard JVM test framework per ADR-017 |
| Kotest assertions | 6.1.4 | Assertion library | Kotlin-idiomatic assertions per ADR-017 |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Precompiled script plugins | Class-based plugins | Class-based compile once and are 94% faster at configuration time (12.7s -> 0.76s in benchmarks), but precompiled scripts are easier to write/maintain and sufficient for 9 convention plugins in a standalone repo where configuration time is not critical |
| detekt 2.0.0-alpha.2 | detekt 1.23.8 (stable) | Stable version produces false positives with Kotlin 2.3.0 metadata; alpha is the only working option. Monitor for stable release. |
| detekt 2.0.0-alpha.2 | No static analysis | Unacceptable -- code quality is a locked decision. Accept alpha risk. |
| ktfmt | ktlint | ktfmt is opinionated with zero configuration by design; ktlint requires rule configuration. Both work. ktfmt chosen for simplicity. |

**Installation (convention plugin project):**
```kotlin
// gradle-platform/build.gradle.kts
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    `version-catalog`
}

dependencies {
    // External plugins used BY convention plugins (version resolved here, applied by ID in scripts)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.2")
    implementation("com.ncorti.ktfmt.gradle:0.25.0")
    // Note: Ktor, jOOQ etc. are NOT needed here -- convention plugins only add
    // dependencies to consumers via the version catalog, they don't compile against them
}
```

## Architecture Patterns

### Recommended Repository Structure

```
gradle-platform/
  build.gradle.kts                    # Plugin project: kotlin-dsl + java-gradle-plugin + maven-publish + version-catalog
  settings.gradle.kts                 # Root project name, plugin repositories
  gradle.properties                   # Group, version, Forgejo token reference
  gradle/
    libs.versions.toml                # Internal dependencies for building the plugins themselves
  src/
    main/
      kotlin/
        eu/servista/gradle/
          ServistaDirStructureCheck.kt   # Shared utility: directory validation logic
          ServistaBasePlugin.kt          # Optional: shared apply logic if needed
        servista.library.gradle.kts      # Base plugin: Kotlin 2.3, JVM 21, detekt, ktfmt, dir check
        servista.api-service.gradle.kts  # Ktor + HTTP deps (applies servista.library)
        servista.jooq.gradle.kts         # jOOQ + HikariCP + Flyway + PostgreSQL JDBC
        servista.kafka-producer.gradle.kts
        servista.kafka-consumer.gradle.kts
        servista.pipeline-service.gradle.kts  # Kafka Streams
        servista.avro.gradle.kts              # Avro + Apicurio SerDes
        servista.testing.gradle.kts           # JUnit 5 + Testcontainers + MockK + Kotest
        servista.observability.gradle.kts     # OTel agent + Micrometer + kotlin-logging + Logback
      resources/
        detekt/
          detekt.yml                   # Shared detekt configuration (rules, thresholds)
    functionalTest/
      kotlin/
        eu/servista/gradle/
          ApiServicePluginTest.kt      # TestKit: applies api-service + jooq + kafka-producer, verifies compilation
          LibraryPluginTest.kt         # TestKit: applies library, verifies Kotlin/JVM config
          CompositionTest.kt           # TestKit: multi-plugin composition test
  catalog/
    libs.versions.toml                 # THE published version catalog (all dependency versions from ADR-017)
```

**Key structural decision:** The `catalog/libs.versions.toml` is separate from `gradle/libs.versions.toml`. The `gradle/` catalog is for building the plugin project itself (Gradle plugin dependencies). The `catalog/` catalog is what gets published and consumed by service repos.

### Pattern 1: Precompiled Script Plugin with External Plugin Application

**What:** Convention plugins are `.gradle.kts` files in `src/main/kotlin/` that get compiled into plugin classes automatically. External plugins are referenced by ID after their Gradle plugin dependency is added to the build.
**When to use:** For all 9 convention plugins.
**Example:**

```kotlin
// src/main/kotlin/servista.library.gradle.kts
// This file IS the plugin. Its ID is "servista.library" (derived from filename).

plugins {
    kotlin("jvm")            // Applied from org.jetbrains.kotlin:kotlin-gradle-plugin dependency
    id("dev.detekt")         // Applied from dev.detekt:detekt-gradle-plugin dependency
    id("com.ncorti.ktfmt.gradle")  // Applied from com.ncorti.ktfmt.gradle dependency
}

kotlin {
    jvmToolchain(21)
}

// Directory structure validation
val requiredDirs = listOf(
    "src/main/kotlin",
    "src/main/resources",
    "src/test/kotlin"
)

tasks.register("validateStructure") {
    doLast {
        requiredDirs.forEach { dir ->
            val dirFile = project.file(dir)
            require(dirFile.exists()) {
                "Required directory '$dir' does not exist. " +
                "Servista convention plugins require standard directory structure."
            }
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("validateStructure")
}

// detekt configuration
detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.from(
        resources.text.fromUri(
            Thread.currentThread().contextClassLoader
                .getResource("detekt/detekt.yml")!!.toURI()
        )
    )
}

// ktfmt configuration
ktfmt {
    kotlinLangStyle()  // or googleStyle() -- Claude's discretion
}
```

### Pattern 2: Composable Plugin Application

**What:** Higher-level plugins apply `servista.library` as their base, then add domain-specific dependencies.
**When to use:** For all plugins except `servista.library` itself.
**Example:**

```kotlin
// src/main/kotlin/servista.api-service.gradle.kts

plugins {
    id("servista.library")  // Gets Kotlin, JVM 21, detekt, ktfmt for free
}

// Ktor dependencies from the published version catalog
// Note: In a precompiled script plugin, we reference versions directly
// because the version catalog is not available during plugin application.
// Dependencies are added using coordinates from ADR-017.
dependencies {
    implementation("io.ktor:ktor-server-core:3.4.0")
    implementation("io.ktor:ktor-server-netty:3.4.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.0")
    implementation("io.ktor:ktor-server-status-pages:3.4.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.0")
    implementation("io.insert-koin:koin-ktor:4.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}
```

**IMPORTANT DESIGN DECISION -- Dependencies in plugins vs version catalog:**

There are two approaches for how convention plugins provide dependencies:

**Option A (Recommended): Plugins add dependencies using hard-coded coordinates, version catalog provides the consumer's extended dependency surface.**

Convention plugins use literal dependency coordinates (from ADR-017). The published version catalog gives consumers access to the full dependency set for service-specific additions. Plugins guarantee the baseline; catalog provides the menu.

**Option B: Plugins reference the version catalog at configuration time.**

This is possible but complex -- it requires the consuming project to have the catalog already resolved when the plugin applies. Precompiled script plugins do not have direct access to the consumer's version catalog at plugin application time, making this fragile.

**Recommendation: Use Option A.** It is simpler, more reliable, and follows Gradle best practices. The version catalog serves as the single source of truth for ALL versions, and convention plugins hard-code the same versions. When versions change, both the catalog TOML and the convention plugin coordinates are updated in the same commit in `gradle-platform`.

### Pattern 3: Version Catalog Publishing

**What:** Publish the version catalog as a separate Maven artifact alongside the convention plugins.
**When to use:** Once, in the `gradle-platform` build configuration.
**Example:**

```kotlin
// build.gradle.kts (relevant sections)
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    `version-catalog`
}

group = "eu.servista"
version = "0.1.0"

catalog {
    versionCatalog {
        from(files("catalog/libs.versions.toml"))
    }
}

gradlePlugin {
    plugins {
        create("library") {
            id = "servista.library"
            implementationClass = "Servista_libraryPlugin"
            // Note: precompiled script plugins generate class names from filenames
            // servista.library.gradle.kts -> Servista_libraryPlugin
        }
        create("apiService") {
            id = "servista.api-service"
            implementationClass = "Servista_apiServicePlugin"
            // servista.api-service.gradle.kts -> Servista_apiServicePlugin
            // Dashes become camelCase with underscore prefix for package separator
        }
        // ... repeat for all 9 plugins
    }
}

publishing {
    publications {
        // Convention plugins are auto-published by java-gradle-plugin
        // Version catalog needs a manual publication:
        create<MavenPublication>("versionCatalog") {
            from(components["versionCatalog"])
            artifactId = "gradle-platform-catalog"
        }
    }

    repositories {
        maven {
            name = "Forgejo"
            url = uri("https://forgejo.servista.eu/api/packages/servista/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "token ${findProperty("forgejo.token")}"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}
```

### Pattern 4: Consumer-Side Configuration

**What:** How service repos consume convention plugins and version catalog from Forgejo.
**When to use:** In every service repo's `settings.gradle.kts`.
**Example:**

```kotlin
// Service repo: settings.gradle.kts
pluginManagement {
    repositories {
        maven {
            name = "Forgejo"
            url = uri("https://forgejo.servista.eu/api/packages/servista/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "token ${settings.providers.gradleProperty("forgejo.token").get()}"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            name = "Forgejo"
            url = uri("https://forgejo.servista.eu/api/packages/servista/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "token ${settings.providers.gradleProperty("forgejo.token").get()}"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from("eu.servista:gradle-platform-catalog:0.1.0")
        }
    }
}

rootProject.name = "api-accounts"  // or whichever service
```

```kotlin
// Service repo: build.gradle.kts
plugins {
    id("servista.api-service") version "0.1.0"
    id("servista.jooq") version "0.1.0"
    id("servista.kafka-producer") version "0.1.0"
    id("servista.avro") version "0.1.0"
    id("servista.testing") version "0.1.0"
    id("servista.observability") version "0.1.0"
}

dependencies {
    // Service-specific dependencies from version catalog
    implementation(libs.openfga.sdk)
    // ... service-specific only
}
```

### Pattern 5: Gradle TestKit Functional Test

**What:** Integration tests that verify convention plugin composition works end-to-end.
**When to use:** For verifying the success criteria (compilation with multi-plugin composition).
**Example:**

```kotlin
// src/functionalTest/kotlin/eu/servista/gradle/ApiServiceCompositionTest.kt
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import io.kotest.matchers.shouldBe

class ApiServiceCompositionTest {

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        // settings.gradle.kts
        projectDir.resolve("settings.gradle.kts").writeText("""
            rootProject.name = "test-api-service"
        """.trimIndent())

        // Standard directory structure (required by servista.library)
        projectDir.resolve("src/main/kotlin").mkdirs()
        projectDir.resolve("src/main/resources").mkdirs()
        projectDir.resolve("src/test/kotlin").mkdirs()

        // build.gradle.kts applying the plugin composition
        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                id("servista.api-service")
                id("servista.jooq")
                id("servista.kafka-producer")
            }
        """.trimIndent())

        // Minimal Kotlin source file
        projectDir.resolve("src/main/kotlin/App.kt").writeText("""
            package eu.servista.test
            fun main() { println("Hello") }
        """.trimIndent())
    }

    @Test
    fun `api-service + jooq + kafka-producer compiles successfully`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("compileKotlin", "--stacktrace")
            .forwardOutput()
            .build()

        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `build fails without required directory structure`() {
        // Remove required directories
        projectDir.resolve("src/main/resources").deleteRecursively()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("compileKotlin", "--stacktrace")
            .forwardOutput()
            .buildAndFail()

        result.output.contains("Required directory") shouldBe true
    }
}
```

### Anti-Patterns to Avoid

- **Monolithic convention plugin:** Do not create a single plugin that includes all dependencies. The user explicitly chose composable plugins so services can pick only what they need.
- **`buildSrc` for convention plugins:** Deprecated pattern. Changes to `buildSrc` invalidate the entire build cache. Use included build or standalone repo (we are using standalone repo).
- **`allprojects {}` / `subprojects {}` configuration:** Cross-project configuration is fragile and not obvious when inspecting a subproject's build script. Convention plugins replace this pattern.
- **Version catalog overrides in consuming projects:** The user decision is "strict -- no overrides." All services must use exactly the versions from the catalog.
- **Runtime configuration in convention plugins:** Logging format, health endpoints, OTel wiring are Phase 7 territory. Phase 3 plugins are dependencies-only + code quality.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Plugin marker artifacts | Manual POM generation for plugin resolution | `java-gradle-plugin` + `maven-publish` | Gradle automatically generates plugin marker artifacts (plugin.id:plugin.id.gradle.plugin:version) that redirect to the real implementation artifact |
| Version catalog publishing | Manual TOML file upload to Maven | `version-catalog` plugin + `maven-publish` | The `version-catalog` plugin creates a `versionCatalog` component that generates the proper TOML artifact for Maven publication |
| Precompiled script plugin class names | Manual `Plugin<Project>` implementation classes | `kotlin-dsl` plugin | Automatically compiles `.gradle.kts` files into plugin classes with generated names (e.g., `servista.library.gradle.kts` -> `Servista_libraryPlugin`) |
| Plugin classpath injection for tests | Manual classpath wiring | `java-gradle-plugin` TestKit integration | Automatically injects plugin-under-test classpath into GradleRunner via `withPluginClasspath()` |
| Directory structure validation | Custom Gradle task from scratch | Simple `require()` checks in a registered task | Minimal code, clear error messages, no framework needed |

**Key insight:** Gradle 9.x has excellent built-in support for the exact workflow this phase needs (publish convention plugins to external Maven repo, publish version catalog, consume both in separate projects). The `java-gradle-plugin` + `maven-publish` combination handles marker artifacts automatically, which is the trickiest part of plugin publishing.

## Common Pitfalls

### Pitfall 1: Precompiled Script Plugin Class Name Mismatch
**What goes wrong:** The `implementationClass` in `gradlePlugin {}` block does not match the auto-generated class name, causing "plugin not found" errors at publish/consume time.
**Why it happens:** Precompiled script plugins generate class names by converting the filename: dots become underscores for package separator, dashes become camelCase. `servista.library.gradle.kts` -> `Servista_libraryPlugin`. The exact naming convention is not well-documented.
**How to avoid:** After creating each `.gradle.kts` file, run `./gradlew classes` and inspect `build/classes/kotlin/main/` to see the generated class name. Use that exact name in the `gradlePlugin {}` block. Alternatively, verify by running `./gradlew pluginDescriptors` which generates descriptor files under `build/pluginDescriptors/`.
**Warning signs:** "Plugin with id 'servista.xxx' not found" errors when consumers try to apply the plugin.

### Pitfall 2: Forgejo Maven Registry Immutable Versions
**What goes wrong:** Publishing fails with an error because a package with the same name and version already exists.
**Why it happens:** Forgejo Maven registry does not allow overwriting existing artifacts. Unlike Nexus or Artifactory, there is no "redeploy" option.
**How to avoid:** Use SNAPSHOT versions during development (`0.1.0-SNAPSHOT`) or increment the version on every publish. For production, use semantic versioning with strict version increments. Note: SNAPSHOT support in Forgejo needs to be verified -- if not supported, use development version increments (0.1.0, 0.1.1, 0.1.2...).
**Warning signs:** HTTP 409 Conflict or similar errors on `./gradlew publish`.

### Pitfall 3: Detekt 2.0.0-alpha.2 Stability Risk
**What goes wrong:** Detekt alpha may have bugs, missing rules, or breaking changes between alpha releases.
**Why it happens:** Only version supporting Kotlin 2.3.0. Stable 1.23.8 cannot handle Kotlin 2.3 metadata (produces false positives across many rules).
**How to avoid:** Pin to `2.0.0-alpha.2` exactly (not a range). Use `buildUponDefaultConfig = true` for conservative defaults. Keep detekt configuration minimal initially. Monitor detekt releases for stable 2.0.0 or newer alphas. The convention plugin makes it easy to update all consumers at once when a stable release arrives.
**Warning signs:** Unexpected analysis failures, rule name changes (alpha.2 renamed several rule categories: "documentation" -> "comments", "empty" -> "emptyblocks", "bugs" -> "potentialbugs").

### Pitfall 4: Version Catalog vs Plugin Dependency Coordinates Drift
**What goes wrong:** Convention plugin hard-codes version X of a dependency, but the published version catalog specifies version Y. Consumers get conflicting versions.
**Why it happens:** Two sources of truth for the same dependency version -- the `.gradle.kts` convention plugin source code and the `catalog/libs.versions.toml` file.
**How to avoid:** Establish a single-source-of-truth principle: the `catalog/libs.versions.toml` file contains all canonical versions. Convention plugins should ideally read from this file during build, or at minimum, a CI check verifies that convention plugin coordinates match catalog versions. A build task that parses both files and fails on mismatch would be a defensive measure.
**Warning signs:** Gradle dependency resolution warnings about version conflicts, or different versions appearing in the dependency tree depending on whether they came from the plugin or the consumer's catalog.

### Pitfall 5: Forgejo HTTP Header Authentication in Gradle
**What goes wrong:** Standard username/password credentials fail because Forgejo expects token-based HTTP header authentication.
**Why it happens:** Forgejo's Maven registry uses `Authorization: token <access_token>` header, not HTTP Basic Auth.
**How to avoid:** Use `HttpHeaderCredentials` with `HttpHeaderAuthentication` in the Maven repository configuration. The token should be stored in `gradle.properties` (user home `~/.gradle/gradle.properties`, NOT committed to the repo) or injected via environment variable.
**Warning signs:** HTTP 401 Unauthorized errors when publishing.

### Pitfall 6: Missing Kotlin Plugin Dependency in Convention Plugin Build
**What goes wrong:** Precompiled script plugin that applies `kotlin("jvm")` fails to compile because the Kotlin Gradle plugin is not on the plugin project's classpath.
**Why it happens:** To use `plugins { kotlin("jvm") }` inside a precompiled script plugin, you must add `org.jetbrains.kotlin:kotlin-gradle-plugin` as an `implementation` dependency in the convention plugin project's `build.gradle.kts`. The plugin ID is resolved from the classpath, not from a plugin portal.
**How to avoid:** Add all external plugins used by convention plugins as `implementation` dependencies in the root `build.gradle.kts`. Map each `id("some.plugin")` in a `.gradle.kts` script to its corresponding Gradle plugin dependency artifact.
**Warning signs:** "Plugin with id 'org.jetbrains.kotlin.jvm' not found" during convention plugin compilation.

## Code Examples

### Complete Convention Plugin Project build.gradle.kts

```kotlin
// gradle-platform/build.gradle.kts
// Source: Gradle official docs - publishing convention plugins + version catalogs
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    `version-catalog`
}

group = "eu.servista"
version = "0.1.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // External Gradle plugins that convention plugins apply
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.3.10")
    implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.2")
    implementation("com.ncorti.ktfmt.gradle:plugin:0.25.0")

    // TestKit + test dependencies
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.2")
    testImplementation("io.kotest:kotest-assertions-core:6.1.4")
}

// Published version catalog (the one consumers import)
catalog {
    versionCatalog {
        from(files("catalog/libs.versions.toml"))
    }
}

// Register all 9 convention plugins
gradlePlugin {
    plugins {
        create("library") {
            id = "servista.library"
            implementationClass = "Servista_libraryPlugin"
        }
        create("apiService") {
            id = "servista.api-service"
            implementationClass = "Servista_api_servicePlugin"
        }
        create("jooq") {
            id = "servista.jooq"
            implementationClass = "Servista_jooqPlugin"
        }
        create("kafkaProducer") {
            id = "servista.kafka-producer"
            implementationClass = "Servista_kafka_producerPlugin"
        }
        create("kafkaConsumer") {
            id = "servista.kafka-consumer"
            implementationClass = "Servista_kafka_consumerPlugin"
        }
        create("pipelineService") {
            id = "servista.pipeline-service"
            implementationClass = "Servista_pipeline_servicePlugin"
        }
        create("avro") {
            id = "servista.avro"
            implementationClass = "Servista_avroPlugin"
        }
        create("testing") {
            id = "servista.testing"
            implementationClass = "Servista_testingPlugin"
        }
        create("observability") {
            id = "servista.observability"
            implementationClass = "Servista_observabilityPlugin"
        }
    }

    testSourceSets(sourceSets["functionalTest"])
}

// Functional test source set for TestKit
val functionalTest by sourceSets.creating {
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].output
}

val functionalTestImplementation by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}

tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn("functionalTest")
}

// Publishing configuration
publishing {
    publications {
        // java-gradle-plugin automatically creates publications for each plugin
        // We add the version catalog publication manually:
        create<MavenPublication>("versionCatalog") {
            from(components["versionCatalog"])
            artifactId = "gradle-platform-catalog"
        }
    }

    repositories {
        maven {
            name = "Forgejo"
            url = uri("https://forgejo.servista.eu/api/packages/servista/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "token ${findProperty("forgejo.token") ?: System.getenv("FORGEJO_TOKEN") ?: ""}"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}
```

### Base Convention Plugin (servista.library)

```kotlin
// src/main/kotlin/servista.library.gradle.kts
plugins {
    kotlin("jvm")
    id("dev.detekt")
    id("com.ncorti.ktfmt.gradle")
}

kotlin {
    jvmToolchain(21)
}

// Standard directory structure enforcement
tasks.register("validateProjectStructure") {
    group = "verification"
    description = "Validates that required Servista directory structure exists"
    doLast {
        val required = listOf("src/main/kotlin", "src/main/resources", "src/test/kotlin")
        val missing = required.filter { !project.file(it).exists() }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Missing required directories: ${missing.joinToString(", ")}. " +
                "Servista projects must follow the standard directory layout."
            )
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("validateProjectStructure")
}

// detekt configuration
detekt {
    buildUponDefaultConfig = true
    parallel = true
}

// ktfmt configuration
ktfmt {
    kotlinLangStyle()
}

// Common Kotlin dependencies
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
}
```

### Domain Convention Plugin Example (servista.jooq)

```kotlin
// src/main/kotlin/servista.jooq.gradle.kts
plugins {
    id("servista.library")
}

dependencies {
    implementation("org.jooq:jooq:3.20.11")
    implementation("org.jooq:jooq-kotlin:3.20.11")
    implementation("org.jooq:jooq-kotlin-coroutines:3.20.11")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-core:12.0.3")
    implementation("org.flywaydb:flyway-database-postgresql:12.0.3")
    implementation("org.postgresql:postgresql:42.7.10")
}
```

### Published Version Catalog

```toml
# catalog/libs.versions.toml
# Source: ADR-017 version compatibility matrix
[versions]
kotlin = "2.3.10"
ktor = "3.4.0"
koin = "4.1.1"
jooq = "3.20.11"
flyway = "12.0.3"
hikari = "7.0.2"
postgresql-jdbc = "42.7.10"
kafka = "4.1.1"
avro = "1.12.1"
apicurio-serdes = "3.0.6"
openfga = "0.9.5"
immudb4j = "1.0.1"
jedis = "5.2.0"
nimbus-jose-jwt = "10.8"
otel-agent = "2.20.0"
micrometer = "1.16.3"
mockk = "1.14.7"
testcontainers = "2.0.3"
junit5 = "5.14.2"
kotest = "6.1.4"
kotlinx-serialization = "1.10.0"
kotlinx-coroutines = "1.10.2"
kotlinx-datetime = "0.7.1"
kotlin-logging = "7.0.3"
logback = "1.5.32"
detekt = "2.0.0-alpha.2"

[libraries]
# Ktor Server
ktor-server-core = { module = "io.ktor:ktor-server-core", version.ref = "ktor" }
ktor-server-netty = { module = "io.ktor:ktor-server-netty", version.ref = "ktor" }
ktor-server-content-negotiation = { module = "io.ktor:ktor-server-content-negotiation", version.ref = "ktor" }
ktor-server-status-pages = { module = "io.ktor:ktor-server-status-pages", version.ref = "ktor" }
ktor-server-auth = { module = "io.ktor:ktor-server-auth", version.ref = "ktor" }
ktor-server-auth-jwt = { module = "io.ktor:ktor-server-auth-jwt", version.ref = "ktor" }
ktor-server-openapi = { module = "io.ktor:ktor-server-openapi", version.ref = "ktor" }
ktor-server-swagger = { module = "io.ktor:ktor-server-swagger", version.ref = "ktor" }
ktor-server-call-logging = { module = "io.ktor:ktor-server-call-logging", version.ref = "ktor" }
ktor-server-metrics-micrometer = { module = "io.ktor:ktor-server-metrics-micrometer", version.ref = "ktor" }
ktor-server-test-host = { module = "io.ktor:ktor-server-test-host", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

# Ktor Client
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }

# DI
koin-ktor = { module = "io.insert-koin:koin-ktor", version.ref = "koin" }
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }

# Database
jooq-core = { module = "org.jooq:jooq", version.ref = "jooq" }
jooq-kotlin = { module = "org.jooq:jooq-kotlin", version.ref = "jooq" }
jooq-kotlin-coroutines = { module = "org.jooq:jooq-kotlin-coroutines", version.ref = "jooq" }
jooq-postgres-extensions = { module = "org.jooq:jooq-postgres-extensions", version.ref = "jooq" }
jooq-codegen = { module = "org.jooq:jooq-codegen", version.ref = "jooq" }
flyway-core = { module = "org.flywaydb:flyway-core", version.ref = "flyway" }
flyway-postgresql = { module = "org.flywaydb:flyway-database-postgresql", version.ref = "flyway" }
hikari = { module = "com.zaxxer:HikariCP", version.ref = "hikari" }
postgresql-jdbc = { module = "org.postgresql:postgresql", version.ref = "postgresql-jdbc" }

# Kafka
kafka-clients = { module = "org.apache.kafka:kafka-clients", version.ref = "kafka" }
kafka-streams = { module = "org.apache.kafka:kafka-streams", version.ref = "kafka" }
kafka-streams-test = { module = "org.apache.kafka:kafka-streams-test-utils", version.ref = "kafka" }
avro = { module = "org.apache.avro:avro", version.ref = "avro" }
apicurio-serdes-avro = { module = "io.apicurio:apicurio-registry-serdes-avro-serde", version.ref = "apicurio-serdes" }

# Infrastructure clients
openfga-sdk = { module = "dev.openfga:openfga-sdk", version.ref = "openfga" }
immudb4j = { module = "io.codenotary:immudb4j", version.ref = "immudb4j" }
jedis = { module = "redis.clients:jedis", version.ref = "jedis" }
nimbus-jose-jwt = { module = "com.nimbusds:nimbus-jose-jwt", version.ref = "nimbus-jose-jwt" }

# Observability
otel-agent = { module = "io.opentelemetry.javaagent:opentelemetry-javaagent", version.ref = "otel-agent" }
micrometer-prometheus = { module = "io.micrometer:micrometer-registry-prometheus", version.ref = "micrometer" }

# Kotlin
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlin-logging = { module = "io.github.oshai:kotlin-logging-jvm", version.ref = "kotlin-logging" }
logback = { module = "ch.qos.logback:logback-classic", version.ref = "logback" }

# Testing
junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit5" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
testcontainers-core = { module = "org.testcontainers:testcontainers", version.ref = "testcontainers" }
testcontainers-postgresql = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" }
testcontainers-kafka = { module = "org.testcontainers:kafka", version.ref = "testcontainers" }
kotest-assertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
ktor-server-test-host-alias = { module = "io.ktor:ktor-server-test-host", version.ref = "ktor" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ktor = { id = "io.ktor.plugin", version.ref = "ktor" }
jooq-codegen = { id = "org.jooq.jooq-codegen-gradle", version.ref = "jooq" }
flyway = { id = "org.flywaydb.flyway", version.ref = "flyway" }
detekt = { id = "dev.detekt", version.ref = "detekt" }
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `buildSrc/` for shared build logic | Composite builds (`build-logic/`) or standalone repos | Gradle 7+ (2021), reinforced in Gradle 9 | `buildSrc` changes invalidate entire build cache; composite builds do not |
| `subprojects {}` / `allprojects {}` | Convention plugins | Gradle best practices 2022+ | Cross-project configuration is fragile; convention plugins are explicit |
| detekt 1.x with `io.gitlab.arturbosch.detekt` | detekt 2.x with `dev.detekt` | detekt 2.0.0-alpha.0 (Sep 2025) | New plugin ID, new rule category names, Analysis API instead of compiler plugin |
| Manual Maven POM for plugin resolution | `java-gradle-plugin` marker artifacts | Gradle 6+ | Automatic marker artifact generation enables `plugins { id("x") }` syntax |
| Version ranges in dependency declarations | Version catalogs + strict resolution | Gradle 7+ | Centralized, typed dependency access; enforced consistency |
| Gradle Wrapper 8.x | Gradle Wrapper 9.3.1 | Gradle 9.0 (2025) | Required for Kotlin 2.3 support; improved configuration cache |

**Deprecated/outdated:**
- `buildSrc/` for convention plugins: Invalidates entire build cache on any change. Use composite build or standalone repo.
- `io.gitlab.arturbosch.detekt` plugin ID: Being replaced by `dev.detekt` in detekt 2.x. The old ID still works for 1.23.x but not for 2.x.
- Gradle `version` keyword in `plugins {}` for composite builds: In composite builds, the version is resolved from the included build, not declared in the consumer. For external Maven-published plugins (our case), version IS required in the consumer's `plugins {}` block.

## Open Questions

1. **Precompiled script plugin generated class names**
   - What we know: Gradle generates class names from `.gradle.kts` filenames, with dots and dashes converted. The exact naming convention for `servista.api-service.gradle.kts` needs verification (is it `Servista_api_servicePlugin` or `Servista_apiServicePlugin`?).
   - What's unclear: The exact transformation rules for dashes in precompiled script plugin filenames.
   - Recommendation: Implement the first plugin, run `./gradlew classes`, and inspect the generated class names in `build/classes/kotlin/main/`. Update `gradlePlugin {}` block accordingly. Alternatively, use class-based plugins if name generation proves unreliable.

2. **Forgejo Maven registry SNAPSHOT support**
   - What we know: Forgejo does not allow publishing a package with the same name and version if it already exists.
   - What's unclear: Whether Forgejo supports Maven SNAPSHOT conventions (where `-SNAPSHOT` versions are overwritable with timestamped artifacts).
   - Recommendation: Test SNAPSHOT publishing early. If not supported, use development version increments (0.1.0, 0.1.1, 0.1.2...) and consider a `publishLocal` task for rapid iteration during development.

3. **Detekt 2.0.0-alpha.2 stability for CI**
   - What we know: It is the only version that supports Kotlin 2.3.0 metadata analysis. Built against Kotlin 2.3.0 and Gradle 9.3.0.
   - What's unclear: How stable it is for daily CI use. Alpha releases may have rule regressions or false positives.
   - Recommendation: Start with `buildUponDefaultConfig = true` and a permissive configuration. If specific rules cause issues, suppress them in `detekt.yml` until stable release. This is acceptable because convention plugins make it trivial to update all consumers when a stable detekt 2.0 ships.

4. **ktfmt Gradle plugin dependency artifact coordinates**
   - What we know: The plugin ID is `com.ncorti.ktfmt.gradle`, version 0.25.0.
   - What's unclear: The exact Maven coordinates for the Gradle plugin dependency (needed in the convention plugin project's `build.gradle.kts`). It may be `com.ncorti.ktfmt.gradle:com.ncorti.ktfmt.gradle.gradle.plugin:0.25.0` or `com.ncorti.ktfmt.gradle:plugin:0.25.0`.
   - Recommendation: Check the Gradle Plugin Portal or Maven Central for the exact artifact coordinates. The `gradlePluginPortal()` repository in the convention plugin project's repositories block should resolve it.

5. **Convention plugin dependency version synchronization with catalog**
   - What we know: Convention plugins will hard-code dependency coordinates, and the version catalog will have the same versions.
   - What's unclear: Best mechanism to ensure they stay synchronized.
   - Recommendation: Create a CI check (or Gradle task) that parses both the convention plugin source files and `catalog/libs.versions.toml` to verify version consistency. Alternatively, consider reading the catalog TOML at build time to generate constants, but this adds complexity.

## Sources

### Primary (HIGH confidence)
- [Gradle Convention Plugins documentation](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html) - convention plugin structure, precompiled scripts
- [Gradle Precompiled Script Plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_precompiled.html) - plugin ID derivation from filenames
- [Gradle Publishing Convention Plugins (multi-repo)](https://docs.gradle.org/current/samples/sample_publishing_convention_plugins.html) - end-to-end publish/consume sample
- [Gradle Version Catalogs](https://docs.gradle.org/current/userguide/version_catalogs.html) - version-catalog plugin, from() method, publishing
- [Gradle TestKit](https://docs.gradle.org/current/userguide/test_kit.html) - GradleRunner, withPluginClasspath, functional test setup
- [Gradle Java Plugin Development Plugin](https://docs.gradle.org/current/userguide/java_gradle_plugin.html) - gradlePlugin block, marker artifacts, maven-publish integration
- [Forgejo Maven Package Registry](https://forgejo.org/docs/next/user/packages/maven/) - URL pattern, authentication, publishing constraints
- [detekt Compatibility Table](https://detekt.dev/docs/introduction/compatibility/) - Kotlin/detekt version matrix
- [ADR-017 Backend Tech Stack](../../architecture/decisions/017-backend-tech-stack.md) - version compatibility matrix, all dependency versions

### Secondary (MEDIUM confidence)
- [Gradle Goodness: Publish Version Catalog](https://blog.mrhaki.com/2023/03/gradle-goodness-publish-version-catalog.html) - verified publish/consume pattern with code examples
- [detekt 2.0.0-alpha.2 release notes](https://github.com/detekt/detekt/releases/tag/v2.0.0-alpha.2) - Kotlin 2.3.0 support, rule renames
- [ktfmt Gradle plugin](https://github.com/cortinico/ktfmt-gradle) - version 0.25.0, plugin configuration
- [Gradle TestKit DEV article](https://dev.to/autonomousapps/gradle-all-the-way-down-testing-your-gradle-plugin-with-gradle-testkit-2hmc) - TestKit patterns with functional test fixtures
- [Convenient Detekt Conventions blog](https://jadarma.github.io/blog/posts/2025/04/convenient-detekt-conventions/) - detekt in convention plugin patterns
- [Gradle Best Practices for Kotlin](https://kotlinlang.org/docs/gradle-best-practices.html) - Kotlin team's Gradle recommendations

### Tertiary (LOW confidence)
- [detekt 1.23.8 Kotlin 2.3 issue](https://github.com/detekt/detekt/issues/8865) - community reports of false positives; confirmed by compatibility table
- Precompiled script plugin class name generation rules - based on observed patterns, not documented formally

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Gradle convention plugins, version catalog publishing, and TestKit are well-documented stable features in Gradle 9.x with extensive official documentation and samples
- Architecture: HIGH - The multi-repo + shared convention plugins pattern is an official Gradle sample ("Sharing build logic in a multi-repo setup") with clear documentation
- Pitfalls: HIGH - Identified from official docs (marker artifacts), Forgejo docs (immutable versions), and confirmed community reports (detekt/Kotlin 2.3 incompatibility)
- Code quality tooling: MEDIUM - detekt 2.0.0-alpha.2 is confirmed working with Kotlin 2.3 but is an alpha; ktfmt 0.25.0 is stable but exact convention plugin dependency coordinates need verification

**Research date:** 2026-03-03
**Valid until:** 2026-04-03 (30 days - stack is stable, detekt 2.0 may get new alpha/stable releases)
