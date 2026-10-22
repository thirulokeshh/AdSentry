package com.adsentry.analytics.dto;

import java.math.BigDecimal;

public class MetricDeltaDto {

    private BigDecimal absoluteChange;
    private BigDecimal percentageChange; // null when previous metric is null or zero

    public MetricDeltaDto() {
    }

    public MetricDeltaDto(BigDecimal absoluteChange, BigDecimal percentageChange) {
        this.absoluteChange = absoluteChange;
        this.percentageChange = percentageChange;
    }

    public BigDecimal getAbsoluteChange() {
        return absoluteChange;
    }

    public void setAbsoluteChange(BigDecimal absoluteChange) {
        this.absoluteChange = absoluteChange;
    }

    public BigDecimal getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(BigDecimal percentageChange) {
        this.percentageChange = percentageChange;
    }
}
