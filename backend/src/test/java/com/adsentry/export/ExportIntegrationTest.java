package com.adsentry.export;

import com.adsentry.importing.dto.ImportConfirmRequest;
import com.adsentry.importing.repository.ImportBatchRepository;
import com.adsentry.importing.repository.ImportedRecordRepository;
import com.adsentry.importing.service.ImportService;
import com.adsentry.recommendation.repository.RecommendationRepository;
import com.adsentry.recommendation.service.RecommendationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImportService importService;

    @Autowired
    private ImportBatchRepository batchRepository;

    @Autowired
    private ImportedRecordRepository recordRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationService recommendationService;

    @BeforeEach
    @AfterEach
    void cleanup() {
        recommendationRepository.deleteAll();
        recordRepository.deleteAll();
        batchRepository.deleteAll();
    }

    private void importSampleData() throws java.io.IOException {
        String csv = """
                Campaign,Day,Cost,Impressions,Clicks,Conversions
                =DangerousCampaign,2026-03-01,1500.00,1000,50,0
                NormalCampaign,2026-03-01,800.00,600,30,5
                """;

        ImportConfirmRequest request = new ImportConfirmRequest();
        request.setConfirmedPlatform("Google Ads");
        request.setConfirmedCurrency("INR");
        request.setDateFormat("yyyy-MM-dd");
        request.setConversionDefinition("PURCHASE");

        importService.importBatch("export_test.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), request);
    }

    @Test
    @DisplayName("GET /api/exports/campaigns.csv returns sanitized RFC-4180 CSV with blank null metrics")
    void testExportCampaignsCsv() throws Exception {
        importSampleData();

        MvcResult result = mockMvc.perform(get("/api/exports/campaigns.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"campaigns_export.csv\""))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Check header row
        assertTrue(body.contains("Campaign Identity Key,Campaign Name,Platform,Active Days,Spend (INR)"));

        // Check formula injection mitigation on =DangerousCampaign
        assertTrue(body.contains("'=DangerousCampaign"), "Formula injection should be mitigated with single-quote prefix");

        // Normal campaign should be present
        assertTrue(body.contains("NormalCampaign"));

        // DangerousCampaign had 0 conversions so CPA must be blank, not 0
        String[] lines = body.split("\r\n");
        boolean foundDangerous = false;
        for (String line : lines) {
            if (line.contains("DangerousCampaign")) {
                foundDangerous = true;
                // Format: key,name,platform,activeDays,spend,impressions,clicks,ctr,cpc,conversions,cpa,convRate,...
                // Conversions is 0.00, CPA is empty between commas: ",0.00,,0.00,"
                assertTrue(line.contains(",0.00,,0.00,"), "Zero conversions should leave CPA blank: " + line);
            }
        }
        assertTrue(foundDangerous);
    }

    @Test
    @DisplayName("GET /api/exports/recommendations.csv returns sanitized recommendations CSV")
    void testExportRecommendationsCsv() throws Exception {
        importSampleData();

        MvcResult result = mockMvc.perform(get("/api/exports/recommendations.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"recommendations_export.csv\""))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Check header row
        assertTrue(body.contains("ID,Fingerprint,Rule,Rule Description,Platform,Campaign Name,Period Start,Period End,Priority,Status,Title,Explanation,Suggested Action,Trigger Condition,Created At"));

        // Recommendation was automatically generated for =DangerousCampaign (Spend 1500 > 1000 with 0 conv)
        assertTrue(body.contains("RULE_A_ZERO_CONVERSIONS"));
        assertTrue(body.contains("'=DangerousCampaign"));
        assertTrue(body.contains("NEW"));
    }
}
