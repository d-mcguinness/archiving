package com.dmc.archiving;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Dev-only: wipe and rebuild the schema from Flyway migrations on every start —
 * the migration-era equivalent of the old {@code ddl-auto=create-drop} "dev
 * reset", and a self-heal for a dev DB that drifted from the model under the
 * former {@code ddl-auto=update}. Requires {@code spring.flyway.clean-disabled=false}
 * (set in application-dev.properties). Never active in prod (the bean is
 * {@code @Profile("dev")} and prod keeps clean disabled).
 */
@Configuration
@Profile("dev")
public class DevDatabaseConfig {

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
