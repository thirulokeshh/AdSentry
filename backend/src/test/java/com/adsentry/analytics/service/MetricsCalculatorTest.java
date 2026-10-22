package com.adsentry.analytics.service;

import com.adsentry.analytics.dto.MetricsCalculationResult;
import com.adsentry.importing.domain.EffectiveRecord;
import com.adsentry.normalization.domain.ConversionDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsCalculatorTest {

    private MetricsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MetricsCalculator();
    }

    private EffectiveRecord createRecord(BigDecimal spend, Long impressions, Long clicks,
                                         BigDecimal conversions, ConversionDefinition def) {
        EffectiveRecord record = new EffectiveRecord();
        ReflectionTestUtils.setField(record, "id", 1L);
        ReflectionTestUtils.setField(record, "spend", spend);
        ReflectionTestUtils.setField(record, "impressions", impressions);
        ReflectionTestUtils.setField(record, "clicks", clicks);
        ReflectionTestUtils.setField(record, "conversions", conversions);
        ReflectionTestUtils.setField(record, "conversionDefinition", def != null ? def : ConversionDefinition.UNSPECIFIED);
        ReflectionTestUtils.setField(record, "reportingDate", LocalDate.of(2026, 3, 1));
        return record;
    }

    @Test
    @DisplayName("Empty record list produces explicit null/zero structure without errors")
    void testEmptyRecords() {
        MetricsCalculationResult result = calculator.calculate(Collections.emptyList());

        assertThat(result.getMetrics()).isNotNull();
        assertThat(result.getMetrics().getSpend()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getMetrics().getImpressions()).isEqualTo(0L);
        assertThat(result.getMetrics().getClicks()).isEqualTo(0L);
        assertThat(result.getMetrics().getConversions()).isNull();
        assertThat(result.getMetrics().getCpa()).isNull();
        assertThat(result.getMetrics().getCtr()).isNull();
        assertThat(result.getWarnings()).contains("No records available for the selected criteria.");
    }

    @Test
    @DisplayName("Aggregate formulas calculate from total sums, not averages of row-level ratios")
    void testAggregateFormulas() {
        // Record 1: Spend 100, Impr 1000, Clicks 10, Conv 2 -> row CTR = 1%, row CPC = 10, row CPA = 50
        // Record 2: Spend 900, Impr 9000, Clicks 190, Conv 18 -> row CTR = 2.11%, row CPC = 4.74, row CPA = 50
        // Total Spend: 1000, Total Impr: 10000, Total Clicks: 200, Total Conv: 20
        // Aggregate CTR: 200 / 10000 * 100 = 2.00%
        // Aggregate CPC: 1000 / 200 = 5.00 INR
        // Aggregate CPA: 1000 / 20 = 50.00 INR
        // Aggregate CVR: 20 / 200 * 100 = 10.00%
        // Aggregate CPM: 1000 / 10000 * 1000 = 100.00 INR
        EffectiveRecord r1 = createRecord(new BigDecimal("100.00"), 1000L, 10L, new BigDecimal("2.00"), ConversionDefinition.PURCHASE);
        EffectiveRecord r2 = createRecord(new BigDecimal("900.00"), 9000L, 190L, new BigDecimal("18.00"), ConversionDefinition.PURCHASE);

        MetricsCalculationResult result = calculator.calculate(List.of(r1, r2));

        assertThat(result.getMetrics().getSpend()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getMetrics().getImpressions()).isEqualTo(10000L);
        assertThat(result.getMetrics().getClicks()).isEqualTo(200L);
        assertThat(result.getMetrics().getConversions()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.getMetrics().getConversionDefinition()).isEqualTo("PURCHASE");

        assertThat(result.getMetrics().getCtr()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(result.getMetrics().getCpc()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(result.getMetrics().getCpa()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.getMetrics().getConversionRate()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(result.getMetrics().getCpm()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Zero conversions: CPA is strictly NULL (never zero or ₹0)")
    void testZeroConversionsProducesNullCpa() {
        EffectiveRecord r1 = createRecord(new BigDecimal("5000.00"), 50000L, 500L, BigDecimal.ZERO, ConversionDefinition.PURCHASE);

        MetricsCalculationResult result = calculator.calculate(List.of(r1));

        assertThat(result.getMetrics().getSpend()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.getMetrics().getConversions()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getMetrics().getCpa()).as("CPA must be null when conversions are zero").isNull();
        assertThat(result.getMetrics().getConversionRate()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("Zero clicks: CPC and Conversion Rate are null, CTR is zero")
    void testZeroClicks() {
        EffectiveRecord r = createRecord(new BigDecimal("200.00"), 1000L, 0L, BigDecimal.ZERO, ConversionDefinition.PURCHASE);

        MetricsCalculationResult result = calculator.calculate(List.of(r));

        assertThat(result.getMetrics().getCtr()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(result.getMetrics().getCpc()).isNull();
        assertThat(result.getMetrics().getCpa()).isNull();
        assertThat(result.getMetrics().getConversionRate()).isNull();
    }

    @Test
    @DisplayName("Zero impressions: CTR and CPM are null")
    void testZeroImpressions() {
        EffectiveRecord r = createRecord(new BigDecimal("50.00"), 0L, 0L, null, null);

        MetricsCalculationResult result = calculator.calculate(List.of(r));

        assertThat(result.getMetrics().getCtr()).isNull();
        assertThat(result.getMetrics().getCpm()).isNull();
    }

    @Test
    @DisplayName("Null versus zero distinction: missing impressions and clicks remain null")
    void testNullVsZeroDistinction() {
        EffectiveRecord r = createRecord(new BigDecimal("300.00"), null, null, null, null);

        MetricsCalculationResult result = calculator.calculate(List.of(r));

        assertThat(result.getMetrics().getImpressions()).isNull();
        assertThat(result.getMetrics().getClicks()).isNull();
        assertThat(result.getMetrics().getConversions()).isNull();
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Impressions were not reported"));
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Clicks were not reported"));
    }

    @Test
    @DisplayName("Generic/Unspecified conversions are excluded from CPA with explanatory warning")
    void testUnspecifiedConversionsExcludedFromCpa() {
        EffectiveRecord r = createRecord(new BigDecimal("1500.00"), 5000L, 100L, new BigDecimal("15.00"), ConversionDefinition.UNSPECIFIED);

        MetricsCalculationResult result = calculator.calculate(List.of(r));

        assertThat(result.getMetrics().getConversionDefinition()).isEqualTo("UNSPECIFIED");
        assertThat(result.getMetrics().getConversions()).isNull();
        assertThat(result.getMetrics().getCpa()).isNull();
        assertThat(result.getMetrics().getConversionRate()).isNull();
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("generic/unspecified"));
    }

    @Test
    @DisplayName("Heterogeneous conversion definitions (Leads + Purchases) are not combined; CPA is null")
    void testDoNotCombineLeadsAndPurchases() {
        EffectiveRecord purchaseRecord = createRecord(new BigDecimal("1000.00"), 4000L, 80L, new BigDecimal("5.00"), ConversionDefinition.PURCHASE);
        EffectiveRecord leadRecord = createRecord(new BigDecimal("1000.00"), 6000L, 120L, new BigDecimal("20.00"), ConversionDefinition.LEAD);

        MetricsCalculationResult result = calculator.calculate(List.of(purchaseRecord, leadRecord));

        assertThat(result.getMetrics().getConversionDefinition()).isEqualTo("MIXED");
        assertThat(result.getMetrics().getConversions()).isNull();
        assertThat(result.getMetrics().getCpa()).isNull();
        assertThat(result.getMetrics().getConversionRate()).isNull();
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Leads and purchases cannot be combined"));
    }
}
