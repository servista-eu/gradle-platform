package eu.servista.gradle

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Gradle TestKit functional tests for the servista.library convention plugin.
 *
 * Verifies:
 * - Kotlin/JVM 21 configuration and compilation with required directory structure
 * - Build failure when required directory structure is missing
 */
class LibraryPluginTest {

    @TempDir lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        // settings.gradle.kts
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test-library"
            """.trimIndent()
        )

        // build.gradle.kts applying servista.library
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("servista.library")
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
                println("Hello from servista.library")
            }
            """.trimIndent()
        )
    }

    @Test
    fun `applying servista library with required directory structure succeeds`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("compileKotlin", "--stacktrace")
            .forwardOutput()
            .build()

        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `applying servista library without required directory structure fails`() {
        // Remove required directory to trigger validation failure
        projectDir.resolve("src/main/resources").deleteRecursively()

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("compileKotlin", "--stacktrace")
            .forwardOutput()
            .buildAndFail()

        result.output shouldContain "Missing required directories"
    }
}
