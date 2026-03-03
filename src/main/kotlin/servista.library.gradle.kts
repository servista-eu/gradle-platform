// servista.library -- Base convention plugin for all Servista projects.
// Provides: Kotlin 2.3/JVM 21, detekt, ktfmt, directory structure validation.
// All other convention plugins apply this as their foundation.

plugins {
    kotlin("jvm")
    id("dev.detekt")
    id("com.ncorti.ktfmt.gradle")
}

kotlin {
    jvmToolchain(21)
}

// Standard directory structure enforcement.
// Per user decision: "Build fails if structure is wrong."
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

// detekt configuration -- conservative defaults, fail on any issue.
detekt {
    buildUponDefaultConfig = true
    parallel = true
    // Load shared detekt.yml from classpath resources (packaged in this plugin jar).
    val detektConfig = Thread.currentThread().contextClassLoader
        .getResource("detekt/detekt.yml")
    if (detektConfig != null) {
        config.from(resources.text.fromUri(detektConfig.toURI()))
    }
}

// ktfmt -- Kotlin official style formatting, zero configuration.
ktfmt {
    kotlinLangStyle()
}

// Common Kotlin dependencies that every project needs.
// Versions match ADR-017 and the published version catalog.
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
}
