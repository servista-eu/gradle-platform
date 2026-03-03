---
phase: 01-shared-build-infrastructure
plan: 03
subsystem: infra
tags: [gradle, testkit, functional-tests, convention-plugins, adr-amendment, multi-repo]

# Dependency graph
requires:
  - phase: 03-shared-build-infrastructure
    provides: 9 convention plugins compiling on servista.library base
provides:
  - Gradle TestKit functional tests verifying all major plugin compositions
  - LibraryPluginTest verifying Kotlin/JVM 21 config and directory validation
  - ApiServiceCompositionTest proving Phase 3 success criteria #3 (api-service + jooq + kafka-producer)
  - EventSinkCompositionTest verifying kafka-consumer + avro + testing + observability
  - PipelineCompositionTest verifying pipeline-service + avro + testing + observability
  - ADR-017 amended to reflect multi-repo + gradle-platform decision
affects: [04-commons-sdk, 07-service-scaffold-template]

# Tech tracking
tech-stack:
  added: [gradle-testkit]
  patterns: [testkit-functional-testing, plugin-composition-verification]

key-files:
  created:
    - infrastructure/gradle-platform/src/functionalTest/kotlin/eu/servista/gradle/LibraryPluginTest.kt
    - infrastructure/gradle-platform/src/functionalTest/kotlin/eu/servista/gradle/ApiServiceCompositionTest.kt
    - infrastructure/gradle-platform/src/functionalTest/kotlin/eu/servista/gradle/EventSinkCompositionTest.kt
    - infrastructure/gradle-platform/src/functionalTest/kotlin/eu/servista/gradle/PipelineCompositionTest.kt
  modified:
    - infrastructure/gradle-platform/build.gradle.kts
    - infrastructure/gradle-platform/catalog/libs.versions.toml
    - infrastructure/gradle-platform/src/main/kotlin/servista.avro.gradle.kts
    - architecture/decisions/017-backend-tech-stack.md

key-decisions:
  - "TestKit test projects need repositories { mavenCentral() } since convention plugins add dependencies but not repository configuration"
  - "Apicurio SerDes version fixed from 3.0.6 (nonexistent on Maven Central) to 3.0.0.M4 (latest available 3.x)"
  - "JUnit Platform launcher must be explicitly added as testRuntimeOnly for Gradle 9.x functional test execution"

patterns-established:
  - "TestKit pattern: TempDir project with settings.gradle.kts, build.gradle.kts (plugins + repos), directory structure, minimal source, GradleRunner with withPluginClasspath()"
  - "Negative testing: buildAndFail() for verifying convention plugin enforcement errors"

requirements-completed: [FOUND-02]

# Metrics
duration: 9min
completed: 2026-03-03
---

# Phase 3 Plan 03: TestKit Verification and ADR Amendment Summary

**5 Gradle TestKit functional tests proving all convention plugin compositions compile end-to-end, plus ADR-017 amended from monorepo to multi-repo + gradle-platform**

## Performance

- **Duration:** 9 min
- **Started:** 2026-03-03T12:19:45Z
- **Completed:** 2026-03-03T12:28:47Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments
- All 5 TestKit tests pass: 2 library tests (success + directory validation failure), 1 API service composition, 1 event sink composition, 1 pipeline composition
- API service composition test (api-service + jooq + kafka-producer) proves Phase 3 success criteria #3: "a hello-world service builds successfully"
- `./gradlew check` passes end-to-end: compiles all 9 plugins AND runs all TestKit tests
- ADR-017 fully amended: monorepo replaced with multi-repo + gradle-platform, plugin inventory expanded from 3 to 9

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Gradle TestKit functional tests for plugin compositions** - `07534d9` (test)
2. **Task 2: Amend ADR-017 to reflect multi-repo with gradle-platform** - `c7b0ab4` (docs)

