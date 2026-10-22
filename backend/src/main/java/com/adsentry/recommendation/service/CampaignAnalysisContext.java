package com.adsentry.recommendation.service;

import com.adsentry.analytics.dto.MetricSummaryDto;
import com.adsentry.importing.domain.EffectiveRecord;
import com.adsentry.normalization.domain.ConversionDefinition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CampaignAnalysisContext(
        String campaignIdentityKey,
        String campaignName,
        String platform,
        String identityReliability,
        LocalDate periodStart,
        LocalDate periodEnd,
        int activeDays,
        int totalCalendarDays,
        BigDecimal coveragePercentage,
        boolean isCoverageSufficient,
        List<EffectiveRecord> records,
        MetricSummaryDto metrics,
        boolean hasMissingConversions,
        boolean hasUnspecifiedConversions,
        boolean hasMixedConversions,
        ConversionDefinition compatibleConversionDefinition,
        BigDecimal compatibleConversions
) {
}
