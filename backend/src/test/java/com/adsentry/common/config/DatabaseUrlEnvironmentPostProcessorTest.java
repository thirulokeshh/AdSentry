package com.adsentry.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    @DisplayName("Converts Render postgres:// URL into jdbc:postgresql:// with credentials")
    void convertsRenderPostgresUrl() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgres://renderuser:secret123@dpg-abc12345.oregon-postgres.render.com:5432/adsentry_db");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-abc12345.oregon-postgres.render.com:5432/adsentry_db");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("renderuser");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("secret123");
    }

    @Test
    @DisplayName("Converts postgresql:// URI without explicit port to default port 5432 and appends query string")
    void convertsPostgresqlUriWithQuery() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgresql://admin:pass@dbhost/marketing_db?sslmode=require");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dbhost:5432/marketing_db?sslmode=require");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("admin");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("pass");
    }

    @Test
    @DisplayName("Preserves standard jdbc:postgresql:// URL unchanged")
    void leavesStandardJdbcUrlUnchanged() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "jdbc:postgresql://localhost:5432/adsentry");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url")).isNull();
    }

    @Test
    @DisplayName("Respects existing explicit username and password when present")
    void respectsExistingExplicitCredentials() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "postgres://user1:pass1@host:5432/db");
        env.setProperty("DATABASE_USERNAME", "custom_user");
        env.setProperty("DATABASE_PASSWORD", "custom_pass");

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://host:5432/db");
        assertThat(env.getProperty("spring.datasource.username")).isNull(); // was not set by processor because DATABASE_USERNAME exists
        assertThat(env.getProperty("DATABASE_USERNAME")).isEqualTo("custom_user");
    }

    @Test
    @DisplayName("Does nothing when DATABASE_URL is not set")
    void doesNothingWhenUrlNotSet() {
        MockEnvironment env = new MockEnvironment();

        processor.postProcessEnvironment(env, null);

        assertThat(env.getProperty("spring.datasource.url")).isNull();
    }
}
