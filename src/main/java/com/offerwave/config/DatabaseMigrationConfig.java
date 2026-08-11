package com.offerwave.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Flyway adoption settings for both empty and previously hand-created databases.
 */
@Configuration
public class DatabaseMigrationConfig {

    static final String LEGACY_SCHEMA_VERSION = "20260309";

    /**
     * Spring Boot 3.1's Flyway factory calls an API removed in Flyway 10. A
     * project-owned bean keeps Boot's migration initializer while allowing the
     * MySQL 8.4-capable Flyway database module to be used.
     */
    @Bean(initMethod = "migrate")
    public Flyway offerWaveFlyway(DataSource dataSource,
                                  FlywayConfigurationCustomizer offerWaveFlywayCustomizer) {
        FluentConfiguration configuration = Flyway.configure().dataSource(dataSource);
        offerWaveFlywayCustomizer.customize(configuration);
        return configuration.load();
    }

    @Bean
    public FlywayConfigurationCustomizer offerWaveFlywayCustomizer(Environment environment) {
        boolean adoptLegacySchema = environment.getProperty(
                "offerwave.database.baseline-existing-schema", Boolean.class, false);
        return configuration -> {
            configuration
                    .baselineVersion(MigrationVersion.fromVersion(LEGACY_SCHEMA_VERSION))
                    .baselineDescription("OfferWave legacy schema through 2026-03-09");
            if (adoptLegacySchema) {
                configuration.baselineOnMigrate(true);
            }
        };
    }
}
