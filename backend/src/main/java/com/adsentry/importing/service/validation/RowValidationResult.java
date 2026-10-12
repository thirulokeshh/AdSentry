package com.adsentry.importing.service.validation;

import com.adsentry.common.exception.ErrorCode;
import com.adsentry.importing.domain.IdentityConfidence;
import com.adsentry.normalization.domain.ConversionDefinition;
import com.adsentry.normalization.domain.Platform;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RowValidationResult {

    private final boolean valid;
    private final int sourceRowNumber;

    // Populated if valid
    private final String campaignIdentityKey;
    private final String recordIdentityKey;
    private final Platform platform;
    private final String campaignId;
    private final String campaignName;
    private final String campaignNameNormalized;
    private final IdentityConfidence identityConfidence;
    private final LocalDate reportingDate;
    private final BigDecimal spend;
    private final String currency;
    private final Long impressions;
    private final Long clicks;
    private final BigDecimal conversions;
    private final ConversionDefinition conversionDefinition;
    private final String dimensionsJson;

    // Populated if invalid
    private final String errorField;
    private final ErrorCode errorCode;
    private final String errorMessage;

    public static RowValidationResult valid(
            int sourceRowNumber,
            String campaignIdentityKey,
            String recordIdentityKey,
            Platform platform,
            String campaignId,
            String campaignName,
            String campaignNameNormalized,
            IdentityConfidence identityConfidence,
            LocalDate reportingDate,
            BigDecimal spend,
            String currency,
            Long impressions,
            Long clicks,
            BigDecimal conversions,
            ConversionDefinition conversionDefinition,
            String dimensionsJson) {
        return new RowValidationResult(
                true, sourceRowNumber, campaignIdentityKey, recordIdentityKey, platform,
                campaignId, campaignName, campaignNameNormalized, identityConfidence,
                reportingDate, spend, currency, impressions, clicks, conversions,
                conversionDefinition, dimensionsJson, null, null, null
        );
    }

    public static RowValidationResult error(int sourceRowNumber, String errorField, ErrorCode errorCode, String errorMessage) {
        return new RowValidationResult(
                false, sourceRowNumber, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null,
                null, null, errorField, errorCode, errorMessage
        );
    }

    private RowValidationResult(
            boolean valid,
            int sourceRowNumber,
            String campaignIdentityKey,
            String recordIdentityKey,
            Platform platform,
            String campaignId,
            String campaignName,
            String campaignNameNormalized,
            IdentityConfidence identityConfidence,
            LocalDate reportingDate,
            BigDecimal spend,
            String currency,
            Long impressions,
            Long clicks,
            BigDecimal conversions,
            ConversionDefinition conversionDefinition,
            String dimensionsJson,
            String errorField,
            ErrorCode errorCode,
            String errorMessage) {
        this.valid = valid;
        this.sourceRowNumber = sourceRowNumber;
        this.campaignIdentityKey = campaignIdentityKey;
        this.recordIdentityKey = recordIdentityKey;
        this.platform = platform;
        this.campaignId = campaignId;
        this.campaignName = campaignName;
        this.campaignNameNormalized = campaignNameNormalized;
        this.identityConfidence = identityConfidence;
        this.reportingDate = reportingDate;
        this.spend = spend;
        this.currency = currency;
        this.impressions = impressions;
        this.clicks = clicks;
        this.conversions = conversions;
        this.conversionDefinition = conversionDefinition;
        this.dimensionsJson = dimensionsJson;
        this.errorField = errorField;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public boolean isValid() {
        return valid;
    }

    public int getSourceRowNumber() {
        return sourceRowNumber;
    }

    public String getCampaignIdentityKey() {
        return campaignIdentityKey;
    }

    public String getRecordIdentityKey() {
        return recordIdentityKey;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public String getCampaignNameNormalized() {
        return campaignNameNormalized;
    }

    public IdentityConfidence getIdentityConfidence() {
        return identityConfidence;
    }

    public LocalDate getReportingDate() {
        return reportingDate;
    }

    public BigDecimal getSpend() {
        return spend;
    }

    public String getCurrency() {
        return currency;
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

    public ConversionDefinition getConversionDefinition() {
        return conversionDefinition;
    }

    public String getDimensionsJson() {
        return dimensionsJson;
    }

    public String getErrorField() {
        return errorField;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
