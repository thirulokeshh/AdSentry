package com.adsentry.analytics.service;

import com.adsentry.analytics.dto.MetricSummaryDto;
import com.adsentry.analytics.dto.MetricsCalculationResult;
import com.adsentry.importing.domain.EffectiveRecord;
import com.adsentry.normalization.domain.ConversionDefinition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class MetricsCalculator {

    public MetricsCalculationResult calculate(List<EffectiveRecord> records) {
        List<String> warnings = new ArrayList<>();

        if (records == null || records.isEmpty()) {
            MetricSummaryDto emptySummary = new MetricSummaryDto(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0L,
                    0L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            warnings.add("No records available for the selected criteria.");
            return new MetricsCalculationResult(emptySummary, warnings);
        }

        BigDecimal totalSpend = BigDecimal.ZERO;
        long totalImpressions = 0L;
        boolean hasImpressions = false;
        long totalClicks = 0L;
        boolean hasClicks = false;

        Map<ConversionDefinition, BigDecimal> compatibleConversionsMap = new LinkedHashMap<>();
        BigDecimal unspecifiedConversionsTotal = BigDecimal.ZERO;
        boolean hasUnspecifiedConversions = false;
        boolean hasReportedConversions = false;
        boolean hasMissingConversions = false;

        for (EffectiveRecord record : records) {
            if (record.getSpend() != null) {
                totalSpend = totalSpend.add(record.getSpend());
            }

            if (record.getImpressions() != null) {
                totalImpressions += record.getImpressions();
                hasImpressions = true;
            }

            if (record.getClicks() != null) {
                totalClicks += record.getClicks();
                hasClicks = true;
            }

            if (record.getConversions() != null) {
                hasReportedConversions = true;
                ConversionDefinition def = record.getConversionDefinition() != null
                        ? record.getConversionDefinition()
                        : ConversionDefinition.UNSPECIFIED;

                if (def == ConversionDefinition.UNSPECIFIED) {
                    unspecifiedConversionsTotal = unspecifiedConversionsTotal.add(record.getConversions());
                    hasUnspecifiedConversions = true;
                } else if (def.isCpaRuleEligible()) {
                    compatibleConversionsMap.merge(def, record.getConversions(), BigDecimal::add);
                }
            } else {
                hasMissingConversions = true;
            }
        }

        totalSpend = totalSpend.setScale(2, RoundingMode.HALF_UP);
        Long impressions = hasImpressions ? totalImpressions : null;
        Long clicks = hasClicks ? totalClicks : null;

        if (!hasImpressions) {
            warnings.add("Impressions were not reported in this dataset.");
        }
        if (!hasClicks) {
            warnings.add("Clicks were not reported in this dataset.");
        }
        if (!hasReportedConversions) {
            warnings.add("Conversions were not reported in this dataset.");
        } else if (hasMissingConversions) {
            warnings.add("Some records had missing conversion counts. Missing conversion values were not treated as zero.");
        }

        // Conversion compatibility evaluation
        BigDecimal compatibleConversions = null;
        String conversionDefinitionStr = null;

        if (compatibleConversionsMap.size() > 1) {
            // Mixed definitions: e.g. both PURCHASE and LEAD
            conversionDefinitionStr = "MIXED";
            warnings.add("Heterogeneous conversion definitions detected ("
                    + String.join(", ", compatibleConversionsMap.keySet().stream().map(Enum::name).toList())
                    + "). Leads and purchases cannot be combined into a single CPA or conversion rate metric.");
        } else if (compatibleConversionsMap.size() == 1) {
            Map.Entry<ConversionDefinition, BigDecimal> singleEntry = compatibleConversionsMap.entrySet().iterator().next();
            conversionDefinitionStr = singleEntry.getKey().name();
            compatibleConversions = singleEntry.getValue().setScale(2, RoundingMode.HALF_UP);

            if (hasUnspecifiedConversions && unspecifiedConversionsTotal.compareTo(BigDecimal.ZERO) > 0) {
                warnings.add("Unspecified conversions (total " + unspecifiedConversionsTotal.toPlainString()
                        + ") were excluded from " + conversionDefinitionStr + " CPA and conversion rate calculations.");
            }
        } else {
            // No compatible definitions found
            if (hasUnspecifiedConversions) {
                conversionDefinitionStr = ConversionDefinition.UNSPECIFIED.name();
                warnings.add("Conversions in the dataset are generic/unspecified and are excluded from CPA and conversion rate calculations.");
            }
        }

        // Computed ratios from aggregate totals
        BigDecimal ctr = null;
        if (impressions != null && impressions > 0 && clicks != null) {
            ctr = BigDecimal.valueOf(clicks)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(impressions), 2, RoundingMode.HALF_UP);
        }

        BigDecimal cpc = null;
        if (clicks != null && clicks > 0 && totalSpend != null) {
            cpc = totalSpend.divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP);
        }

        // CPA: MUST be null if compatibleConversions is null or 0 (NEVER ₹0)
        BigDecimal cpa = null;
        if (compatibleConversions != null && compatibleConversions.compareTo(BigDecimal.ZERO) > 0 && totalSpend != null) {
            cpa = totalSpend.divide(compatibleConversions, 2, RoundingMode.HALF_UP);
        }

        BigDecimal conversionRate = null;
        if (clicks != null && clicks > 0 && compatibleConversions != null) {
            conversionRate = compatibleConversions
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP);
        }

        BigDecimal cpm = null;
        if (impressions != null && impressions > 0 && totalSpend != null) {
            cpm = totalSpend
                    .multiply(BigDecimal.valueOf(1000))
                    .divide(BigDecimal.valueOf(impressions), 2, RoundingMode.HALF_UP);
        }

        MetricSummaryDto summary = new MetricSummaryDto(
                totalSpend,
                impressions,
                clicks,
                compatibleConversions,
                conversionDefinitionStr,
                ctr,
                cpc,
                cpa,
                conversionRate,
                cpm
        );

        return new MetricsCalculationResult(summary, warnings);
    }
}
