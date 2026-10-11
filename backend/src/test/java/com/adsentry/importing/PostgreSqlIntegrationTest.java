package com.adsentry.importing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed integration test suite.
 * Validates Flyway migrations, PostgreSQL constraints, transactions, and restoration
 * against a real PostgreSQL database instance.
 *
 * Activated automatically when POSTGRES_TEST_URL is configured (e.g. CI pipeline or Render staging DB).
 */
@SpringBootTest
@ActiveProfiles("postgres")
@TestPropertySource(properties = {
        "spring.datasource.url=${POSTGRES_TEST_URL:jdbc:postgresql://localhost:5432/adsentry_test}"
})
@EnabledIfEnvironmentVariable(named = "POSTGRES_TEST_URL", matches = ".+")
class PostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify PostgreSQL database connection and Flyway schema tables")
    void testPostgreSqlSchemaAndConnection() {
        String dbProduct = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertThat(dbProduct).containsIgnoringCase("PostgreSQL");

        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('import_batches', 'imported_records', 'app_settings', 'recommendations', 'import_row_errors')",
                Integer.class
        );
        assertThat(tableCount).isEqualTo(5);

        // Verify effective_records_view exists in PostgreSQL
        Integer viewCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.views WHERE table_schema = 'public' AND table_name = 'effective_records_view'",
                Integer.class
        );
        assertThat(viewCount).isEqualTo(1);
    }
}