## Files Created/Modified
- `infrastructure/gradle-platform/src/functionalTest/kotlin/eu/servista/gradle/LibraryPluginTest.kt` - Base plugin tests: compilation success with directory structure, failure without
- `infrastructure/gradle-platform/src/functionalTest/kotlin/eu/servista/gradle/ApiServiceCompositionTest.kt` - Critical Phase 3 test: api-service + jooq + kafka-producer compiles
- `infrastructure/gradle-platform/src/functionalTest/kotlin/eu/servista/gradle/EventSinkCompositionTest.kt` - Event sink composition: kafka-consumer + avro + testing + observability
- `infrastructure/gradle-platform/src/functionalTest/kotlin/eu/servista/gradle/PipelineCompositionTest.kt` - Pipeline composition: pipeline-service + avro + testing + observability
- `infrastructure/gradle-platform/build.gradle.kts` - Added functionalTestRuntimeOnly config extension and junit-platform-launcher dependency
- `infrastructure/gradle-platform/catalog/libs.versions.toml` - Fixed apicurio-serdes version from 3.0.6 to 3.0.0.M4
- `infrastructure/gradle-platform/src/main/kotlin/servista.avro.gradle.kts` - Fixed apicurio-serdes version from 3.0.6 to 3.0.0.M4
- `architecture/decisions/017-backend-tech-stack.md` - Amended: multi-repo structure, 9 plugins, Forgejo Maven registry

## Decisions Made
- TestKit test projects require explicit `repositories { mavenCentral() }` blocks -- convention plugins define dependencies but not repository configuration (responsibility of the consuming project)
- Apicurio Registry SerDes version corrected from 3.0.6 (does not exist on Maven Central) to 3.0.0.M4 (latest available 3.x release)
- JUnit Platform launcher must be added explicitly as `testRuntimeOnly` dependency for Gradle 9.x to discover and execute JUnit 5 tests in custom source sets

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added junit-platform-launcher for functional test execution**
- **Found during:** Task 1 (TestKit test creation)
- **Issue:** Gradle 9.x functional test task failed with "Failed to load JUnit Platform" -- junit-platform-launcher not on runtime classpath
- **Fix:** Added `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` to build.gradle.kts and `functionalTestRuntimeOnly` configuration extending `testRuntimeOnly`
- **Files modified:** infrastructure/gradle-platform/build.gradle.kts
- **Verification:** `./gradlew functionalTest` loads JUnit Platform and discovers all 5 tests
- **Committed in:** 07534d9 (Task 1 commit)

**2. [Rule 1 - Bug] Fixed Apicurio SerDes version from nonexistent 3.0.6 to 3.0.0.M4**
- **Found during:** Task 1 (TestKit test creation)
- **Issue:** `io.apicurio:apicurio-registry-serdes-avro-serde:3.0.6` does not exist on Maven Central; EventSink and Pipeline composition tests failed to resolve dependencies
- **Fix:** Changed version to 3.0.0.M4 (latest available 3.x) in both the avro convention plugin and version catalog
- **Files modified:** infrastructure/gradle-platform/src/main/kotlin/servista.avro.gradle.kts, infrastructure/gradle-platform/catalog/libs.versions.toml
- **Verification:** All 5 TestKit tests pass, `./gradlew check` succeeds
- **Committed in:** 07534d9 (Task 1 commit)

**3. [Rule 3 - Blocking] Added mavenCentral() repository to TestKit test projects**
- **Found during:** Task 1 (TestKit test creation)
- **Issue:** TestKit test projects had no repositories defined; convention plugins add dependencies but not repository configuration, so `compileKotlin` failed with "no repositories are defined"
- **Fix:** Added `repositories { mavenCentral() }` to each test project's generated build.gradle.kts
- **Files modified:** All 4 TestKit test files
- **Verification:** All compilation tasks succeed with resolved dependencies
- **Committed in:** 07534d9 (Task 1 commit)

---

**Total deviations:** 3 auto-fixed (1 bug, 2 blocking)
**Impact on plan:** All auto-fixes necessary for correctness and test execution. No scope creep.

## Issues Encountered
- System JRE at `/usr/lib/jvm/java-21-openjdk` lacks `javac` (JRE-only, no JDK) -- resolved by using SDKMAN-installed JDK at `/home/sven/.sdkman/candidates/java/21.0.10-tem` via JAVA_HOME. This is an environment issue, not a project issue; CI environments will have a full JDK.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 3 complete: all 9 convention plugins verified by TestKit, ADR-017 amended
- gradle-platform ready for publishing to Forgejo Maven registry (Phase 3 success criteria met)
- Phase 4 (Commons SDK) can proceed: shared library will consume convention plugins
- Phase 7 (Service Scaffold Template) can proceed: all plugin compositions proven

## Self-Check: PASSED

---
*Phase: 03-shared-build-infrastructure*
*Completed: 2026-03-03*
