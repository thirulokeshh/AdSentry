package com.adsentry.recommendation.service;

import com.adsentry.analytics.dto.MetricSummaryDto;
import com.adsentry.normalization.domain.ConversionDefinition;
import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RuleIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RuleAZeroConversionsTest {

    private RuleAZeroConversions ruleA;
    private RecommendationThresholds defaultThresholds;

    @BeforeEach
    void setUp() {
        ruleA = new RuleAZeroConversions();
        defaultThresholds = RecommendationThresholds.of(
                new BigDecimal("500.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2.0")
        );
    }

    @Test
    @DisplayName("Rule A triggers when spend is strictly greater than minSpendThreshold with zero known conversions")
    void evaluate_triggersWhenSpendStrictlyGreaterThanThreshold() {
        CampaignAnalysisContext context = createContext(
                new BigDecimal("1000.01"),
                BigDecimal.ZERO,
                ConversionDefinition.PURCHASE,
                false, false, false,
                true
        );

        Optional<RecommendationCandidate> candidate = ruleA.evaluate(context, defaultThresholds);
        assertTrue(candidate.isPresent());
        assertEquals(RuleIdentifier.RULE_A_ZERO_CONVERSIONS, candidate.get().ruleIdentifier());
        assertEquals(RecommendationPriority.LOW, candidate.get().priority());
        assertTrue(candidate.get().explanation().contains("potentially at-risk"));
        assertFalse(candidate.get().explanation().toLowerCase().contains("wasted spend"));
    }

    @Test
    @DisplayName("Rule A does NOT trigger when spend is exactly at or below minSpendThreshold")
    void evaluate_doesNotTriggerAtOrBelowThreshold() {
        // Exactly at threshold
        CampaignAnalysisContext exactContext = createContext(
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                ConversionDefinition.PURCHASE,
                false, false, false,
                true
        );
        Optional<RecommendationCandidate> exactResult = ruleA.evaluate(exactContext, defaultThresholds);
        assertFalse(exactResult.isPresent(), "Spend exactly equal to threshold must not trigger");

        // Below threshold
        CampaignAnalysisContext belowContext = createContext(
                new BigDecimal("999.99"),
                BigDecimal.ZERO,
                ConversionDefinition.PURCHASE,
                false, false, false,
                true
        );
        Optional<RecommendationCandidate> belowResult = ruleA.evaluate(belowContext, defaultThresholds);
        assertFalse(belowResult.isPresent(), "Spend below threshold must not trigger");
    }

    @Test
    @DisplayName("Rule A does NOT trigger when conversions are missing (null) - missing must not be treated as zero")
    void evaluate_doesNotTriggerWhenConversionsMissing() {
        CampaignAnalysisContext context = createContext(
                new BigDecimal("3000.00"),
                null,
                ConversionDefinition.PURCHASE,
                true, // hasMissingConversions = true
                false,
                false,
                true
        );

        Optional<RecommendationCandidate> result = ruleA.evaluate(context, defaultThresholds);
        assertFalse(result.isPresent(), "Missing conversions must never be treated as zero conversions");
    }

    @Test
    @DisplayName("Rule A does NOT trigger when conversions are positive")
    void evaluate_doesNotTriggerWhenConversionsPositive() {
        CampaignAnalysisContext context = createContext(
                new BigDecimal("3000.00"),
                new BigDecimal("1.00"),
                ConversionDefinition.PURCHASE,
                false, false, false,
                true
        );

        Optional<RecommendationCandidate> result = ruleA.evaluate(context, defaultThresholds);
        assertFalse(result.isPresent(), "Positive conversions must not trigger zero-conversion rule");
    }

    @Test
    @DisplayName("Rule A does NOT trigger when conversion definition is UNSPECIFIED or mixed")
    void evaluate_doesNotTriggerForUnspecifiedOrMixedConversions() {
        // Unspecified
        CampaignAnalysisContext unspecifiedContext = createContext(
                new BigDecimal("3000.00"),
                BigDecimal.ZERO,
                ConversionDefinition.UNSPECIFIED,
                false, true, false,
                true
        );
        assertFalse(ruleA.evaluate(unspecifiedContext, defaultThresholds).isPresent());

        // Mixed definitions
        CampaignAnalysisContext mixedContext = createContext(
                new BigDecimal("3000.00"),
                BigDecimal.ZERO,
                null,
                false, false, true,
                true
        );
        assertFalse(ruleA.evaluate(mixedContext, defaultThresholds).isPresent());
    }

    @Test
    @DisplayName("Rule A does NOT trigger when data coverage is insufficient (< 50%)")
    void evaluate_doesNotTriggerWhenCoverageInsufficient() {
        CampaignAnalysisContext context = createContext(
                new BigDecimal("3000.00"),
                BigDecimal.ZERO,
                ConversionDefinition.PURCHASE,
                false, false, false,
                false // coverage sufficient = false
        );

        Optional<RecommendationCandidate> result = ruleA.evaluate(context, defaultThresholds);
        assertFalse(result.isPresent(), "Insufficient coverage must suppress recommendation");
    }

    @Test
    @DisplayName("Rule A priority cutoffs: HIGH (>= high cutoff), MEDIUM (>= 2x minSpend), LOW (< 2x minSpend)")
    void evaluate_priorityCutoffs() {
        // High spend cutoff is 5000.00
        CampaignAnalysisContext highContext = createContext(
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                ConversionDefinition.PURCHASE,
                false, false, false,
                true
        );
        assertEquals(RecommendationPriority.HIGH, ruleA.evaluate(highContext, defaultThresholds).orElseThrow().priority());

        // Medium spend cutoff is 2 * minSpendThreshold = 2000.00
        CampaignAnalysisContext mediumContext = createContext(
                new BigDecimal("2000.00"),
                BigDecimal.ZERO,
                ConversionDefinition.PURCHASE,
                false, false, false,
                true
        );
        assertEquals(RecommendationPriority.MEDIUM, ruleA.evaluate(mediumContext, defaultThresholds).orElseThrow().priority());

        // Low spend cutoff is < 2000.00
        CampaignAnalysisContext lowContext = createContext(
                new BigDecimal("1500.00"),
                BigDecimal.ZERO,
                ConversionDefinition.PURCHASE,
                false, false, false,
                true
        );
        assertEquals(RecommendationPriority.LOW, ruleA.evaluate(lowContext, defaultThresholds).orElseThrow().priority());
    }

    private CampaignAnalysisContext createContext(BigDecimal spend,
                                                  BigDecimal compatibleConversions,
                                                  ConversionDefinition convDef,
                                                  boolean hasMissing,
                                                  boolean hasUnspecified,
                                                  boolean hasMixed,
                                                  boolean coverageSufficient) {
        MetricSummaryDto metrics = new MetricSummaryDto(
                spend,
                1000L,
                50L,
                compatibleConversions,
                convDef != null ? convDef.name() : null,
                new BigDecimal("5.00"),
                new BigDecimal("20.00"),
                null,
                null,
                null
        );

        return new CampaignAnalysisContext(
                "camp-key-1",
                "Campaign 1",
                "Google Ads",
                "HIGH",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 10),
                coverageSufficient ? 8 : 2,
                10,
                coverageSufficient ? new BigDecimal("80.00") : new BigDecimal("20.00"),
                coverageSufficient,
                Collections.emptyList(),
                metrics,
                hasMissing,
                hasUnspecified,
                hasMixed,
                convDef,
                compatibleConversions
        );
    }
}
