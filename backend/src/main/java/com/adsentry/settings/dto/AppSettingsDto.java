package com.adsentry.settings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public class AppSettingsDto {

    @NotNull(message = "globalCpaTarget is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "globalCpaTarget must be non-negative")
    private BigDecimal globalCpaTarget;

    @NotNull(message = "minSpendThreshold is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "minSpendThreshold must be non-negative")
    private BigDecimal minSpendThreshold;

    @DecimalMin(value = "0.0", inclusive = true, message = "priorityHighSpendCutoff must be non-negative")
    private BigDecimal priorityHighSpendCutoff;

    @DecimalMin(value = "0.0", inclusive = true, message = "priorityHighCpaMultiplier must be non-negative")
    private BigDecimal priorityHighCpaMultiplier;

    private Instant updatedAt;

    public AppSettingsDto() {
    }

    public AppSettingsDto(BigDecimal globalCpaTarget,
                          BigDecimal minSpendThreshold,
                          BigDecimal priorityHighSpendCutoff,
                          BigDecimal priorityHighCpaMultiplier,
                          Instant updatedAt) {
        this.globalCpaTarget = globalCpaTarget;
        this.minSpendThreshold = minSpendThreshold;
        this.priorityHighSpendCutoff = priorityHighSpendCutoff;
        this.priorityHighCpaMultiplier = priorityHighCpaMultiplier;
        this.updatedAt = updatedAt;
    }

    public BigDecimal getGlobalCpaTarget() {
        return globalCpaTarget;
    }

    public void setGlobalCpaTarget(BigDecimal globalCpaTarget) {
        this.globalCpaTarget = globalCpaTarget;
    }

    public BigDecimal getMinSpendThreshold() {
        return minSpendThreshold;
    }

    public void setMinSpendThreshold(BigDecimal minSpendThreshold) {
        this.minSpendThreshold = minSpendThreshold;
    }

    public BigDecimal getPriorityHighSpendCutoff() {
        return priorityHighSpendCutoff;
    }

    public void setPriorityHighSpendCutoff(BigDecimal priorityHighSpendCutoff) {
        this.priorityHighSpendCutoff = priorityHighSpendCutoff;
    }

    public BigDecimal getPriorityHighCpaMultiplier() {
        return priorityHighCpaMultiplier;
    }

    public void setPriorityHighCpaMultiplier(BigDecimal priorityHighCpaMultiplier) {
        this.priorityHighCpaMultiplier = priorityHighCpaMultiplier;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
