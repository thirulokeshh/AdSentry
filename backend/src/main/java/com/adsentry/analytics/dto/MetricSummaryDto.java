package com.adsentry.analytics.dto;

import java.math.BigDecimal;

/**
 * Aggregated metric summary.
 * Ratios are calculated centrally from total sums, not averages of row-level rates.
 * When a ratio cannot be defined (e.g. division by zero, null, or zero conversions for CPA),
 * the value is strictly NULL (never represented as ₹0).
 */
public class MetricSummaryDto {

    private BigDecimal spend;
    private Long impressions;
    private Long clicks;
    private BigDecimal conversions;
    private String conversionDefinition;

    // Computed ratios (from aggregate totals)
    private BigDecimal ctr;            // Clicks / Impressions * 100 (%)
    private BigDecimal cpc;            // Spend / Clicks (INR)
    private BigDecimal cpa;            // Spend / Compatible Conversions (INR) - NULL if conversions == 0 or unspecified
    private BigDecimal conversionRate; // Compatible Conversions / Clicks * 100 (%)
    private BigDecimal cpm;            // Spend / Impressions * 1000 (INR)

    public MetricSummaryDto() {
    }

    public MetricSummaryDto(BigDecimal spend,
                            Long impressions,
                            Long clicks,
                            BigDecimal conversions,
                            String conversionDefinition,
                            BigDecimal ctr,
                            BigDecimal cpc,
                            BigDecimal cpa,
                            BigDecimal conversionRate,
                            BigDecimal cpm) {
        this.spend = spend;
        this.impressions = impressions;
        this.clicks = clicks;
        this.conversions = conversions;
        this.conversionDefinition = conversionDefinition;
        this.ctr = ctr;
        this.cpc = cpc;
        this.cpa = cpa;
        this.conversionRate = conversionRate;
        this.cpm = cpm;
    }

    public BigDecimal getSpend() {
        return spend;
    }

    public void setSpend(BigDecimal spend) {
        this.spend = spend;
    }

    public Long getImpressions() {
        return impressions;
    }

    public void setImpressions(Long impressions) {
        this.impressions = impressions;
    }

    public Long getClicks() {
        return clicks;
    }

    public void setClicks(Long clicks) {
        this.clicks = clicks;
    }

    public BigDecimal getConversions() {
        return conversions;
    }

    public void setConversions(BigDecimal conversions) {
        this.conversions = conversions;
    }

    public String getConversionDefinition() {
        return conversionDefinition;
    }

    public void setConversionDefinition(String conversionDefinition) {
        this.conversionDefinition = conversionDefinition;
    }

    public BigDecimal getCtr() {
        return ctr;
    }

    public void setCtr(BigDecimal ctr) {
        this.ctr = ctr;
    }

    public BigDecimal getCpc() {
        return cpc;
    }

    public void setCpc(BigDecimal cpc) {
        this.cpc = cpc;
    }

    public BigDecimal getCpa() {
        return cpa;
    }

    public void setCpa(BigDecimal cpa) {
        this.cpa = cpa;
    }

    public BigDecimal getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
    }

    public BigDecimal getCpm() {
        return cpm;
    }

    public void setCpm(BigDecimal cpm) {
        this.cpm = cpm;
    }
}
