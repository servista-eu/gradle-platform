// servista.jooq -- Convention plugin for database access.
// Provides: jOOQ + HikariCP + Flyway + PostgreSQL JDBC driver.
// Composes on top of servista.library (Kotlin/JVM 21, detekt, ktfmt).

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
