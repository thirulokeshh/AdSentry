package com.adsentry.common.health;

import java.time.Instant;
import java.util.Map;

public record HealthStatusResponse(
        String status,
        String database,
        String version,
        String sharedWorkspaceWarning,
        Instant timestamp,
        Map<String, Object> details
) {}
