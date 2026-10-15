package com.alexabeauty.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Standard Flyway migration strategy enforcing forward-only immutable migrations and schema validation.
 * Emergency repair is strictly gated behind the 'app.flyway.repair' flag.
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Value("${app.flyway.repair:false}")
    private boolean repairOnStartup;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            if (repairOnStartup) {
                log.warn("Emergency Flyway repair requested via app.flyway.repair=true. Repairing schema history...");
                flyway.repair();
            }
            log.info("Executing Flyway migrate with validation...");
            flyway.migrate();
        };
    }
}
