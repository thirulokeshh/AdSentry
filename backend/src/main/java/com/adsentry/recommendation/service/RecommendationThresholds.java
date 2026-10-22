package com.adsentry.recommendation.service;

import java.math.BigDecimal;

public record RecommendationThresholds(
        BigDecimal globalCpaTarget,
        BigDecimal minSpendThreshold,
        BigDecimal priorityHighSpendCutoff,
        BigDecimal priorityHighCpaMultiplier
) {
    public static RecommendationThresholds of(BigDecimal globalCpaTarget,
                                              BigDecimal minSpendThreshold,
                                              BigDecimal priorityHighSpendCutoff,
                                              BigDecimal priorityHighCpaMultiplier) {
        return new RecommendationThresholds(
                globalCpaTarget != null ? globalCpaTarget : new BigDecimal("500.00"),
                minSpendThreshold != null ? minSpendThreshold : new BigDecimal("1000.00"),
                priorityHighSpendCutoff != null ? priorityHighSpendCutoff : new BigDecimal("5000.00"),
                priorityHighCpaMultiplier != null ? priorityHighCpaMultiplier : new BigDecimal("2.0")
        );
    }
}
