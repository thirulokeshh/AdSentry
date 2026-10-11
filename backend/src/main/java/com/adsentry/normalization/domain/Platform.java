package com.adsentry.normalization.domain;

public enum Platform {
    GOOGLE_ADS("Google Ads", true),
    META_ADS("Meta Ads", true),
    UNSUPPORTED("Unsupported Channel", false);

    private final String displayName;
    private final boolean supported;

    Platform(String displayName, boolean supported) {
        this.displayName = displayName;
        this.supported = supported;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSupported() {
        return supported;
    }

    public static Platform fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNSUPPORTED;
        }
        String normalized = raw.trim().toLowerCase();
        if (normalized.contains("google")) {
            return GOOGLE_ADS;
        }
        if (normalized.contains("meta") || normalized.contains("facebook")) {
            return META_ADS;
        }
        return UNSUPPORTED;
    }
}
