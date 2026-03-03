plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    `version-catalog`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // External Gradle plugins that convention plugins apply via plugins {} blocks.
    // Each convention script that uses `kotlin("jvm")`, `id("dev.detekt")`, etc.
    // needs the corresponding plugin artifact on the classpath.
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.3.10")
    implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.2")
    implementation("com.ncorti.ktfmt.gradle:plugin:0.25.0")

    // TestKit + test dependencies
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.kotest:kotest-assertions-core:6.1.4")
}

// Published version catalog -- loaded from catalog/ directory.
// Consumers import via: from("eu.servista:gradle-platform-catalog:x.y.z")
catalog {
    versionCatalog {
        from(files("catalog/libs.versions.toml"))
    }
}

// Functional test source set for Gradle TestKit (used in Plan 03).
// Must be defined before gradlePlugin {} references it.
val functionalTest by sourceSets.creating {
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].output
}

val functionalTestImplementation by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}
val functionalTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations["testRuntimeOnly"])
}

// Register convention plugins. All 9 plugins registered here.
gradlePlugin {
    plugins {
        create("library") {
            id = "servista.library"
            implementationClass = "Servista_libraryPlugin"
        }
        create("api-service") {
            id = "servista.api-service"
            implementationClass = "Servista_apiServicePlugin"
        }
        create("jooq") {
            id = "servista.jooq"
            implementationClass = "Servista_jooqPlugin"
        }
        create("kafka-producer") {
            id = "servista.kafka-producer"
            implementationClass = "Servista_kafkaProducerPlugin"
        }
        create("kafka-consumer") {
            id = "servista.kafka-consumer"
            implementationClass = "Servista_kafkaConsumerPlugin"
        }
        create("pipeline-service") {
            id = "servista.pipeline-service"
            implementationClass = "Servista_pipelineServicePlugin"
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

tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn("functionalTest")
}

// Publishing configuration -- version catalog + Forgejo Maven registry
publishing {
    publications {
        // java-gradle-plugin auto-publishes convention plugin artifacts.
        // The version catalog needs a manual publication:
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
