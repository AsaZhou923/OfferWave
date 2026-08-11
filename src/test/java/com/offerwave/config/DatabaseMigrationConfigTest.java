package com.offerwave.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DatabaseMigrationConfigTest {

    private final DatabaseMigrationConfig migrationConfig = new DatabaseMigrationConfig();

    @Test
    void shouldNotBaselineUnexpectedNonEmptyDatabaseByDefault() {
        FluentConfiguration flyway = Flyway.configure();

        migrationConfig.offerWaveFlywayCustomizer(new MockEnvironment()).customize(flyway);

        assertFalse(flyway.isBaselineOnMigrate());
        assertEquals(DatabaseMigrationConfig.LEGACY_SCHEMA_VERSION, flyway.getBaselineVersion().getVersion());
    }

    @Test
    void shouldAllowExplicitAdoptionOfLegacySchema() {
        FluentConfiguration flyway = Flyway.configure();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("offerwave.database.baseline-existing-schema", "true");

        migrationConfig.offerWaveFlywayCustomizer(environment).customize(flyway);

        assertTrue(flyway.isBaselineOnMigrate());
    }

    @Test
    void shouldOwnFlywayBeanAndMigrateDuringInitialization() throws NoSuchMethodException {
        DataSource dataSource = mock(DataSource.class);
        MockEnvironment environment = new MockEnvironment();

        Flyway flyway = migrationConfig.offerWaveFlyway(
                dataSource,
                migrationConfig.offerWaveFlywayCustomizer(environment));
        Bean bean = DatabaseMigrationConfig.class
                .getDeclaredMethod("offerWaveFlyway", DataSource.class, org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer.class)
                .getAnnotation(Bean.class);

        assertSame(dataSource, flyway.getConfiguration().getDataSource());
        assertEquals("migrate", bean.initMethod());
        assertEquals(DatabaseMigrationConfig.LEGACY_SCHEMA_VERSION,
                flyway.getConfiguration().getBaselineVersion().getVersion());
    }
}
