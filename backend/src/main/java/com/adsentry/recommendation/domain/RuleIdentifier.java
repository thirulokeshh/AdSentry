package com.adsentry.recommendation.domain;

public enum RuleIdentifier {
    RULE_A_ZERO_CONVERSIONS("High spend with zero known conversions"),
    RULE_B_HIGH_CPA("Cost Per Acquisition (CPA) above target threshold");

    private final String description;

    RuleIdentifier(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
