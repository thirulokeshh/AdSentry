package com.adsentry.recommendation.service;

import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RuleIdentifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record RecommendationCandidate(
        RuleIdentifier ruleIdentifier,
        String platform,
        String campaignName,
        String campaignIdentityKey,
        String identityReliability,
        LocalDate periodStart,
        LocalDate periodEnd,
        RecommendationPriority priority,
        String title,
        String explanation,
        String suggestedAction,
        Map<String, Object> supportingMetrics,
        String triggerCondition,
        Map<String, Object> thresholdsUsed,
        BigDecimal spend,
        BigDecimal cpa
) {
}
