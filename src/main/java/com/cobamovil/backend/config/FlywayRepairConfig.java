package com.cobamovil.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Optionally runs Flyway.repair() before migrate to fix checksum mismatches
 * without dropping data. Enable with env var APP_FLYWAY_REPAIR_ON_MIGRATE=true
 */
@Configuration
@ConditionalOnProperty(name = "app.flyway.repairOnMigrate", havingValue = "true")
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return (Flyway flyway) -> {
            // First fix checksums and history inconsistencies
            flyway.repair();
            // Then run the normal migration
            flyway.migrate();
        };
    }
}
