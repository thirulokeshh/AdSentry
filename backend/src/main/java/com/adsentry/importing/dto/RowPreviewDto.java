package com.adsentry.importing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RowPreviewDto {

    private int sourceRowNumber;
    private String platform;
    private String campaignName;
    private String campaignIdentityKey;
    private String recordIdentityKey;
    private LocalDate reportingDate;
    private BigDecimal spend;
    private Long impressions;
    private Long clicks;
    private BigDecimal conversions;
    private String conversionDefinition;

    public RowPreviewDto() {
    }

    public RowPreviewDto(int sourceRowNumber,
                         String platform,
                         String campaignName,
                         String campaignIdentityKey,
                         String recordIdentityKey,
                         LocalDate reportingDate,
                         BigDecimal spend,
                         Long impressions,
                         Long clicks,
                         BigDecimal conversions,
                         String conversionDefinition) {
        this.sourceRowNumber = sourceRowNumber;
        this.platform = platform;
        this.campaignName = campaignName;
        this.campaignIdentityKey = campaignIdentityKey;
        this.recordIdentityKey = recordIdentityKey;
        this.reportingDate = reportingDate;
        this.spend = spend;
        this.impressions = impressions;
        this.clicks = clicks;
        this.conversions = conversions;
        this.conversionDefinition = conversionDefinition;
    }

    public int getSourceRowNumber() {
        return sourceRowNumber;
    }

    public String getPlatform() {
        return platform;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public String getCampaignIdentityKey() {
        return campaignIdentityKey;
    }

    public String getRecordIdentityKey() {
        return recordIdentityKey;
    }

    public LocalDate getReportingDate() {
        return reportingDate;
    }

    public BigDecimal getSpend() {
        return spend;
    }

    public Long getImpressions() {
        return impressions;
    }

    public Long getClicks() {
        return clicks;
    }

    public BigDecimal getConversions() {
        return conversions;
    }

    public String getConversionDefinition() {
        return conversionDefinition;
    }
}
