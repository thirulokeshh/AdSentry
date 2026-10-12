package com.adsentry.normalization.domain;

/**
 * Explicit conversion definitions.
 * Per specification: Generic conversions from unmapped or standard column headers
 * MUST be marked UNSPECIFIED. They must not be assumed to mean purchase, lead, checkout,
 * or messaging conversation until confirmed as compatible.
 */
public enum ConversionDefinition {
    UNSPECIFIED(false, "Generic / Unspecified conversion action. Not CPA-compatible without confirmation."),
    PURCHASE(true, "Purchase or transaction conversion."),
    LEAD(true, "Lead generation or contact submission conversion."),
    CHECKOUT_INITIATED(true, "Checkout initiated conversion."),
    MESSAGING_CONVERSATION(true, "Messaging conversation started.");

    private final boolean cpaRuleEligible;
    private final String description;

    ConversionDefinition(boolean cpaRuleEligible, String description) {
        this.cpaRuleEligible = cpaRuleEligible;
        this.description = description;
    }

    public boolean isCpaRuleEligible() {
        return cpaRuleEligible;
    }

    public String getDescription() {
        return description;
    }

    public static ConversionDefinition fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNSPECIFIED;
        }
        String normalized = raw.trim().toUpperCase();
        for (ConversionDefinition def : values()) {
            if (def.name().equals(normalized)) {
                return def;
            }
        }
        return UNSPECIFIED;
    }

    public static ConversionDefinition detectFromHeaderOrAction(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNSPECIFIED;
        }
        String clean = raw.trim().toLowerCase();
        if (clean.contains("purchase") || clean.contains("order") || clean.contains("sale")) {
            return PURCHASE;
        }
        if (clean.contains("lead")) {
            return LEAD;
        }
        if (clean.contains("checkout")) {
            return CHECKOUT_INITIATED;
        }
        if (clean.contains("messaging") || clean.contains("conversation")) {
            return MESSAGING_CONVERSATION;
        }
        return UNSPECIFIED;
    }
}
