---
phase: 01-shared-build-infrastructure
plan: 01
subsystem: infra
tags: [gradle, convention-plugins, version-catalog, detekt, ktfmt, kotlin-dsl, forgejo-maven]

# Dependency graph
requires:
  - phase: 02-backend-tech-stack-decision
    provides: ADR-017 version compatibility matrix with all dependency versions
provides:
  - gradle-platform project skeleton with Gradle 9.3.1 wrapper
  - servista.library base convention plugin (Kotlin 2.3/JVM 21, detekt, ktfmt, directory validation)
  - Published version catalog with 80 entries covering all ADR-017 dependencies
  - Forgejo Maven registry publishing configuration with HttpHeaderCredentials
  - Functional test source set wired for Gradle TestKit
affects: [03-shared-build-infrastructure, 04-commons-sdk, 07-service-scaffold-template]

# Tech tracking
tech-stack:
  added: [gradle-9.3.1, kotlin-dsl, java-gradle-plugin, maven-publish, version-catalog, detekt-2.0.0-alpha.2, ktfmt-0.25.0]
  patterns: [precompiled-script-plugins, composable-convention-plugins, published-version-catalog, forgejo-maven-publishing]

key-files:
  created:
    - infrastructure/gradle-platform/build.gradle.kts
    - infrastructure/gradle-platform/settings.gradle.kts
    - infrastructure/gradle-platform/gradle.properties
    - infrastructure/gradle-platform/gradle/libs.versions.toml
    - infrastructure/gradle-platform/catalog/libs.versions.toml
    - infrastructure/gradle-platform/src/main/kotlin/servista.library.gradle.kts
    - infrastructure/gradle-platform/src/main/resources/detekt/detekt.yml
  modified: []

key-decisions:
  - "Functional test source set must be defined before gradlePlugin block references it (Gradle ordering requirement)"
  - "detekt config loaded from classpath via Thread.currentThread().contextClassLoader with null-safe fallback"
  - "Only servista.library registered in gradlePlugin block for Plan 01; remaining 8 plugins added in Plan 02"

patterns-established:
  - "Precompiled script plugins: filename servista.X.gradle.kts generates Servista_XPlugin class"
  - "Version catalog separation: gradle/libs.versions.toml (internal build) vs catalog/libs.versions.toml (published to consumers)"
  - "Convention plugins use hard-coded dependency coordinates matching the published catalog versions"

requirements-completed: [FOUND-02]

# Metrics
duration: 7min
completed: 2026-03-03
---

# Phase 3 Plan 01: Gradle Platform Skeleton Summary

**Compilable gradle-platform project with Gradle 9.3.1, servista.library convention plugin (Kotlin 2.3/JVM 21 + detekt + ktfmt + directory validation), published version catalog (80 ADR-017 entries), and Forgejo Maven publishing**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-03T12:01:19Z
- **Completed:** 2026-03-03T12:08:33Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- gradle-platform project compiles successfully with Gradle 9.3.1 (`./gradlew classes` passes)
- servista.library convention plugin with Kotlin 2.3/JVM 21 target, detekt 2.0.0-alpha.2, ktfmt 0.25.0, and directory structure validation
- Published version catalog (catalog/libs.versions.toml) with 80 entries covering all ADR-017 dependencies
- Forgejo Maven registry publishing configured with HttpHeaderCredentials authentication
- Functional test source set wired and ready for Plan 03 TestKit tests

## Task Commits

Each task was committed atomically:

1. **Task 1: Create gradle-platform project skeleton with publishing configuration and version catalog** - `d42b0bc` (feat)
2. **Task 2: Create servista.library base convention plugin with code quality and directory validation** - `2d3a6a0` (feat)

## Files Created/Modified
- `infrastructure/gradle-platform/build.gradle.kts` - Root build with kotlin-dsl, java-gradle-plugin, maven-publish, version-catalog, publishing config
- `infrastructure/gradle-platform/settings.gradle.kts` - Root project name
- `infrastructure/gradle-platform/gradle.properties` - Group (eu.servista), version (0.1.0), token reference
- `infrastructure/gradle-platform/gradle/libs.versions.toml` - Internal catalog for building plugins
- `infrastructure/gradle-platform/catalog/libs.versions.toml` - Published version catalog with all ADR-017 dependencies
- `infrastructure/gradle-platform/src/main/kotlin/servista.library.gradle.kts` - Base convention plugin
- `infrastructure/gradle-platform/src/main/resources/detekt/detekt.yml` - Shared detekt configuration

## Decisions Made
- Functional test source set defined before gradlePlugin block to satisfy Gradle ordering requirement (source set must exist before reference)
- detekt config loaded from classpath resources with null-safe check; falls back gracefully if resource unavailable
- Only servista.library registered in this plan; remaining 8 plugins deferred to Plan 02 per plan scope
- JDK 21 (Temurin) installed via SDKMAN for compilation -- system only had JRE headless

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Installed JDK 21 via SDKMAN**
- **Found during:** Task 2 (convention plugin compilation)
- **Issue:** System had only java-21-openjdk-headless (JRE), no javac -- Gradle could not compile
- **Fix:** Installed Temurin JDK 21.0.10 via SDKMAN (sdk install java 21.0.10-tem)
- **Files modified:** None (system-level change)
- **Verification:** ./gradlew classes completes successfully
- **Committed in:** N/A (environment setup, not code change)

**2. [Rule 1 - Bug] Fixed source set ordering in build.gradle.kts**
- **Found during:** Task 1 (./gradlew help verification)
- **Issue:** gradlePlugin block referenced sourceSets["functionalTest"] before it was created, causing "SourceSet not found" error
- **Fix:** Moved functionalTest source set creation above gradlePlugin block
- **Files modified:** infrastructure/gradle-platform/build.gradle.kts
- **Verification:** ./gradlew help succeeds
- **Committed in:** d42b0bc (Task 1 commit, fixed before committing)

---

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking)
**Impact on plan:** Both fixes necessary for correct compilation. No scope creep.

## Issues Encountered
None beyond the auto-fixed deviations above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- gradle-platform project skeleton ready for Plan 02 (remaining 8 convention plugins)
- Functional test source set wired for Plan 03 (TestKit verification tests)
- Published version catalog ready for consumers once published to Forgejo Maven registry

## Self-Check: PASSED

All 7 created files verified on disk. Both task commits (d42b0bc, 2d3a6a0) found in git log.

---
*Phase: 03-shared-build-infrastructure*
*Completed: 2026-03-03*
