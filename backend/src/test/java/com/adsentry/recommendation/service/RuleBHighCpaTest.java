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

class RuleBHighCpaTest {

    private RuleBHighCpa ruleB;
    private RecommendationThresholds defaultThresholds;

    @BeforeEach
    void setUp() {
        ruleB = new RuleBHighCpa();
        defaultThresholds = RecommendationThresholds.of(
                new BigDecimal("500.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2.0")
        );
    }

    @Test
    @DisplayName("Rule B triggers when CPA is strictly greater than target with positive compatible conversions")
    void evaluate_triggersWhenCpaStrictlyAboveTarget() {
        CampaignAnalysisContext context = createContext(
                new BigDecimal("2500.05"),
                new BigDecimal("5.00"),
                new BigDecimal("500.01"), // CPA > 500.00
                ConversionDefinition.PURCHASE,
                false, false,
                true
        );

        Optional<RecommendationCandidate> candidate = ruleB.evaluate(context, defaultThresholds);
        assertTrue(candidate.isPresent());
        assertEquals(RuleIdentifier.RULE_B_HIGH_CPA, candidate.get().ruleIdentifier());
        assertEquals(RecommendationPriority.LOW, candidate.get().priority());
        assertNotNull(candidate.get().cpa());
        assertTrue(candidate.get().explanation().contains("500.01"));
    }

    @Test
    @DisplayName("Rule B does NOT trigger when CPA is exactly at or below target")
    void evaluate_doesNotTriggerAtOrBelowTarget() {
        // Exactly at target (500.00)
        CampaignAnalysisContext exactContext = createContext(
                new BigDecimal("2500.00"),
                new BigDecimal("5.00"),
                new BigDecimal("500.00"),
                ConversionDefinition.PURCHASE,
                false, false,
                true
        );
        assertFalse(ruleB.evaluate(exactContext, defaultThresholds).isPresent(),
                "CPA exactly at target must not trigger");

        // Below target (499.99)
        CampaignAnalysisContext belowContext = createContext(
                new BigDecimal("2499.95"),
                new BigDecimal("5.00"),
                new BigDecimal("499.99"),
                ConversionDefinition.PURCHASE,
                false, false,
                true
        );
        assertFalse(ruleB.evaluate(belowContext, defaultThresholds).isPresent(),
                "CPA below target must not trigger");
    }

    @Test
    @DisplayName("Rule B requires positive conversions; zero or null conversions do NOT trigger")
    void evaluate_requiresPositiveConversions() {
        // Zero conversions (Rule B requires positive conversions; zero conversions belongs to Rule A)
        CampaignAnalysisContext zeroContext = createContext(
                new BigDecimal("2500.00"),
                BigDecimal.ZERO,
                null,
                ConversionDefinition.PURCHASE,
                false, false,
                true
        );
        assertFalse(ruleB.evaluate(zeroContext, defaultThresholds).isPresent());

        // Null conversions
        CampaignAnalysisContext nullContext = createContext(
                new BigDecimal("2500.00"),
                null,
                null,
                ConversionDefinition.PURCHASE,
                false, false,
                true
        );
        assertFalse(ruleB.evaluate(nullContext, defaultThresholds).isPresent());
    }

    @Test
    @DisplayName("Rule B does NOT trigger when conversion definition is UNSPECIFIED, mixed, or ineligible")
    void evaluate_doesNotTriggerForIncompatibleConversionDefinitions() {
        // Unspecified
        CampaignAnalysisContext unspecContext = createContext(
                new BigDecimal("5000.00"),
                new BigDecimal("5.00"),
                new BigDecimal("1000.00"),
                ConversionDefinition.UNSPECIFIED,
                true, false,
                true
        );
        assertFalse(ruleB.evaluate(unspecContext, defaultThresholds).isPresent());

        // Mixed definitions
        CampaignAnalysisContext mixedContext = createContext(
                new BigDecimal("5000.00"),
                new BigDecimal("5.00"),
                new BigDecimal("1000.00"),
                null,
                false, true,
                true
        );
        assertFalse(ruleB.evaluate(mixedContext, defaultThresholds).isPresent());
    }

    @Test
    @DisplayName("Rule B does NOT trigger when data coverage is insufficient (< 50%)")
    void evaluate_doesNotTriggerWhenCoverageInsufficient() {
        CampaignAnalysisContext context = createContext(
                new BigDecimal("5000.00"),
                new BigDecimal("5.00"),
                new BigDecimal("1000.00"),
                ConversionDefinition.PURCHASE,
                false, false,
                false // insufficient coverage
        );
        assertFalse(ruleB.evaluate(context, defaultThresholds).isPresent());
    }

    @Test
    @DisplayName("Rule B priority cutoffs: HIGH (>= 2x target = 1000.00), MEDIUM (>= 1.3x target = 650.00), LOW (< 650.00)")
    void evaluate_priorityCutoffs() {
        // CPA >= 1000.00 (2.0x 500.00) -> HIGH
        CampaignAnalysisContext highContext = createContext(
                new BigDecimal("5000.00"),
                new BigDecimal("5.00"),
                new BigDecimal("1000.00"),
                ConversionDefinition.PURCHASE,
                false, false,
                true
        );
        assertEquals(RecommendationPriority.HIGH, ruleB.evaluate(highContext, defaultThresholds).orElseThrow().priority());

        // CPA >= 650.00 (1.30x 500.00) and < 1000.00 -> MEDIUM
        CampaignAnalysisContext mediumContext = createContext(
                new BigDecimal("3500.00"),
                new BigDecimal("5.00"),
                new BigDecimal("700.00"),
                ConversionDefinition.PURCHASE,
                false, false,
                true
        );
        assertEquals(RecommendationPriority.MEDIUM, ruleB.evaluate(mediumContext, defaultThresholds).orElseThrow().priority());

        // CPA < 650.00 -> LOW
        CampaignAnalysisContext lowContext = createContext(
                new BigDecimal("2750.00"),
                new BigDecimal("5.00"),
                new BigDecimal("550.00"),
                ConversionDefinition.PURCHASE,
                false, false,
                true
        );
        assertEquals(RecommendationPriority.LOW, ruleB.evaluate(lowContext, defaultThresholds).orElseThrow().priority());
    }

    private CampaignAnalysisContext createContext(BigDecimal spend,
                                                  BigDecimal compatibleConversions,
                                                  BigDecimal cpa,
                                                  ConversionDefinition convDef,
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
                cpa,
                null,
                null
        );

        return new CampaignAnalysisContext(
                "camp-key-2",
                "Campaign 2",
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
                false,
                hasUnspecified,
                hasMixed,
                convDef,
                compatibleConversions
        );
    }
}
