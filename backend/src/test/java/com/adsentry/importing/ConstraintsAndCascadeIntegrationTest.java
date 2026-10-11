package com.adsentry.importing;

import com.adsentry.importing.domain.*;
import com.adsentry.importing.repository.ImportBatchRepository;
import com.adsentry.importing.repository.ImportRowErrorRepository;
import com.adsentry.importing.repository.ImportedRecordRepository;
import com.adsentry.normalization.domain.ConversionDefinition;
import com.adsentry.recommendation.domain.Recommendation;
import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RecommendationStatus;
import com.adsentry.recommendation.domain.RuleIdentifier;
import com.adsentry.recommendation.repository.RecommendationRepository;
import com.adsentry.settings.domain.AppSetting;
import com.adsentry.settings.repository.AppSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ConstraintsAndCascadeIntegrationTest {

    @Autowired
    private ImportBatchRepository batchRepository;

    @Autowired
    private ImportedRecordRepository recordRepository;

    @Autowired
    private ImportRowErrorRepository errorRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private AppSettingRepository settingRepository;

    @BeforeEach
    void cleanUp() {
        recommendationRepository.deleteAll();
        recordRepository.deleteAll();
        errorRepository.deleteAll();
        batchRepository.deleteAll();
    }

    @Test
    @DisplayName("Deleting an ImportBatch must cascade delete its records and row errors")
    void deleteBatch_shouldCascadeDeleteRecordsAndErrors() {
        ImportBatch batch = new ImportBatch("Meta Ads", "meta_may.csv", Instant.now(), "INR", BatchStatus.COMPLETED);
        batch = batchRepository.save(batch);

        ImportedRecord record = new ImportedRecord();
        record.setBatch(batch);
        record.setIdentityKey("META_ADS|campaign-1|2026-03-01|default|UNSPECIFIED");
        record.setPlatform("META_ADS");
        record.setCampaignName("Campaign-1");
        record.setCampaignNameNormalized("campaign-1");
        record.setIdentityConfidence(IdentityConfidence.LOW_NAME);
        record.setReportingDate(LocalDate.of(2026, 3, 1));
        record.setSpend(new BigDecimal("500.00"));
        record.setCurrency("INR");
        record.setConversionDefinition(ConversionDefinition.UNSPECIFIED);
        record.setSourceRowNumber(2);
        record.setImportedAt(Instant.now());
        recordRepository.save(record);

        ImportRowError error = new ImportRowError(batch, 5, "spend", "INVALID_NUMBER", "Spend cannot be negative");
        errorRepository.save(error);

        assertThat(recordRepository.findByBatchId(batch.getId())).hasSize(1);
        assertThat(errorRepository.findByBatchIdOrderBySourceRowNumberAsc(batch.getId())).hasSize(1);

        // Delete the batch
        batchRepository.delete(batch);

        // Verification: Cascaded deletion
        assertThat(recordRepository.findByBatchId(batch.getId())).isEmpty();
        assertThat(errorRepository.findByBatchIdOrderBySourceRowNumberAsc(batch.getId())).isEmpty();
    }

    @Test
    @DisplayName("Recommendation fingerprint must be unique to prevent duplicate findings")
    void recommendationFingerprint_mustBeUnique() {
        String fingerprint = "fp-rule-a-google-ads-brand-4-2026-03";

        Recommendation rec1 = new Recommendation();
        rec1.setFingerprint(fingerprint);
        rec1.setRuleIdentifier(RuleIdentifier.RULE_A_ZERO_CONVERSIONS);
        rec1.setPlatform("Google Ads");
        rec1.setCampaignName("Brand-4");
        rec1.setCampaignIdentityKey("GOOGLE_ADS|brand-4");
        rec1.setIdentityReliability("LOW_NAME");
        rec1.setPeriodStart(LocalDate.of(2026, 3, 1));
        rec1.setPeriodEnd(LocalDate.of(2026, 3, 31));
        rec1.setPriority(RecommendationPriority.HIGH);
        rec1.setTitle("High spend with zero conversions");
        rec1.setExplanation("Spend exceeded ₹1,000 with 0 conversions");
        rec1.setSuggestedAction("Investigate campaign landing page");
        rec1.setSupportingMetricsJson("{\"spend\": 5000, \"conversions\": 0}");
        rec1.setTriggerCondition("spend > 1000 AND conversions == 0");
        rec1.setThresholdsUsedJson("{\"minSpend\": 1000}");
        rec1.setStatus(RecommendationStatus.NEW);

        recommendationRepository.saveAndFlush(rec1);

        Recommendation rec2 = new Recommendation();
        rec2.setFingerprint(fingerprint); // Duplicate fingerprint
        rec2.setRuleIdentifier(RuleIdentifier.RULE_A_ZERO_CONVERSIONS);
        rec2.setPlatform("Google Ads");
        rec2.setCampaignName("Brand-4");
        rec2.setCampaignIdentityKey("GOOGLE_ADS|brand-4");
        rec2.setIdentityReliability("LOW_NAME");
        rec2.setPeriodStart(LocalDate.of(2026, 3, 1));
        rec2.setPeriodEnd(LocalDate.of(2026, 3, 31));
        rec2.setPriority(RecommendationPriority.HIGH);
        rec2.setTitle("High spend with zero conversions duplicate");
        rec2.setExplanation("Duplicate finding");
        rec2.setSuggestedAction("Action");
        rec2.setSupportingMetricsJson("{}");
        rec2.setTriggerCondition("condition");
        rec2.setThresholdsUsedJson("{}");
        rec2.setStatus(RecommendationStatus.NEW);

        assertThatThrownBy(() -> recommendationRepository.saveAndFlush(rec2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
