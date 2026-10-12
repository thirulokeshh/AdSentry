package com.adsentry.importing.service.validation;

import com.adsentry.common.exception.ErrorCode;
import com.adsentry.importing.domain.IdentityConfidence;
import com.adsentry.importing.service.currency.CurrencyService;
import com.adsentry.importing.service.csv.CsvRow;
import com.adsentry.importing.service.mapping.ColumnMapping;
import com.adsentry.normalization.domain.ChannelClassifier;
import com.adsentry.normalization.domain.ConversionDefinition;
import com.adsentry.normalization.domain.IdentityKeyGenerator;
import com.adsentry.normalization.domain.Platform;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Validates individual CSV rows according to AdSentry business and data integrity rules:
 * - Missing values are NOT zero (null vs explicitly reported 0)
 * - Reporting date is strictly required on every imported record
 * - Unsupported channels are rejected row-by-row with clear error reasons
 * - Negative values are rejected
 * - Campaign identity aggregates across dates; record key deduplicates by date
 */
@Component
public class RowValidator {

    private final DateParser dateParser;
    private final CurrencyService currencyService;
    private final ChannelClassifier channelClassifier;
    private final IdentityKeyGenerator identityKeyGenerator;
    private final ObjectMapper objectMapper;

    public RowValidator(DateParser dateParser,
                        CurrencyService currencyService,
                        ChannelClassifier channelClassifier,
                        IdentityKeyGenerator identityKeyGenerator,
                        ObjectMapper objectMapper) {
        this.dateParser = dateParser;
        this.currencyService = currencyService;
        this.channelClassifier = channelClassifier;
        this.identityKeyGenerator = identityKeyGenerator;
        this.objectMapper = objectMapper;
    }

