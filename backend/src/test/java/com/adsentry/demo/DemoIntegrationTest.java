package com.adsentry.demo;

import com.adsentry.importing.repository.ImportBatchRepository;
import com.adsentry.importing.repository.ImportedRecordRepository;
import com.adsentry.recommendation.repository.RecommendationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImportBatchRepository batchRepository;

    @Autowired
    private ImportedRecordRepository recordRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Test
    @DisplayName("GET /api/demo endpoints return realistic data and keep database completely isolated")
    void testDemoEndpointsAndIsolation() throws Exception {
        // Initial database counts
        long initialBatches = batchRepository.count();
        long initialRecords = recordRepository.count();
        long initialRecs = recommendationRepository.count();

        // 1. Overview
        mockMvc.perform(get("/api/demo/overview"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.currency", is("INR")))
                .andExpect(jsonPath("$.totalCampaigns", is(5)))
                .andExpect(jsonPath("$.metrics.spend", is(148250.0)))
                .andExpect(jsonPath("$.metrics.cpa", is(429.71)))
                .andExpect(jsonPath("$.warnings", hasItem(containsString("Demo mode active"))));

        // 2. Campaigns
        mockMvc.perform(get("/api/demo/campaigns"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCampaigns", is(5)))
                .andExpect(jsonPath("$.campaigns", hasSize(5)))
                .andExpect(jsonPath("$.campaigns[?(@.platform == 'Google Ads')]", hasSize(3)))
                .andExpect(jsonPath("$.campaigns[?(@.platform == 'Meta Ads')]", hasSize(2)));

        // 3. Recommendations
        mockMvc.perform(get("/api/demo/recommendations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ruleIdentifier", is("RULE_A_ZERO_CONVERSIONS")))
                .andExpect(jsonPath("$[0].status", is("NEW")))
                .andExpect(jsonPath("$[1].ruleIdentifier", is("RULE_B_HIGH_CPA")))
                .andExpect(jsonPath("$[1].status", is("REVIEWED")));

        // Verify strict isolation: No database mutations occurred
        assertEquals(initialBatches, batchRepository.count(), "Demo calls must not write batches to DB");
        assertEquals(initialRecords, recordRepository.count(), "Demo calls must not write records to DB");
        assertEquals(initialRecs, recommendationRepository.count(), "Demo calls must not write recommendations to DB");
    }
}
