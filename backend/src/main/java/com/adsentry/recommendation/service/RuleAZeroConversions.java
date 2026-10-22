package com.adsentry.recommendation.service;

import com.adsentry.normalization.domain.ConversionDefinition;
import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RuleIdentifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Rule A: High spend with zero known, compatible conversions.
 * Requires spend strictly greater than configured minimum threshold,
 * known conversion data (no null conversion counts), confirmed compatible definition,
 * aggregate conversions exactly equal to zero, and sufficient data reliability.
 * Missing or unspecified conversions are NEVER flagged as zero-conversion campaigns.
 */
@Component
public class RuleAZeroConversions implements RecommendationRule {

    @Override
    public RuleIdentifier getIdentifier() {
        return RuleIdentifier.RULE_A_ZERO_CONVERSIONS;
    }

    @Override
    public Optional<RecommendationCandidate> evaluate(CampaignAnalysisContext context, RecommendationThresholds thresholds) {
        if (context == null || context.metrics() == null) {
            return Optional.empty();
        }

        BigDecimal spend = context.metrics().getSpend();
        if (spend == null || spend.compareTo(thresholds.minSpendThreshold()) <= 0) {
            // Require spend strictly greater than the configured minimum threshold
            return Optional.empty();
        }

        // Require sufficient data reliability
        if (!context.isCoverageSufficient()) {
            return Optional.empty();
        }

        // Missing conversions must not be treated as zero
        if (context.hasMissingConversions()) {
            return Optional.empty();
        }

        // Unspecified or mixed conversion definitions do not qualify as known, compatible conversions
        if (context.hasUnspecifiedConversions() || context.hasMixedConversions()) {
            return Optional.empty();
        }

        ConversionDefinition convDef = context.compatibleConversionDefinition();
        if (convDef == null || !convDef.isCpaRuleEligible()) {
            return Optional.empty();
        }

        BigDecimal conversions = context.compatibleConversions();
        if (conversions == null || conversions.compareTo(BigDecimal.ZERO) != 0) {
            // Must have known zero conversions
            return Optional.empty();
        }

        // Determine deterministic priority
        BigDecimal highCutoff = thresholds.priorityHighSpendCutoff();
        BigDecimal mediumCutoff = thresholds.minSpendThreshold().multiply(BigDecimal.valueOf(2));

        RecommendationPriority priority;
        if (spend.compareTo(highCutoff) >= 0) {
            priority = RecommendationPriority.HIGH;
        } else if (spend.compareTo(mediumCutoff) >= 0) {
            priority = RecommendationPriority.MEDIUM;
        } else {
            priority = RecommendationPriority.LOW;
        }

        String convDefName = convDef.name();

        Map<String, Object> supportingMetrics = new LinkedHashMap<>();
        supportingMetrics.put("spend", spend);
        supportingMetrics.put("currency", "INR");
        supportingMetrics.put("conversions", BigDecimal.ZERO.setScale(2));
        supportingMetrics.put("conversionDefinition", convDefName);
        supportingMetrics.put("impressions", context.metrics().getImpressions());
        supportingMetrics.put("clicks", context.metrics().getClicks());
        supportingMetrics.put("ctr", context.metrics().getCtr());
        supportingMetrics.put("cpc", context.metrics().getCpc());
        supportingMetrics.put("cpa", null);
        supportingMetrics.put("activeDays", context.activeDays());
        supportingMetrics.put("totalPeriodDays", context.totalCalendarDays());
        supportingMetrics.put("coveragePercentage", context.coveragePercentage());

        Map<String, Object> thresholdsUsed = new LinkedHashMap<>();
        thresholdsUsed.put("minSpendThreshold", thresholds.minSpendThreshold());
        thresholdsUsed.put("priorityHighSpendCutoff", highCutoff);
        thresholdsUsed.put("priorityMediumSpendCutoff", mediumCutoff);

        String title = String.format("Zero %s conversions with high spend (₹%s): %s",
                convDefName, spend.toPlainString(), context.campaignName());

        StringBuilder explanation = new StringBuilder(String.format(
                "Campaign '%s' accumulated ₹%s in spend across %d active days (%s to %s) with 0 confirmed %s conversions. " +
                        "This exceeds the configured minimum spend threshold of ₹%s. This spend is potentially at-risk.",
                context.campaignName(), spend.toPlainString(), context.activeDays(),
                context.periodStart(), context.periodEnd(), convDefName, thresholds.minSpendThreshold().toPlainString()
        ));

        if ("LOW".equalsIgnoreCase(context.identityReliability())) {
            explanation.append(" Note: Campaign identity was resolved by normalized name without a platform ID and may be ambiguous.");
        }

        String suggestedAction = "Verify conversion tracking tag health on the destination URL, inspect search terms and audience targeting relevance, evaluate landing page experience, and review campaign objective alignment before adjusting budget.";

        String triggerCondition = String.format("Spend (₹%s) > minimum threshold (₹%s) with 0 confirmed %s conversions",
                spend.toPlainString(), thresholds.minSpendThreshold().toPlainString(), convDefName);

        RecommendationCandidate candidate = new RecommendationCandidate(
                RuleIdentifier.RULE_A_ZERO_CONVERSIONS,
                context.platform(),
                context.campaignName(),
                context.campaignIdentityKey(),
                context.identityReliability(),
                context.periodStart(),
                context.periodEnd(),
                priority,
                title,
                explanation.toString(),
                suggestedAction,
                supportingMetrics,
                triggerCondition,
                thresholdsUsed,
                spend,
                null // cpa not applicable
        );

        return Optional.of(candidate);
    }
}
