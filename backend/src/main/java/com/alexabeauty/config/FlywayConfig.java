package com.alexabeauty.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Flyway migration strategy that runs repair before migration.
 * This automatically heals historical checksum discrepancies and removes failed entries
 * across environment deploys without breaking the Spring Boot ApplicationContext.
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Executing Flyway repair to align migration checksums with schema history...");
            flyway.repair();
            log.info("Executing Flyway migrate...");
            flyway.migrate();
        };
    }
}
