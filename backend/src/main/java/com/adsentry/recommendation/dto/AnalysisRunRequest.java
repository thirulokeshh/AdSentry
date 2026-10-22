package com.adsentry.recommendation.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AnalysisRunRequest {

    private LocalDate startDate;
    private LocalDate endDate;
    private String platform;

    @DecimalMin(value = "0.0", inclusive = true, message = "globalCpaTargetOverride must be non-negative")
    private BigDecimal globalCpaTargetOverride;

    @DecimalMin(value = "0.0", inclusive = true, message = "minSpendThresholdOverride must be non-negative")
    private BigDecimal minSpendThresholdOverride;

    @DecimalMin(value = "0.0", inclusive = true, message = "priorityHighSpendCutoffOverride must be non-negative")
    private BigDecimal priorityHighSpendCutoffOverride;

    @DecimalMin(value = "0.0", inclusive = true, message = "priorityHighCpaMultiplierOverride must be non-negative")
    private BigDecimal priorityHighCpaMultiplierOverride;

    public AnalysisRunRequest() {
    }

    public AnalysisRunRequest(LocalDate startDate,
                              LocalDate endDate,
                              String platform,
                              BigDecimal globalCpaTargetOverride,
                              BigDecimal minSpendThresholdOverride,
                              BigDecimal priorityHighSpendCutoffOverride,
                              BigDecimal priorityHighCpaMultiplierOverride) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.platform = platform;
        this.globalCpaTargetOverride = globalCpaTargetOverride;
        this.minSpendThresholdOverride = minSpendThresholdOverride;
        this.priorityHighSpendCutoffOverride = priorityHighSpendCutoffOverride;
        this.priorityHighCpaMultiplierOverride = priorityHighCpaMultiplierOverride;
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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public BigDecimal getGlobalCpaTargetOverride() {
        return globalCpaTargetOverride;
    }

    public void setGlobalCpaTargetOverride(BigDecimal globalCpaTargetOverride) {
        this.globalCpaTargetOverride = globalCpaTargetOverride;
    }

    public BigDecimal getMinSpendThresholdOverride() {
        return minSpendThresholdOverride;
    }

    public void setMinSpendThresholdOverride(BigDecimal minSpendThresholdOverride) {
        this.minSpendThresholdOverride = minSpendThresholdOverride;
    }

    public BigDecimal getPriorityHighSpendCutoffOverride() {
        return priorityHighSpendCutoffOverride;
    }

    public void setPriorityHighSpendCutoffOverride(BigDecimal priorityHighSpendCutoffOverride) {
        this.priorityHighSpendCutoffOverride = priorityHighSpendCutoffOverride;
    }

    public BigDecimal getPriorityHighCpaMultiplierOverride() {
        return priorityHighCpaMultiplierOverride;
    }

    public void setPriorityHighCpaMultiplierOverride(BigDecimal priorityHighCpaMultiplierOverride) {
        this.priorityHighCpaMultiplierOverride = priorityHighCpaMultiplierOverride;
    }
}
