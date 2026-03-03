package eu.servista.gradle

import io.kotest.matchers.shouldBe
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Gradle TestKit functional test for API service plugin composition.
 *
 * This is the critical test matching Phase 3 success criteria #3:
 * "A hello-world service builds successfully" -- proven by applying
 * api-service + jooq + kafka-producer and compiling a minimal Kotlin source.
 */
class ApiServiceCompositionTest {

    @TempDir lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        // settings.gradle.kts
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test-api-service"
            """.trimIndent()
        )

        // build.gradle.kts applying the API service composition
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("servista.api-service")
                id("servista.jooq")
                id("servista.kafka-producer")
            }

            repositories {
                mavenCentral()
            }
            """.trimIndent()
        )

        // Required directory structure
        projectDir.resolve("src/main/kotlin").mkdirs()
        projectDir.resolve("src/main/resources").mkdirs()
        projectDir.resolve("src/test/kotlin").mkdirs()

        // Minimal Kotlin source file
        projectDir.resolve("src/main/kotlin/App.kt").writeText(
            """
            package eu.servista.test

            fun main() {
                println("Hello from API service composition")
            }
            """.trimIndent()
        )
    }

    @Test
    fun `api-service plus jooq plus kafka-producer composition compiles successfully`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("compileKotlin", "--stacktrace")
            .forwardOutput()
            .build()

        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
