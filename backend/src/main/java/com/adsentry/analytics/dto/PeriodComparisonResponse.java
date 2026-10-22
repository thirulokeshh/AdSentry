package com.adsentry.analytics.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeriodComparisonResponse {

    private String currency = "INR";
    private String platformFilter;

    // Current period
    private LocalDate currentStartDate;
    private LocalDate currentEndDate;
    private int currentTotalDays;
    private int currentActiveDays;
    private MetricSummaryDto currentMetrics;

    // Previous adjacent period (equal length)
    private LocalDate previousStartDate;
    private LocalDate previousEndDate;
    private int previousTotalDays;
    private int previousActiveDays;
    private MetricSummaryDto previousMetrics;

    // Evaluation & deltas
    private boolean comparisonReliable;
    private Map<String, MetricDeltaDto> deltas = new HashMap<>();
    private List<String> warnings = new ArrayList<>();

    public PeriodComparisonResponse() {
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPlatformFilter() {
        return platformFilter;
    }

    public void setPlatformFilter(String platformFilter) {
        this.platformFilter = platformFilter;
    }

    public LocalDate getCurrentStartDate() {
        return currentStartDate;
    }

    public void setCurrentStartDate(LocalDate currentStartDate) {
        this.currentStartDate = currentStartDate;
    }

    public LocalDate getCurrentEndDate() {
        return currentEndDate;
    }

    public void setCurrentEndDate(LocalDate currentEndDate) {
        this.currentEndDate = currentEndDate;
    }

    public int getCurrentTotalDays() {
        return currentTotalDays;
    }

    public void setCurrentTotalDays(int currentTotalDays) {
        this.currentTotalDays = currentTotalDays;
    }

    public int getCurrentActiveDays() {
        return currentActiveDays;
    }

    public void setCurrentActiveDays(int currentActiveDays) {
        this.currentActiveDays = currentActiveDays;
    }

    public MetricSummaryDto getCurrentMetrics() {
        return currentMetrics;
    }

    public void setCurrentMetrics(MetricSummaryDto currentMetrics) {
        this.currentMetrics = currentMetrics;
    }

    public LocalDate getPreviousStartDate() {
        return previousStartDate;
    }

    public void setPreviousStartDate(LocalDate previousStartDate) {
        this.previousStartDate = previousStartDate;
    }

    public LocalDate getPreviousEndDate() {
        return previousEndDate;
    }

    public void setPreviousEndDate(LocalDate previousEndDate) {
        this.previousEndDate = previousEndDate;
    }

    public int getPreviousTotalDays() {
        return previousTotalDays;
    }

    public void setPreviousTotalDays(int previousTotalDays) {
        this.previousTotalDays = previousTotalDays;
    }

    public int getPreviousActiveDays() {
        return previousActiveDays;
    }

    public void setPreviousActiveDays(int previousActiveDays) {
        this.previousActiveDays = previousActiveDays;
    }

    public MetricSummaryDto getPreviousMetrics() {
        return previousMetrics;
    }

    public void setPreviousMetrics(MetricSummaryDto previousMetrics) {
        this.previousMetrics = previousMetrics;
    }

    public boolean isComparisonReliable() {
        return comparisonReliable;
    }

    public void setComparisonReliable(boolean comparisonReliable) {
        this.comparisonReliable = comparisonReliable;
    }

    public Map<String, MetricDeltaDto> getDeltas() {
        return deltas;
    }

    public void setDeltas(Map<String, MetricDeltaDto> deltas) {
        this.deltas = deltas != null ? deltas : new HashMap<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
}
