package com.adsentry.analytics.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CampaignPerformanceResponse {

    private String currency = "INR";
    private LocalDate startDate;
    private LocalDate endDate;
    private String platformFilter;
    private int totalCampaigns;
    private List<CampaignPerformanceDto> campaigns = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public CampaignPerformanceResponse() {
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getPlatformFilter() {
        return platformFilter;
    }

    public void setPlatformFilter(String platformFilter) {
        this.platformFilter = platformFilter;
    }

    public int getTotalCampaigns() {
        return totalCampaigns;
    }

    public void setTotalCampaigns(int totalCampaigns) {
        this.totalCampaigns = totalCampaigns;
    }

    public List<CampaignPerformanceDto> getCampaigns() {
        return campaigns;
    }

    public void setCampaigns(List<CampaignPerformanceDto> campaigns) {
        this.campaigns = campaigns != null ? campaigns : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
}
