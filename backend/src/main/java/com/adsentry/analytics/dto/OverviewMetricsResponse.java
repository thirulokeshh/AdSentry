package com.adsentry.analytics.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OverviewMetricsResponse {

    private String currency = "INR";
    private LocalDate startDate;
    private LocalDate endDate;
    private String platformFilter;
    private int totalCampaigns;
    private int totalRecords;
    private MetricSummaryDto metrics;
    private List<String> warnings = new ArrayList<>();

    public OverviewMetricsResponse() {
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

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
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