    public RowValidationResult validate(CsvRow row,
                                        ColumnMapping mapping,
                                        Platform defaultPlatform,
                                        String explicitDateFormat,
                                        ConversionDefinition explicitConversionDefinition) {
        int rowNum = row.getSourceRowNumber();

        // 1. Reporting Date Validation
        String rawDate = row.get(mapping.getDateColumn());
        if (rawDate == null || rawDate.isBlank()) {
            return RowValidationResult.error(rowNum, "date", ErrorCode.INVALID_DATE,
                    "Reporting date is required for every imported record.");
        }
        DateParseResult dateResult = dateParser.parseDate(rawDate, explicitDateFormat);
        if (dateResult.isAmbiguous()) {
            return RowValidationResult.error(rowNum, "date", ErrorCode.AMBIGUOUS_DATE_FORMAT, dateResult.getErrorMessage());
        }
        if (!dateResult.isValid()) {
            return RowValidationResult.error(rowNum, "date", ErrorCode.INVALID_DATE, dateResult.getErrorMessage());
        }

        // 2. Channel / Platform Validation
        Platform rowPlatform = defaultPlatform;
        if (mapping.getChannelColumn() != null) {
            String rawChannel = row.get(mapping.getChannelColumn());
            if (rawChannel == null || rawChannel.isBlank()) {
                return RowValidationResult.error(rowNum, "channel", ErrorCode.UNSUPPORTED_CHANNEL,
                        "Channel is blank or missing. Platform cannot be established.");
            }
            if (!channelClassifier.isSupported(rawChannel)) {
                return RowValidationResult.error(rowNum, "channel", ErrorCode.UNSUPPORTED_CHANNEL,
                        "Unsupported advertising channel '" + rawChannel + "'. AdSentry MVP only supports Google Ads and Meta Ads.");
            }
            rowPlatform = channelClassifier.classify(rawChannel);
        }

        if (rowPlatform == null || rowPlatform == Platform.UNSUPPORTED) {
            return RowValidationResult.error(rowNum, "platform", ErrorCode.UNSUPPORTED_CHANNEL,
                    "Could not determine a supported advertising platform (Google Ads / Meta Ads) for row.");
        }

        // 3. Campaign Identity Validation
        String rawCampaignName = mapping.getCampaignNameColumn() != null ? row.get(mapping.getCampaignNameColumn()) : null;
        String rawCampaignId = mapping.getCampaignIdColumn() != null ? row.get(mapping.getCampaignIdColumn()) : null;

        if ((rawCampaignName == null || rawCampaignName.isBlank()) && (rawCampaignId == null || rawCampaignId.isBlank())) {
            return RowValidationResult.error(rowNum, "campaign", ErrorCode.MALFORMED_ROW,
                    "Row must have at least a campaign name or campaign ID to establish identity.");
        }

        String campaignName = (rawCampaignName != null && !rawCampaignName.isBlank())
                ? rawCampaignName.trim()
                : "Campaign-" + rawCampaignId.trim();

        String campaignNameNormalized = identityKeyGenerator.normalizeCampaignName(campaignName);
        IdentityConfidence confidence = identityKeyGenerator.determineConfidence(rawCampaignId);
        String campaignIdentityKey = identityKeyGenerator.buildCampaignIdentityKey(rowPlatform, rawCampaignId, campaignNameNormalized);

        // 4. Spend Validation
        String rawSpend = row.get(mapping.getSpendColumn());
        if (rawSpend == null || rawSpend.isBlank()) {
            return RowValidationResult.error(rowNum, "spend", ErrorCode.MISSING_REQUIRED_COLUMN,
                    "Spend value is missing. Actual spend is required.");
        }

        BigDecimal spend;
        try {
            spend = currencyService.parseMonetaryValue(rawSpend);
            if (spend == null) {
                return RowValidationResult.error(rowNum, "spend", ErrorCode.MISSING_REQUIRED_COLUMN,
                        "Spend value is missing or null.");
            }
        } catch (Exception e) {
            return RowValidationResult.error(rowNum, "spend", ErrorCode.MALFORMED_ROW,
                    "Invalid monetary number format for spend: '" + rawSpend + "'.");
        }

        if (spend.compareTo(BigDecimal.ZERO) < 0) {
            return RowValidationResult.error(rowNum, "spend", ErrorCode.NEGATIVE_VALUE,
                    "Spend cannot be negative: ₹" + spend);
        }

        // 5. Optional Metrics: Impressions, Clicks, Conversions (Missing != Zero)
        Long impressions = null;
        if (mapping.getImpressionsColumn() != null) {
            String rawImp = row.get(mapping.getImpressionsColumn());
            if (rawImp != null && !rawImp.isBlank() && !"-".equals(rawImp.trim()) && !"n/a".equalsIgnoreCase(rawImp.trim())) {
                try {
                    String clean = rawImp.replace(",", "").replace(" ", "").trim();
                    impressions = Long.parseLong(clean);
                    if (impressions < 0) {
                        return RowValidationResult.error(rowNum, "impressions", ErrorCode.NEGATIVE_VALUE,
                                "Impressions cannot be negative: " + impressions);
                    }
                } catch (NumberFormatException e) {
                    return RowValidationResult.error(rowNum, "impressions", ErrorCode.MALFORMED_ROW,
                            "Invalid integer format for impressions: '" + rawImp + "'.");
                }
            }
        }

        Long clicks = null;
        if (mapping.getClicksColumn() != null) {
            String rawClicks = row.get(mapping.getClicksColumn());
            if (rawClicks != null && !rawClicks.isBlank() && !"-".equals(rawClicks.trim()) && !"n/a".equalsIgnoreCase(rawClicks.trim())) {
                try {
                    String clean = rawClicks.replace(",", "").replace(" ", "").trim();
                    clicks = Long.parseLong(clean);
                    if (clicks < 0) {
                        return RowValidationResult.error(rowNum, "clicks", ErrorCode.NEGATIVE_VALUE,
                                "Clicks cannot be negative: " + clicks);
                    }
                } catch (NumberFormatException e) {
                    return RowValidationResult.error(rowNum, "clicks", ErrorCode.MALFORMED_ROW,
                            "Invalid integer format for clicks: '" + rawClicks + "'.");
                }
            }
        }

        BigDecimal conversions = null;
        if (mapping.getConversionsColumn() != null) {
            String rawConv = row.get(mapping.getConversionsColumn());
            if (rawConv != null && !rawConv.isBlank() && !"-".equals(rawConv.trim()) && !"n/a".equalsIgnoreCase(rawConv.trim())) {
                try {
                    String clean = rawConv.replace(",", "").replace(" ", "").trim();
                    conversions = new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
                    if (conversions.compareTo(BigDecimal.ZERO) < 0) {
                        return RowValidationResult.error(rowNum, "conversions", ErrorCode.NEGATIVE_VALUE,
                                "Conversions cannot be negative: " + conversions);
                    }
                } catch (Exception e) {
                    return RowValidationResult.error(rowNum, "conversions", ErrorCode.MALFORMED_ROW,
                            "Invalid numeric format for conversions: '" + rawConv + "'.");
                }
            }
        }

        // 6. Conversion Definition
        ConversionDefinition convDef = ConversionDefinition.UNSPECIFIED;
        if (explicitConversionDefinition != null && explicitConversionDefinition != ConversionDefinition.UNSPECIFIED) {
            convDef = explicitConversionDefinition;
        } else if (mapping.getConversionsColumn() != null) {
            convDef = ConversionDefinition.detectFromHeaderOrAction(mapping.getConversionsColumn());
        }

        // 7. Dimensions & JSON
        Map<String, String> dimensions = new HashMap<>();
        StringBuilder dimKeyBuilder = new StringBuilder();
        if (mapping.getDimensionColumns() != null) {
            for (String dimCol : mapping.getDimensionColumns()) {
                String val = row.get(dimCol);
                if (val != null && !val.isBlank()) {
                    dimensions.put(dimCol.toLowerCase(), val.trim());
                    dimKeyBuilder.append(dimCol.toLowerCase()).append("=").append(val.trim()).append(";");
                }
            }
        }

        String dimensionsJson = null;
        if (!dimensions.isEmpty()) {
            try {
                dimensionsJson = objectMapper.writeValueAsString(dimensions);
            } catch (JsonProcessingException ignored) {
            }
        }

        // 8. Record Deduplication Key (includes date and dimensions)
        String dimensionsKey = dimKeyBuilder.length() > 0 ? dimKeyBuilder.toString() : "default";
        String recordIdentityKey = identityKeyGenerator.buildRecordIdentityKey(
                campaignIdentityKey,
                dateResult.getDate(),
                dimensionsKey,
                convDef
        );

        return RowValidationResult.valid(
                rowNum,
                campaignIdentityKey,
                recordIdentityKey,
                rowPlatform,
                rawCampaignId,
                campaignName,
                campaignNameNormalized,
                confidence,
                dateResult.getDate(),
                spend,
                CurrencyService.SUPPORTED_CURRENCY,
                impressions,
                clicks,
                conversions,
                convDef,
                dimensionsJson
        );
    }
}
