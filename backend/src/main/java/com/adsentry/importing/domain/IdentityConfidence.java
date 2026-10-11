package com.adsentry.importing.domain;

public enum IdentityConfidence {
    HIGH_ID("High confidence: Platform native Campaign ID present"),
    LOW_NAME("Lower confidence fallback: Normalized campaign name used");

    private final String description;

    IdentityConfidence(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
