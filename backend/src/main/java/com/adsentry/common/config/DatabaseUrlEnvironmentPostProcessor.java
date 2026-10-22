package com.adsentry.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Automatically converts cloud-provider (Render/Heroku) database URLs formatted as
 * {@code postgres://user:password@host:port/database} into standard Spring Boot
 * {@code jdbc:postgresql://host:port/database} URLs and extracts credentials if not
 * separately provided.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            databaseUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        }
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        databaseUrl = databaseUrl.trim();

        if ((databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))
                && !databaseUrl.startsWith("jdbc:")) {
            try {
                URI uri = new URI(databaseUrl);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String query = uri.getQuery();

                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path != null ? path : "");

                if (query != null && !query.isBlank()) {
                    jdbcUrl.append("?").append(query);
                }

                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl.toString());

                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    String user = parts[0];
                    String pass = parts.length > 1 ? parts[1] : "";

                    if (environment.getProperty("spring.datasource.username") == null
                            && environment.getProperty("DATABASE_USERNAME") == null) {
                        props.put("spring.datasource.username", user);
                    }
                    if (environment.getProperty("spring.datasource.password") == null
                            && environment.getProperty("DATABASE_PASSWORD") == null) {
                        props.put("spring.datasource.password", pass);
                    }
                }

                environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrlProperties", props));
            } catch (Exception ignored) {
                // Fall back to standard Spring Boot datasource resolution if URI cannot be parsed
            }
        }
    }
}
