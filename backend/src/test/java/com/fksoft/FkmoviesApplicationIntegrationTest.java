package com.fksoft;

import org.junit.jupiter.api.Test;

/**
 * Proves the application context boots against a real Postgres: Flyway runs the baseline
 * migration and Hibernate validates the (empty) schema (SPEC-0001).
 */
class FkmoviesApplicationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Booting the cached context with Flyway + ddl-auto=validate is the assertion itself.
    }
}
