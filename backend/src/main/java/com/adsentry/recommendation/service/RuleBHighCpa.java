package com.adsentry.recommendation.service;

import com.adsentry.normalization.domain.ConversionDefinition;
import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RuleIdentifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Rule B: CPA above the configured target threshold.
 * Requires known positive INR spend, positive compatible conversions,
 * a confirmed compatible conversion definition (not UNSPECIFIED or MIXED),
 * aggregate CPA strictly greater than the configured target, and sufficient data reliability.
 */
@Component
public class RuleBHighCpa implements RecommendationRule {

    @Override
    public RuleIdentifier getIdentifier() {
        return RuleIdentifier.RULE_B_HIGH_CPA;
    }

    @Override
    public Optional<RecommendationCandidate> evaluate(CampaignAnalysisContext context, RecommendationThresholds thresholds) {
        if (context == null || context.metrics() == null) {
            return Optional.empty();
        }

        BigDecimal spend = context.metrics().getSpend();
        if (spend == null || spend.compareTo(BigDecimal.ZERO) <= 0) {
            // Require known, positive INR spend
            return Optional.empty();
        }

        // Require sufficient data reliability
        if (!context.isCoverageSufficient()) {
            return Optional.empty();
        }

        // Conversion definitions must be unambiguous and compatible
        if (context.hasUnspecifiedConversions() || context.hasMixedConversions()) {
            return Optional.empty();
        }

        ConversionDefinition convDef = context.compatibleConversionDefinition();
        if (convDef == null || !convDef.isCpaRuleEligible()) {
            return Optional.empty();
        }

        BigDecimal conversions = context.compatibleConversions();
        if (conversions == null || conversions.compareTo(BigDecimal.ZERO) <= 0) {
            // Rule B requires positive compatible conversions
            return Optional.empty();
        }

        BigDecimal cpa = context.metrics().getCpa();
        if (cpa == null || cpa.compareTo(thresholds.globalCpaTarget()) <= 0) {
            // Require CPA strictly greater than configured target
            return Optional.empty();
        }

        // Calculate over-target deltas
        BigDecimal target = thresholds.globalCpaTarget();
        BigDecimal amountOverTarget = cpa.subtract(target).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentOverTarget = target.compareTo(BigDecimal.ZERO) > 0
                ? amountOverTarget.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Determine deterministic priority
        BigDecimal highCutoff = target.multiply(thresholds.priorityHighCpaMultiplier()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal mediumCutoff = target.multiply(new BigDecimal("1.30")).setScale(2, RoundingMode.HALF_UP);

        RecommendationPriority priority;
        if (cpa.compareTo(highCutoff) >= 0) {
            priority = RecommendationPriority.HIGH;
        } else if (cpa.compareTo(mediumCutoff) >= 0) {
            priority = RecommendationPriority.MEDIUM;
        } else {
            priority = RecommendationPriority.LOW;
        }

        String convDefName = convDef.name();

        Map<String, Object> supportingMetrics = new LinkedHashMap<>();
        supportingMetrics.put("spend", spend);
        supportingMetrics.put("currency", "INR");
        supportingMetrics.put("conversions", conversions);
        supportingMetrics.put("conversionDefinition", convDefName);
        supportingMetrics.put("cpa", cpa);
        supportingMetrics.put("targetCpa", target);
        supportingMetrics.put("amountOverTarget", amountOverTarget);
        supportingMetrics.put("percentOverTarget", percentOverTarget);
        supportingMetrics.put("impressions", context.metrics().getImpressions());
        supportingMetrics.put("clicks", context.metrics().getClicks());
        supportingMetrics.put("ctr", context.metrics().getCtr());
        supportingMetrics.put("cpc", context.metrics().getCpc());
        supportingMetrics.put("activeDays", context.activeDays());
        supportingMetrics.put("totalPeriodDays", context.totalCalendarDays());
        supportingMetrics.put("coveragePercentage", context.coveragePercentage());

        Map<String, Object> thresholdsUsed = new LinkedHashMap<>();
        thresholdsUsed.put("globalCpaTarget", target);
        thresholdsUsed.put("priorityHighCpaMultiplier", thresholds.priorityHighCpaMultiplier());
        thresholdsUsed.put("priorityHighCpaCutoff", highCutoff);
        thresholdsUsed.put("priorityMediumCpaCutoff", mediumCutoff);

        String title = String.format("CPA (₹%s) exceeds target (₹%s): %s",
                cpa.toPlainString(), target.toPlainString(), context.campaignName());

        StringBuilder explanation = new StringBuilder(String.format(
                "Campaign '%s' recorded an aggregate CPA of ₹%s for %s conversions across %d active days (%s to %s), " +
                        "exceeding the configured target CPA of ₹%s by ₹%s (%.1f%% above target).",
                context.campaignName(), cpa.toPlainString(), convDefName, context.activeDays(),
                context.periodStart(), context.periodEnd(), target.toPlainString(),
                amountOverTarget.toPlainString(), percentOverTarget.doubleValue()
        ));

        if ("LOW".equalsIgnoreCase(context.identityReliability())) {
            explanation.append(" Note: Campaign identity was resolved by normalized name without a platform ID and may be ambiguous.");
        }

        String suggestedAction = "Review search terms to add negative keywords, evaluate audience targeting precision, review ad creative relevance and fatigue, and consider revising bidding strategy or target CPA settings.";

        String triggerCondition = String.format("Actual CPA (₹%s) > target CPA (₹%s) for confirmed %s conversions",
                cpa.toPlainString(), target.toPlainString(), convDefName);

        RecommendationCandidate candidate = new RecommendationCandidate(
                RuleIdentifier.RULE_B_HIGH_CPA,
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
                cpa
        );

        return Optional.of(candidate);
    }
}
