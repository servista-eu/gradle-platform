package eu.servista.gradle

import io.kotest.matchers.shouldBe
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Gradle TestKit functional test for pipeline service plugin composition.
 *
 * Verifies the typical composition for pipeline services like silver-streaming:
 * pipeline-service + avro + testing + observability.
 *
 * Note: servista.library is NOT applied explicitly -- it is inherited from the domain plugins.
 */
class PipelineCompositionTest {

    @TempDir lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        // settings.gradle.kts
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test-pipeline"
            """.trimIndent()
        )

        // build.gradle.kts applying the pipeline composition
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("servista.pipeline-service")
                id("servista.avro")
                id("servista.testing")
                id("servista.observability")
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
                println("Hello from pipeline composition")
            }
            """.trimIndent()
        )
    }

    @Test
    fun `pipeline-service plus avro plus testing plus observability composition compiles successfully`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("compileKotlin", "--stacktrace")
            .forwardOutput()
            .build()

        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
