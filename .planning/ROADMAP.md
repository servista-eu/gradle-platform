# Roadmap: Gradle Platform

## Overview

This roadmap delivers the Servista shared build infrastructure. The gradle-platform repo provides composable Gradle convention plugins and a published version catalog consumed by all Kotlin/JVM service repos. Plans are governed by the servista/governance/mgmt-platform repo.

## Governance Mapping

| Gradle-Platform Phase | Governance Phase | Name |
|-----------------------|-----------------|------|
| 1 | 3 | Shared Build Infrastructure |

## Phases

### Phase 1: Shared Build Infrastructure
**Goal**: A `gradle-platform` repo exists on Forgejo with composable convention plugins and a published version catalog that all service repos consume for consistent builds
**Depends on**: Nothing (governance Phase 2 tech stack decision already made)
**Requirements**: FOUND-02
**Success Criteria** (what must be TRUE):
  1. `gradle-platform` project contains 9 composable convention plugins with code quality tooling (detekt, ktfmt)
  2. Version catalog contains all dependencies from the tech stack decision and is published to Forgejo Maven registry
  3. Gradle TestKit integration tests verify that a project applying `servista.api-service` + `servista.jooq` + `servista.kafka-producer` compiles successfully
  4. ADR-017 is amended to replace monorepo with multi-repo + gradle-platform approach
Plans:
- [x] 01-01-PLAN.md -- Project scaffold, version catalog, base library plugin, Forgejo publishing
- [x] 01-02-PLAN.md -- Remaining convention plugins (api-service, jooq, kafka, observability, avro, pipeline, testing)
- [x] 01-03-PLAN.md -- Gradle TestKit functional tests for plugin composition
