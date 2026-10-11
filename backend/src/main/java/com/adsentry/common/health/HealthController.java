package com.adsentry.common.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    @Value("${adsentry.app.version:0.0.1-SNAPSHOT}")
    private String version;

    @Value("${adsentry.app.shared-workspace-warning:AdSentry is configured as a shared workspace MVP. Do not upload sensitive credentials or unredacted PII.}")
    private String sharedWorkspaceWarning;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<HealthStatusResponse> getHealth() {
        String dbStatus = "UP";
        Map<String, Object> details = new HashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(2);
            dbStatus = isValid ? "UP" : "DOWN";
            details.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            dbStatus = "DOWN";
            details.put("databaseError", e.getMessage());
        }

        String overallStatus = "UP".equals(dbStatus) ? "UP" : "DEGRADED";

        HealthStatusResponse response = new HealthStatusResponse(
                overallStatus,
                dbStatus,
                version,
                sharedWorkspaceWarning,
                Instant.now(),
                details
        );

        return "UP".equals(overallStatus)
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(503).body(response);
    }
}
