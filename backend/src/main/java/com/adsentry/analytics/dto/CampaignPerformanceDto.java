package com.adsentry.analytics.dto;

import java.util.ArrayList;
import java.util.List;

public class CampaignPerformanceDto {

    private String campaignIdentityKey;
    private String campaignName;
    private String platform;
    private String identityConfidence;
    private int activeDays;
    private MetricSummaryDto metrics;
    private List<String> warnings = new ArrayList<>();

    public CampaignPerformanceDto() {
    }

    public CampaignPerformanceDto(String campaignIdentityKey,
                                  String campaignName,
                                  String platform,
                                  String identityConfidence,
                                  int activeDays,
                                  MetricSummaryDto metrics,
                                  List<String> warnings) {
        this.campaignIdentityKey = campaignIdentityKey;
        this.campaignName = campaignName;
        this.platform = platform;
        this.identityConfidence = identityConfidence;
        this.activeDays = activeDays;
        this.metrics = metrics;
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public String getCampaignIdentityKey() {
        return campaignIdentityKey;
    }

    public void setCampaignIdentityKey(String campaignIdentityKey) {
        this.campaignIdentityKey = campaignIdentityKey;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getIdentityConfidence() {
        return identityConfidence;
    }

    public void setIdentityConfidence(String identityConfidence) {
        this.identityConfidence = identityConfidence;
    }

    public int getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(int activeDays) {
        this.activeDays = activeDays;
    }

    public MetricSummaryDto getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricSummaryDto metrics) {
        this.metrics = metrics;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
}
