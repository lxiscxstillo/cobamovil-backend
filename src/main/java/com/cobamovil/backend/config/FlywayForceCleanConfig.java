package com.cobamovil.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Danger: Drops the entire schema and migrates from scratch.
 * Enable only when you are sure there is no important data.
 * Activate with env var app.flyway.forceClean=true
 */
@Configuration
@ConditionalOnProperty(name = "app.flyway.forceClean", havingValue = "true")
public class FlywayForceCleanConfig {

    @Bean
    public FlywayMigrationStrategy flywayForceCleanMigrationStrategy() {
        return (Flyway flyway) -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}

