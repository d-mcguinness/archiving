package com.dmc.archiving;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Flyway baseline is correct against a REAL Postgres — the verification
 * our H2 unit tests (create-drop, Flyway disabled) cannot give. The Spring context
 * only loads if Flyway applies V1 AND Hibernate's {@code ddl-auto=validate}
 * confirms the migrated schema matches the JPA entities; a baseline that drifts
 * from the entities fails this test rather than only failing in production.
 *
 * <p>Skipped automatically when Docker is unavailable, so a Docker-less dev build
 * stays green; CI (which has Docker) runs it on every PR.
 */
// Mirrors the real Postgres runtime config (Flyway on, ddl-auto=validate, data.sql
// seeding on) — NOT spring.sql.init=never — so it also catches the
// defer-datasource-initialization <-> Flyway circular-dependency boot failure and
// proves data.sql runs cleanly after Flyway. defer-datasource-initialization is
// deliberately NOT set (it clashes with Flyway).
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.sql.init.mode=always"
})
class FlywaySchemaValidationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayBaselineAppliesValidatePassesAndDataSeeds() {
        // Reaching this point means the context booted: Flyway ran V1, Hibernate
        // validate accepted the schema, and data.sql seeded — no circular dependency.
        Integer appliedMigrations = jdbc.queryForObject(
            "select count(*) from flyway_schema_history where success = true", Integer.class);
        assertThat(appliedMigrations).isGreaterThanOrEqualTo(1);

        Integer tables = jdbc.queryForObject(
            "select count(*) from information_schema.tables "
            + "where table_schema = 'public' and table_name <> 'flyway_schema_history'", Integer.class);
        assertThat(tables).isEqualTo(22); // 16 entity + 5 join + stripe_meter_reports (V3)

        // data.sql ran after Flyway (it INSERTs the demo users).
        Integer users = jdbc.queryForObject("select count(*) from users", Integer.class);
        assertThat(users).isGreaterThan(0);
    }
}
