package com.adsentry.recommendation;

import com.adsentry.importing.dto.ImportConfirmRequest;
import com.adsentry.importing.repository.ImportBatchRepository;
import com.adsentry.importing.repository.ImportedRecordRepository;
import com.adsentry.importing.service.ImportService;
import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RecommendationStatus;
import com.adsentry.recommendation.domain.RuleIdentifier;
import com.adsentry.recommendation.dto.AnalysisRunRequest;
import com.adsentry.recommendation.dto.UpdateRecommendationStatusRequest;
import com.adsentry.recommendation.repository.RecommendationRepository;
import com.adsentry.recommendation.service.RecommendationService;
import com.adsentry.settings.dto.AppSettingsDto;
import com.adsentry.settings.service.AppSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImportService importService;

    @Autowired
    private ImportBatchRepository batchRepository;

    @Autowired
    private ImportedRecordRepository importedRecordRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private AppSettingsService appSettingsService;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        recommendationRepository.deleteAll();
        importedRecordRepository.deleteAll();
        batchRepository.deleteAll();
        appSettingsService.updateSettings(new AppSettingsDto(
                new BigDecimal("500.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2.0"),
                null
        ));
    }

    private Long importCsv(String filename, String content, String conversionDefinition) {
        try {
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            ImportConfirmRequest request = new ImportConfirmRequest();
            request.setConfirmedCurrency("INR");
            request.setConversionDefinition(conversionDefinition != null ? conversionDefinition : "PURCHASE");
            return importService.importBatch(filename, inputStream, request).getId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Rule A triggers on high spend with zero conversions, providing evidence and potentially at-risk explanation")
    void testRuleAZeroConversions_lifecycleAndEvidence() throws Exception {
        // Spend = 3000.00 across 2 days, 0 conversions, PURCHASE definition
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Zero-Conv-Campaign,1500.00,5000,100,0
                2026-03-02,Google Ads,Zero-Conv-Campaign,1500.00,5000,100,0
                """;
        importCsv("batch_zero_conv.csv", csv, "PURCHASE");

        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ruleIdentifier").value(RuleIdentifier.RULE_A_ZERO_CONVERSIONS.name()))
                .andExpect(jsonPath("$[0].campaignName").value("Zero-Conv-Campaign"))
                .andExpect(jsonPath("$[0].priority").value(RecommendationPriority.MEDIUM.name())) // 3000 >= 2000 (2x minSpend) and < 5000
                .andExpect(jsonPath("$[0].status").value(RecommendationStatus.NEW.name()))
                .andExpect(jsonPath("$[0].explanation", containsString("potentially at-risk")))
                .andExpect(jsonPath("$[0].explanation", not(containsString("wasted spend"))))
                .andExpect(jsonPath("$[0].suggestedAction", containsString("Verify conversion tracking tag health")))
                .andExpect(jsonPath("$[0].supportingMetrics.spend").value(3000.00))
                .andExpect(jsonPath("$[0].supportingMetrics.conversions").value(0.00))
                .andExpect(jsonPath("$[0].fingerprint", notNullValue()));
    }

    @Test
    @DisplayName("Rule B triggers when CPA strictly exceeds target, documenting over-target deltas and high priority")
    void testRuleBHighCpa_lifecycleAndEvidence() throws Exception {
        // Spend = 3000.00, 2 conversions -> CPA = 1500.00 (Target = 500.00, high cutoff = 1000.00) -> HIGH priority
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Meta Ads,High-Cpa-Campaign,1500.00,10000,200,1
                2026-03-02,Meta Ads,High-Cpa-Campaign,1500.00,10000,200,1
                """;
        importCsv("batch_high_cpa.csv", csv, "PURCHASE");

        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ruleIdentifier").value(RuleIdentifier.RULE_B_HIGH_CPA.name()))
                .andExpect(jsonPath("$[0].campaignName").value("High-Cpa-Campaign"))
                .andExpect(jsonPath("$[0].priority").value(RecommendationPriority.HIGH.name()))
                .andExpect(jsonPath("$[0].status").value(RecommendationStatus.NEW.name()))
                .andExpect(jsonPath("$[0].supportingMetrics.cpa").value(1500.00))
                .andExpect(jsonPath("$[0].supportingMetrics.targetCpa").value(500.00))
                .andExpect(jsonPath("$[0].supportingMetrics.amountOverTarget").value(1000.00))
                .andExpect(jsonPath("$[0].supportingMetrics.percentOverTarget").value(200.00))
                .andExpect(jsonPath("$[0].suggestedAction", containsString("Review search terms to add negative keywords")));
    }

    @Test
    @DisplayName("Missing conversions and unspecified definitions never trigger Rule A or Rule B")
    void testMissingVsZeroConversions_andIncompatibleDefinitions() throws Exception {
        // Campaign 1: missing conversions (empty/null)
        // Campaign 2: unspecified conversion definition
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Missing-Conv-Campaign,2000.00,5000,100,
                2026-03-02,Google Ads,Missing-Conv-Campaign,2000.00,5000,100,
                """;
        importCsv("batch_missing.csv", csv, "PURCHASE");

        // Verify missing conversions does not trigger zero-conversion rule
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Unspecified conversion definition
        String unspecCsv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Unspecified-Conv-Campaign,3000.00,5000,100,0
                2026-03-02,Google Ads,Unspecified-Conv-Campaign,3000.00,5000,100,0
                """;
        importCsv("batch_unspec.csv", unspecCsv, "UNSPECIFIED");

        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("A single campaign never triggers multiple rules (spend is not double-counted)")
    void testDoubleCountingPrevention() throws Exception {
        // Zero conversions triggers Rule A only, never Rule B
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Campaign-Alpha,6000.00,10000,200,0
                2026-03-02,Google Ads,Campaign-Alpha,6000.00,10000,200,0
                """;
        importCsv("batch_alpha.csv", csv, "PURCHASE");

        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ruleIdentifier").value(RuleIdentifier.RULE_A_ZERO_CONVERSIONS.name()));
    }

    @Test
    @DisplayName("Repeated analysis runs do not duplicate recommendations and preserve REVIEWED and DISMISSED statuses")
    void testDeduplicationAndStatusPersistence() throws Exception {
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Stable-Campaign,1500.00,5000,100,0
                2026-03-02,Google Ads,Stable-Campaign,1500.00,5000,100,0
                """;
        importCsv("batch_stable.csv", csv, "PURCHASE");

        // 1. Initial run created 1 recommendation with status NEW
        assertEquals(1, recommendationRepository.count());
        Long recId = recommendationRepository.findAll().get(0).getId();
        String initialFingerprint = recommendationRepository.findAll().get(0).getFingerprint();

        // 2. Re-run analysis explicitly via POST /api/analysis/run
        mockMvc.perform(post("/api/analysis/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecommendationsGenerated").value(1));

        // Still exactly 1 recommendation in repository with identical fingerprint
        assertEquals(1, recommendationRepository.count());
        assertEquals(initialFingerprint, recommendationRepository.findAll().get(0).getFingerprint());
        assertEquals(RecommendationStatus.NEW, recommendationRepository.findAll().get(0).getStatus());

        // 3. User marks recommendation as REVIEWED
        mockMvc.perform(patch("/api/recommendations/" + recId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRecommendationStatusRequest(RecommendationStatus.REVIEWED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(RecommendationStatus.REVIEWED.name()));

        assertEquals(RecommendationStatus.REVIEWED, recommendationRepository.findById(recId).orElseThrow().getStatus());

        // 4. Re-run analysis -> status remains REVIEWED!
        mockMvc.perform(post("/api/analysis/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        assertEquals(1, recommendationRepository.count());
        assertEquals(RecommendationStatus.REVIEWED, recommendationRepository.findById(recId).orElseThrow().getStatus());

        // 5. User marks recommendation as DISMISSED
        mockMvc.perform(patch("/api/recommendations/" + recId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRecommendationStatusRequest(RecommendationStatus.DISMISSED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(RecommendationStatus.DISMISSED.name()));

        // 6. Re-run analysis -> status remains DISMISSED!
        mockMvc.perform(post("/api/analysis/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        assertEquals(RecommendationStatus.DISMISSED, recommendationRepository.findById(recId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Material change resets status to NEW from REVIEWED or DISMISSED")
    void testMaterialChangeResetsStatusToNew() throws Exception {
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Scaling-Campaign,1200.00,5000,100,0
                2026-03-02,Google Ads,Scaling-Campaign,1200.00,5000,100,0
                """;
        importCsv("batch_scaling_1.csv", csv, "PURCHASE");

        Long recId = recommendationRepository.findAll().get(0).getId();

        // Mark as REVIEWED
        mockMvc.perform(patch("/api/recommendations/" + recId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRecommendationStatusRequest(RecommendationStatus.REVIEWED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(RecommendationStatus.REVIEWED.name()));

        // Add additional batch that increases spend by > 20% (e.g. doubles spend to 4800) and escalates priority
        String csv2 = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-03,Google Ads,Scaling-Campaign,2400.00,5000,100,0
                """;
        importCsv("batch_scaling_2.csv", csv2, "PURCHASE");

        // The recalculation should reset status to NEW because underlying spend increased by 100% (>= 20%)
        assertEquals(1, recommendationRepository.count());
        assertEquals(RecommendationStatus.NEW, recommendationRepository.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("Stale recommendations are retired when the campaign rule no longer triggers")
    void testRetireStaleRecommendations() throws Exception {
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Resolving-Campaign,1500.00,5000,100,0
                2026-03-02,Google Ads,Resolving-Campaign,1500.00,5000,100,0
                """;
        Long batchId = importCsv("batch_resolving.csv", csv, "PURCHASE");

        assertEquals(1, recommendationRepository.count());

        // Deleting the batch removes all records for this campaign
        mockMvc.perform(delete("/api/imports/" + batchId))
                .andExpect(status().isNoContent());

        // Recalculation event retires the recommendation
        assertEquals(0, recommendationRepository.count());
    }

    @Test
    @DisplayName("Settings updates automatically trigger recalculation and retire findings below new threshold")
    void testSettingsUpdateTriggersRecalculation() throws Exception {
        // Spend = 1500.00 (Exceeds default 1000.00 threshold)
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Borderline-Campaign,750.00,5000,100,0
                2026-03-02,Google Ads,Borderline-Campaign,750.00,5000,100,0
                """;
        importCsv("batch_borderline.csv", csv, "PURCHASE");

        assertEquals(1, recommendationRepository.count());

        // Update settings: raise minSpendThreshold to 2000.00 (above 1500.00)
        AppSettingsDto updateDto = new AppSettingsDto(
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2.0"),
                null
        );

        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        // The event listener recalculates latest analysis, finding no campaign with spend > 2000.00, retiring recommendation
        assertEquals(0, recommendationRepository.count());
    }

    @Test
    @DisplayName("Recommendations query filtering by platform, status, priority, and rule")
    void testQueryFiltering() throws Exception {
        // Create 2 campaigns: Rule A (Google Ads) and Rule B (Meta Ads)
        String csv = """
                date,channel,campaign,spend,impressions,clicks,conversions
                2026-03-01,Google Ads,Camp-Zero,1500.00,5000,100,0
                2026-03-02,Google Ads,Camp-Zero,1500.00,5000,100,0
                2026-03-01,Meta Ads,Camp-Cpa,3000.00,10000,200,2
                2026-03-02,Meta Ads,Camp-Cpa,3000.00,10000,200,2
                """;
        importCsv("batch_filter.csv", csv, "PURCHASE");

        assertEquals(2, recommendationRepository.count());

        // Filter by platform
        mockMvc.perform(get("/api/recommendations").param("platform", "Google Ads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].campaignName").value("Camp-Zero"));

        // Filter by rule
        mockMvc.perform(get("/api/recommendations").param("rule", RuleIdentifier.RULE_B_HIGH_CPA.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].campaignName").value("Camp-Cpa"));

        // Filter by priority
        mockMvc.perform(get("/api/recommendations").param("priority", RecommendationPriority.HIGH.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].campaignName").value("Camp-Cpa"));
    }

    @Test
    @DisplayName("Validation and error handling for recommendation endpoints")
    void testValidationAndErrors() throws Exception {
        // Non-existent ID returns 404
        mockMvc.perform(patch("/api/recommendations/999999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRecommendationStatusRequest(RecommendationStatus.REVIEWED))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_NOT_FOUND"));

        // Null status returns 400
        mockMvc.perform(patch("/api/recommendations/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // Negative override returns 400
        AnalysisRunRequest badRequest = new AnalysisRunRequest();
        badRequest.setGlobalCpaTargetOverride(new BigDecimal("-100.00"));

        mockMvc.perform(post("/api/analysis/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
