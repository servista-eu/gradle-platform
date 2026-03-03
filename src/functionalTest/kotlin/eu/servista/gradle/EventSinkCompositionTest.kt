package eu.servista.gradle

import io.kotest.matchers.shouldBe
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Gradle TestKit functional test for event sink plugin composition.
 *
 * Verifies the typical composition for event sink services like svc-audit-writer:
 * kafka-consumer + avro + testing + observability.
 *
 * Note: servista.library is NOT applied explicitly -- it is inherited from the domain plugins.
 */
class EventSinkCompositionTest {

    @TempDir lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        // settings.gradle.kts
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test-event-sink"
            """.trimIndent()
        )

        // build.gradle.kts applying the event sink composition
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("servista.kafka-consumer")
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
                println("Hello from event sink composition")
            }
            """.trimIndent()
        )
    }

    @Test
    fun `kafka-consumer plus avro plus testing plus observability composition compiles successfully`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("compileKotlin", "--stacktrace")
            .forwardOutput()
            .build()

        result.task(":compileKotlin")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
