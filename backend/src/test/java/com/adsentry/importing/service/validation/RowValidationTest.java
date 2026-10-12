package com.adsentry.importing.service.validation;

import com.adsentry.common.exception.ErrorCode;
import com.adsentry.importing.service.currency.CurrencyService;
import com.adsentry.importing.service.csv.CsvRow;
import com.adsentry.importing.service.mapping.ColumnMapping;
import com.adsentry.normalization.domain.ChannelClassifier;
import com.adsentry.normalization.domain.ConversionDefinition;
import com.adsentry.normalization.domain.IdentityKeyGenerator;
import com.adsentry.normalization.domain.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RowValidationTest {

    private RowValidator validator;
    private ColumnMapping standardMapping;

    @BeforeEach
    void setUp() {
        validator = new RowValidator(
                new DateParser(),
                new CurrencyService(),
                new ChannelClassifier(),
                new IdentityKeyGenerator(),
                new ObjectMapper()
        );

        standardMapping = new ColumnMapping();
        standardMapping.setDateColumn("date");
        standardMapping.setChannelColumn("channel");
        standardMapping.setCampaignNameColumn("campaign");
        standardMapping.setSpendColumn("spend");
        standardMapping.setImpressionsColumn("impressions");
        standardMapping.setClicksColumn("clicks");
        standardMapping.setConversionsColumn("conversions");
    }

    @Test
    @DisplayName("Unsupported channels are rejected with UNSUPPORTED_CHANNEL row error")
    void validateRow_unsupportedChannelsRejected() {
        for (String unsupportedChannel : List.of("Organic", "Email", "TikTok", "LinkedIn", "Affiliate")) {
            CsvRow row = new CsvRow(2, List.of("2026-03-01", unsupportedChannel, "Brand-1", "100", "10", "500.00", "2"),
                    Map.of("date", "2026-03-01", "channel", unsupportedChannel, "campaign", "Brand-1",
                            "impressions", "100", "clicks", "10", "spend", "500.00", "conversions", "2"));

            RowValidationResult result = validator.validate(row, standardMapping, Platform.GOOGLE_ADS, null, null);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_CHANNEL);
            assertThat(result.getErrorMessage()).containsIgnoringCase(unsupportedChannel);
        }
    }

    @Test
    @DisplayName("Supported channels (Google Ads and Meta Ads) are accepted")
    void validateRow_supportedChannelsAccepted() {
        for (String supportedChannel : List.of("Google Ads", "Meta Ads", "facebook ads", "google")) {
            CsvRow row = new CsvRow(2, List.of("2026-03-01", supportedChannel, "Brand-1", "100", "10", "500.00", "2"),
                    Map.of("date", "2026-03-01", "channel", supportedChannel, "campaign", "Brand-1",
                            "impressions", "100", "clicks", "10", "spend", "500.00", "conversions", "2"));

            RowValidationResult result = validator.validate(row, standardMapping, Platform.GOOGLE_ADS, null, null);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getSpend()).isEqualByComparingTo("500.00");
        }
    }

    @Test
    @DisplayName("Missing metrics are preserved as null, and explicit zeros are preserved as zero")
    void validateRow_missingVersusZeroMetrics() {
        // Row with missing metrics ("-" or blank)
        CsvRow rowMissing = new CsvRow(2, List.of("2026-03-01", "Google Ads", "Brand-1", "-", "", "500.00", "N/A"),
                Map.of("date", "2026-03-01", "channel", "Google Ads", "campaign", "Brand-1",
                        "impressions", "-", "clicks", "", "spend", "500.00", "conversions", "N/A"));

        RowValidationResult resMissing = validator.validate(rowMissing, standardMapping, Platform.GOOGLE_ADS, null, null);
        assertThat(resMissing.isValid()).isTrue();
        assertThat(resMissing.getImpressions()).isNull();
        assertThat(resMissing.getClicks()).isNull();
        assertThat(resMissing.getConversions()).isNull();

        // Row with explicit zero metrics
        CsvRow rowZero = new CsvRow(3, List.of("2026-03-01", "Google Ads", "Brand-1", "0", "0", "500.00", "0"),
                Map.of("date", "2026-03-01", "channel", "Google Ads", "campaign", "Brand-1",
                        "impressions", "0", "clicks", "0", "spend", "500.00", "conversions", "0"));

        RowValidationResult resZero = validator.validate(rowZero, standardMapping, Platform.GOOGLE_ADS, null, null);
        assertThat(resZero.isValid()).isTrue();
        assertThat(resZero.getImpressions()).isEqualTo(0L);
        assertThat(resZero.getClicks()).isEqualTo(0L);
        assertThat(resZero.getConversions()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Ambiguous conversion definitions are marked UNSPECIFIED unless confirmed or action-named")
    void validateRow_conversionDefinitions() {
        // Standard conversions header -> UNSPECIFIED
        CsvRow row = new CsvRow(2, List.of("2026-03-01", "Google Ads", "Brand-1", "500.00", "5"),
                Map.of("date", "2026-03-01", "channel", "Google Ads", "campaign", "Brand-1",
                        "spend", "500.00", "conversions", "5"));

        RowValidationResult res1 = validator.validate(row, standardMapping, Platform.GOOGLE_ADS, null, null);
        assertThat(res1.isValid()).isTrue();
        assertThat(res1.getConversionDefinition()).isEqualTo(ConversionDefinition.UNSPECIFIED);

        // Explicit column named "purchases"
        ColumnMapping purchaseMapping = new ColumnMapping();
        purchaseMapping.setDateColumn("date");
        purchaseMapping.setChannelColumn("channel");
        purchaseMapping.setCampaignNameColumn("campaign");
        purchaseMapping.setSpendColumn("spend");
        purchaseMapping.setConversionsColumn("purchases");

        CsvRow rowPurchase = new CsvRow(2, List.of("2026-03-01", "Google Ads", "Brand-1", "500.00", "5"),
                Map.of("date", "2026-03-01", "channel", "Google Ads", "campaign", "Brand-1",
                        "spend", "500.00", "purchases", "5"));

        RowValidationResult resPurchase = validator.validate(rowPurchase, purchaseMapping, Platform.GOOGLE_ADS, null, null);
        assertThat(resPurchase.isValid()).isTrue();
        assertThat(resPurchase.getConversionDefinition()).isEqualTo(ConversionDefinition.PURCHASE);
    }

    @Test
    @DisplayName("Ambiguous date format (03/04/2026) is rejected without explicit format specification")
    void validateRow_ambiguousDateRejected() {
        CsvRow row = new CsvRow(2, List.of("03/04/2026", "Google Ads", "Brand-1", "500.00"),
                Map.of("date", "03/04/2026", "channel", "Google Ads", "campaign", "Brand-1", "spend", "500.00"));

        RowValidationResult result = validator.validate(row, standardMapping, Platform.GOOGLE_ADS, null, null);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.AMBIGUOUS_DATE_FORMAT);

        // When explicit format is provided
        RowValidationResult resolvedResult = validator.validate(row, standardMapping, Platform.GOOGLE_ADS, "dd/MM/yyyy", null);
        assertThat(resolvedResult.isValid()).isTrue();
        assertThat(resolvedResult.getReportingDate().getDayOfMonth()).isEqualTo(3);
        assertThat(resolvedResult.getReportingDate().getMonthValue()).isEqualTo(4);
    }

    @Test
    @DisplayName("Negative metric values and malformed numbers are rejected")
    void validateRow_negativeAndMalformedValuesRejected() {
        CsvRow negativeSpend = new CsvRow(2, List.of("2026-03-01", "Google Ads", "Brand-1", "-500.00"),
                Map.of("date", "2026-03-01", "channel", "Google Ads", "campaign", "Brand-1", "spend", "-500.00"));

        RowValidationResult resNeg = validator.validate(negativeSpend, standardMapping, Platform.GOOGLE_ADS, null, null);
        assertThat(resNeg.isValid()).isFalse();
        assertThat(resNeg.getErrorCode()).isEqualTo(ErrorCode.NEGATIVE_VALUE);

        CsvRow malformedSpend = new CsvRow(3, List.of("2026-03-01", "Google Ads", "Brand-1", "not_a_number"),
                Map.of("date", "2026-03-01", "channel", "Google Ads", "campaign", "Brand-1", "spend", "not_a_number"));

        RowValidationResult resMalformed = validator.validate(malformedSpend, standardMapping, Platform.GOOGLE_ADS, null, null);
        assertThat(resMalformed.isValid()).isFalse();
        assertThat(resMalformed.getErrorCode()).isEqualTo(ErrorCode.MALFORMED_ROW);
    }
}
