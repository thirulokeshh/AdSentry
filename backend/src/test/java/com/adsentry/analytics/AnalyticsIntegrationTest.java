package com.adsentry.analytics;

import com.adsentry.importing.dto.ImportConfirmRequest;
import com.adsentry.importing.repository.ImportBatchRepository;
import com.adsentry.importing.repository.ImportedRecordRepository;
import com.adsentry.importing.service.ImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImportService importService;

    @Autowired
    private ImportBatchRepository batchRepository;

    @Autowired
    private ImportedRecordRepository importedRecordRepository;

    @BeforeEach
    void cleanDatabase() {
        importedRecordRepository.deleteAll();
        batchRepository.deleteAll();
    }

    private void importCsv(String filename, String content) {
        try {
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            ImportConfirmRequest request = new ImportConfirmRequest();
            request.setConfirmedCurrency("INR");
            request.setConversionDefinition("PURCHASE");
            importService.importBatch(filename, inputStream, request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Empty dataset returns 200 OK with clean null/zero metrics and coverage warning")
    void testEmptyDataset() throws Exception {
        mockMvc.perform(get("/api/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.totalCampaigns").value(0))
                .andExpect(jsonPath("$.totalRecords").value(0))
                .andExpect(jsonPath("$.metrics.spend").value(0.00))
                .andExpect(jsonPath("$.metrics.cpa").value(nullValue()))
                .andExpect(jsonPath("$.warnings", hasItem(containsString("No records available"))));

        mockMvc.perform(get("/api/analytics/coverage")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalendarDays").value(0))
                .andExpect(jsonPath("$.activeDays").value(0))
                .andExpect(jsonPath("$.sufficientCoverage").value(false));
    }

    @Test
    @DisplayName("Overview & Campaign APIs calculate metrics from aggregate totals across dates")
    void testOverviewAndCampaignAggregation() throws Exception {
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Brand-Alpha,1000.00,10000,200,20
                2026-03-02,Google Ads,Brand-Alpha,1000.00,10000,200,20
                2026-03-02,Meta Ads,Retargeting-Beta,500.00,5000,50,5
                """;
        importCsv("batch1.csv", csv);

        // Test Overview endpoint
        mockMvc.perform(get("/api/analytics/overview")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.endDate").value("2026-03-02"))
                .andExpect(jsonPath("$.totalCampaigns").value(2))
                .andExpect(jsonPath("$.totalRecords").value(3))
                .andExpect(jsonPath("$.metrics.spend").value(2500.00))
                .andExpect(jsonPath("$.metrics.impressions").value(25000))
                .andExpect(jsonPath("$.metrics.clicks").value(450))
                .andExpect(jsonPath("$.metrics.conversions").value(45.00))
                .andExpect(jsonPath("$.metrics.ctr").value(1.80))
                .andExpect(jsonPath("$.metrics.cpc").value(5.56))
                .andExpect(jsonPath("$.metrics.cpa").value(55.56))
                .andExpect(jsonPath("$.metrics.conversionRate").value(10.00))
                .andExpect(jsonPath("$.metrics.cpm").value(100.00));

        // Test Campaign Performance endpoint
        mockMvc.perform(get("/api/analytics/campaigns")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCampaigns").value(2))
                .andExpect(jsonPath("$.campaigns[0].campaignName").value("Brand-Alpha"))
                .andExpect(jsonPath("$.campaigns[0].activeDays").value(2))
                .andExpect(jsonPath("$.campaigns[0].metrics.spend").value(2000.00))
                .andExpect(jsonPath("$.campaigns[0].metrics.impressions").value(20000))
                .andExpect(jsonPath("$.campaigns[0].metrics.clicks").value(400))
                .andExpect(jsonPath("$.campaigns[0].metrics.conversions").value(40.00))
                .andExpect(jsonPath("$.campaigns[0].metrics.cpa").value(50.00));

        // Test Platform filter on Overview
        mockMvc.perform(get("/api/analytics/overview")
                        .param("platform", "Meta Ads")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCampaigns").value(1))
                .andExpect(jsonPath("$.metrics.spend").value(500.00))
                .andExpect(jsonPath("$.metrics.clicks").value(50));
    }

    @Test
    @DisplayName("Overlapping imports replace older version without double-counting in analytics")
    void testOverlappingImportDeduplication() throws Exception {
        // Initial import
        String csv1 = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Brand-Alpha,1000.00,10000,200,20
                2026-03-02,Google Ads,Brand-Alpha,1000.00,10000,200,20
                """;
        importCsv("batch1.csv", csv1);

        // Updated import for 2026-03-01
        String csv2 = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Brand-Alpha,1200.00,12000,240,24
                """;
        importCsv("batch2.csv", csv2);

        // Analytics should see winning version for 2026-03-01 (1200) + 2026-03-02 (1000) = 2200 total spend
        // NEVER double count 1000 + 1200 + 1000 = 3200
        mockMvc.perform(get("/api/analytics/overview")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCampaigns").value(1))
                .andExpect(jsonPath("$.totalRecords").value(2))
                .andExpect(jsonPath("$.metrics.spend").value(2200.00))
                .andExpect(jsonPath("$.metrics.impressions").value(22000))
                .andExpect(jsonPath("$.metrics.clicks").value(440))
                .andExpect(jsonPath("$.metrics.conversions").value(44.00))
                .andExpect(jsonPath("$.metrics.cpa").value(50.00));
    }

    @Test
    @DisplayName("Data coverage honestly reports missing calendar days and flags insufficient coverage")
    void testDataCoverageReporting() throws Exception {
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Brand-Alpha,1000.00,10000,200,20
                2026-03-02,Google Ads,Brand-Alpha,1000.00,10000,200,20
                """;
        importCsv("batch1.csv", csv);

        // 5-day span with only 2 active days (40% coverage)
        mockMvc.perform(get("/api/analytics/coverage")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalendarDays").value(5))
                .andExpect(jsonPath("$.activeDays").value(2))
                .andExpect(jsonPath("$.missingDays").value(3))
                .andExpect(jsonPath("$.coveragePercentage").value(40.00))
                .andExpect(jsonPath("$.sufficientCoverage").value(false))
                .andExpect(jsonPath("$.warnings", hasItem(containsString("3 of 5 calendar days had no recorded activity"))));
    }

    @Test
    @DisplayName("Period comparison checks equal-length adjacent periods and flags missing previous data")
    void testAdjacentPeriodComparison() throws Exception {
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-05,Google Ads,Brand-Alpha,1000.00,10000,200,20
                2026-03-06,Google Ads,Brand-Alpha,1000.00,10000,200,20
                """;
        importCsv("batch1.csv", csv);

        // Current period: 2026-03-05 to 2026-03-06 (2 days)
        // Previous adjacent period: 2026-03-03 to 2026-03-04 (2 days)
        mockMvc.perform(get("/api/analytics/comparison")
                        .param("startDate", "2026-03-05")
                        .param("endDate", "2026-03-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStartDate").value("2026-03-05"))
                .andExpect(jsonPath("$.currentEndDate").value("2026-03-06"))
                .andExpect(jsonPath("$.currentTotalDays").value(2))
                .andExpect(jsonPath("$.currentActiveDays").value(2))
                .andExpect(jsonPath("$.previousStartDate").value("2026-03-03"))
                .andExpect(jsonPath("$.previousEndDate").value("2026-03-04"))
                .andExpect(jsonPath("$.previousTotalDays").value(2))
                .andExpect(jsonPath("$.previousActiveDays").value(0))
                .andExpect(jsonPath("$.comparisonReliable").value(false))
                .andExpect(jsonPath("$.warnings", hasItem(containsString("has no recorded data"))));
    }

    @Test
    @DisplayName("Invalid date range returns 400 Bad Request")
    void testInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/analytics/overview")
                        .param("startDate", "2026-03-10")
                        .param("endDate", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail", containsString("startDate cannot be after endDate")));
    }
}
