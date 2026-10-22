package com.adsentry.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DataCoverageResponse {

    private String currency = "INR";
    private LocalDate startDate;
    private LocalDate endDate;
    private String platformFilter;
    private int totalCalendarDays;
    private int activeDays;
    private int missingDays;
    private BigDecimal coveragePercentage;
    private boolean sufficientCoverage;
    private List<String> warnings = new ArrayList<>();

    public DataCoverageResponse() {
    }

    public DataCoverageResponse(String currency,
                                LocalDate startDate,
                                LocalDate endDate,
                                String platformFilter,
                                int totalCalendarDays,
                                int activeDays,
                                int missingDays,
                                BigDecimal coveragePercentage,
                                boolean sufficientCoverage,
                                List<String> warnings) {
        this.currency = currency != null ? currency : "INR";
        this.startDate = startDate;
        this.endDate = endDate;
        this.platformFilter = platformFilter;
        this.totalCalendarDays = totalCalendarDays;
        this.activeDays = activeDays;
        this.missingDays = missingDays;
        this.coveragePercentage = coveragePercentage;
        this.sufficientCoverage = sufficientCoverage;
        this.warnings = warnings != null ? warnings : new ArrayList<>();
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

    public int getTotalCalendarDays() {
        return totalCalendarDays;
    }

    public void setTotalCalendarDays(int totalCalendarDays) {
        this.totalCalendarDays = totalCalendarDays;
    }

    public int getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(int activeDays) {
        this.activeDays = activeDays;
    }

    public int getMissingDays() {
        return missingDays;
    }

    public void setMissingDays(int missingDays) {
        this.missingDays = missingDays;
    }

    public BigDecimal getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(BigDecimal coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public boolean isSufficientCoverage() {
        return sufficientCoverage;
    }

    public void setSufficientCoverage(boolean sufficientCoverage) {
        this.sufficientCoverage = sufficientCoverage;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
}
