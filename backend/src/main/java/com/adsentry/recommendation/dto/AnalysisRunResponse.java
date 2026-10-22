package com.adsentry.recommendation.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AnalysisRunResponse {

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String platformFilter;
    private Instant runTimestamp;
    private int totalCampaignsAnalyzed;
    private int totalRecommendationsGenerated;
    private Map<String, Object> thresholdsUsed;
    private Map<String, Object> coverageSummary;
    private List<String> warnings;
    private List<RecommendationDto> recommendations;

    public AnalysisRunResponse() {
    }

    public AnalysisRunResponse(LocalDate periodStart,
                               LocalDate periodEnd,
                               String platformFilter,
                               Instant runTimestamp,
                               int totalCampaignsAnalyzed,
                               int totalRecommendationsGenerated,
                               Map<String, Object> thresholdsUsed,
                               Map<String, Object> coverageSummary,
                               List<String> warnings,
                               List<RecommendationDto> recommendations) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.platformFilter = platformFilter;
        this.runTimestamp = runTimestamp;
        this.totalCampaignsAnalyzed = totalCampaignsAnalyzed;
        this.totalRecommendationsGenerated = totalRecommendationsGenerated;
        this.thresholdsUsed = thresholdsUsed;
        this.coverageSummary = coverageSummary;
        this.warnings = warnings;
        this.recommendations = recommendations;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getPlatformFilter() {
        return platformFilter;
    }

    public void setPlatformFilter(String platformFilter) {
        this.platformFilter = platformFilter;
    }

    public Instant getRunTimestamp() {
        return runTimestamp;
    }

    public void setRunTimestamp(Instant runTimestamp) {
        this.runTimestamp = runTimestamp;
    }

    public int getTotalCampaignsAnalyzed() {
        return totalCampaignsAnalyzed;
    }

    public void setTotalCampaignsAnalyzed(int totalCampaignsAnalyzed) {
        this.totalCampaignsAnalyzed = totalCampaignsAnalyzed;
    }

    public int getTotalRecommendationsGenerated() {
        return totalRecommendationsGenerated;
    }

    public void setTotalRecommendationsGenerated(int totalRecommendationsGenerated) {
        this.totalRecommendationsGenerated = totalRecommendationsGenerated;
    }

    public Map<String, Object> getThresholdsUsed() {
        return thresholdsUsed;
    }

    public void setThresholdsUsed(Map<String, Object> thresholdsUsed) {
        this.thresholdsUsed = thresholdsUsed;
    }

    public Map<String, Object> getCoverageSummary() {
        return coverageSummary;
    }

    public void setCoverageSummary(Map<String, Object> coverageSummary) {
        this.coverageSummary = coverageSummary;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<RecommendationDto> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationDto> recommendations) {
        this.recommendations = recommendations;
    }
}
