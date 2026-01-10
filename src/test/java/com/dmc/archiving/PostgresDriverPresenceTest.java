package com.dmc.archiving;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PostgresDriverPresenceTest {

    @Test
    void postgresDriverIsOnClasspath() {
        assertDoesNotThrow(() -> Class.forName("org.postgresql.Driver"));
    }
}

