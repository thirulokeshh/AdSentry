package com.adsentry.export.service;

import com.adsentry.analytics.dto.CampaignPerformanceDto;
import com.adsentry.analytics.dto.CampaignPerformanceResponse;
import com.adsentry.analytics.dto.MetricSummaryDto;
import com.adsentry.analytics.service.AnalyticsService;
import com.adsentry.recommendation.dto.RecommendationDto;
import com.adsentry.recommendation.service.RecommendationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ExportService {

    private final AnalyticsService analyticsService;
    private final RecommendationService recommendationService;

    public ExportService(AnalyticsService analyticsService, RecommendationService recommendationService) {
        this.analyticsService = analyticsService;
        this.recommendationService = recommendationService;
    }

    /**
     * Generates RFC-4180 compliant CSV content for campaign performance metrics.
     * Missing/undefined metrics are left blank (never zero). Formula injection is mitigated.
     */
    public String exportCampaignsCsv(LocalDate startDate, LocalDate endDate, String platform) {
        CampaignPerformanceResponse response = analyticsService.getCampaignPerformance(startDate, endDate, platform);
        StringBuilder sb = new StringBuilder();

        // Header row
        sb.append(String.join(",", List.of(
                "Campaign Identity Key",
                "Campaign Name",
                "Platform",
                "Active Days",
                "Spend (INR)",
                "Impressions",
                "Clicks",
                "CTR (%)",
                "CPC (INR)",
                "Conversions",
                "CPA (INR)",
                "Conversion Rate (%)",
                "Identity Confidence",
                "Warnings"
        ))).append("\r\n");

        for (CampaignPerformanceDto campaign : response.getCampaigns()) {
            MetricSummaryDto m = campaign.getMetrics();
            String warnings = campaign.getWarnings() != null && !campaign.getWarnings().isEmpty()
                    ? String.join(" | ", campaign.getWarnings())
                    : "";

            sb.append(CsvSanitizer.escapeField(campaign.getCampaignIdentityKey())).append(",");
            sb.append(CsvSanitizer.escapeField(campaign.getCampaignName())).append(",");
            sb.append(CsvSanitizer.escapeField(campaign.getPlatform())).append(",");
            sb.append(campaign.getActiveDays()).append(",");
            sb.append(m != null && m.getSpend() != null ? m.getSpend().toPlainString() : "").append(",");
            sb.append(m != null && m.getImpressions() != null ? m.getImpressions().toString() : "").append(",");
            sb.append(m != null && m.getClicks() != null ? m.getClicks().toString() : "").append(",");
            sb.append(m != null && m.getCtr() != null ? m.getCtr().toPlainString() : "").append(",");
            sb.append(m != null && m.getCpc() != null ? m.getCpc().toPlainString() : "").append(",");
            sb.append(m != null && m.getConversions() != null ? m.getConversions().toPlainString() : "").append(",");
            sb.append(m != null && m.getCpa() != null ? m.getCpa().toPlainString() : "").append(",");
            sb.append(m != null && m.getConversionRate() != null ? m.getConversionRate().toPlainString() : "").append(",");
            sb.append(CsvSanitizer.escapeField(campaign.getIdentityConfidence())).append(",");
            sb.append(CsvSanitizer.escapeField(warnings));
            sb.append("\r\n");
        }

        return sb.toString();
    }

    /**
     * Generates RFC-4180 compliant CSV content for recommendations.
     */
    public String exportRecommendationsCsv(LocalDate startDate, LocalDate endDate, String platform) {
        List<RecommendationDto> recommendations = recommendationService.getRecommendations(
                startDate, endDate, platform, null, null, null
        );
        StringBuilder sb = new StringBuilder();

        // Header row
        sb.append(String.join(",", List.of(
                "ID",
                "Fingerprint",
                "Rule",
                "Rule Description",
                "Platform",
                "Campaign Name",
                "Period Start",
                "Period End",
                "Priority",
                "Status",
                "Title",
                "Explanation",
                "Suggested Action",
                "Trigger Condition",
                "Created At"
        ))).append("\r\n");

        for (RecommendationDto rec : recommendations) {
            sb.append(rec.getId() != null ? rec.getId().toString() : "").append(",");
            sb.append(CsvSanitizer.escapeField(rec.getFingerprint())).append(",");
            sb.append(rec.getRuleIdentifier() != null ? rec.getRuleIdentifier().name() : "").append(",");
            sb.append(CsvSanitizer.escapeField(rec.getRuleDescription())).append(",");
            sb.append(CsvSanitizer.escapeField(rec.getPlatform())).append(",");
            sb.append(CsvSanitizer.escapeField(rec.getCampaignName())).append(",");
            sb.append(rec.getPeriodStart() != null ? rec.getPeriodStart().toString() : "").append(",");
            sb.append(rec.getPeriodEnd() != null ? rec.getPeriodEnd().toString() : "").append(",");
            sb.append(rec.getPriority() != null ? rec.getPriority().name() : "").append(",");
            sb.append(rec.getStatus() != null ? rec.getStatus().name() : "").append(",");
            sb.append(CsvSanitizer.escapeField(rec.getTitle())).append(",");
            sb.append(CsvSanitizer.escapeField(rec.getExplanation())).append(",");
            sb.append(CsvSanitizer.escapeField(rec.getSuggestedAction())).append(",");
            sb.append(CsvSanitizer.escapeField(rec.getTriggerCondition())).append(",");
            sb.append(rec.getCreatedAt() != null ? rec.getCreatedAt().toString() : "");
            sb.append("\r\n");
        }

        return sb.toString();
    }
}
