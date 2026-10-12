package com.adsentry.importing;

import com.adsentry.importing.domain.*;
import com.adsentry.importing.repository.EffectiveRecordRepository;
import com.adsentry.importing.repository.ImportBatchRepository;
import com.adsentry.importing.repository.ImportedRecordRepository;
import com.adsentry.normalization.domain.ConversionDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RecordRestorationIntegrationTest {

    @Autowired
    private ImportBatchRepository batchRepository;

    @Autowired
    private ImportedRecordRepository recordRepository;

    @Autowired
    private EffectiveRecordRepository effectiveRecordRepository;

    @BeforeEach
    void setUp() {
        recordRepository.deleteAll();
        batchRepository.deleteAll();
    }

    @Test
    @DisplayName("Overlapping imports win by newest timestamp, and deleting newest restores older version")
    void versioningAndDeduplication_deletingNewestRestoresOlderVersion() {
        String identityKey = "GOOGLE_ADS|brand-4|2026-03-01|default|UNSPECIFIED";
        LocalDate reportingDate = LocalDate.of(2026, 3, 1);

        // 1. Ingest Batch 1 (Oldest version, timestamp T1)
        Instant timestamp1 = Instant.parse("2026-03-01T10:00:00Z");
        ImportBatch batch1 = new ImportBatch("Google Ads", "first_export.csv", timestamp1, "INR", BatchStatus.COMPLETED);
        batch1.setTotalRows(1);
        batch1.setAcceptedRows(1);
        batch1 = batchRepository.save(batch1);

        ImportedRecord record1 = new ImportedRecord();
        record1.setBatch(batch1);
        record1.setCampaignIdentityKey("GOOGLE_ADS:NAME:brand-4");
        record1.setIdentityKey(identityKey);
        record1.setPlatform("GOOGLE_ADS");
        record1.setCampaignName("Brand-4");
        record1.setCampaignNameNormalized("brand-4");
        record1.setIdentityConfidence(IdentityConfidence.LOW_NAME);
        record1.setReportingDate(reportingDate);
        record1.setSpend(new BigDecimal("1000.00"));
        record1.setCurrency("INR");
        record1.setClicks(50L);
        record1.setImpressions(1500L);
        record1.setConversions(new BigDecimal("5.00"));
        record1.setConversionDefinition(ConversionDefinition.UNSPECIFIED);
        record1.setSourceRowNumber(2);
        record1.setImportedAt(timestamp1);
        recordRepository.save(record1);

        // Verify: Batch 1 is effective
        Optional<EffectiveRecord> effectiveV1 = effectiveRecordRepository.findByIdentityKey(identityKey);
        assertThat(effectiveV1).isPresent();
        assertThat(effectiveV1.get().getBatchId()).isEqualTo(batch1.getId());
        assertThat(effectiveV1.get().getCampaignIdentityKey()).isEqualTo("GOOGLE_ADS:NAME:brand-4");
        assertThat(effectiveV1.get().getSpend()).isEqualByComparingTo("1000.00");
        assertThat(effectiveV1.get().getClicks()).isEqualTo(50L);

        // 2. Ingest Batch 2 (Newer version, timestamp T2 > T1)
        Instant timestamp2 = Instant.parse("2026-03-02T15:30:00Z");
        ImportBatch batch2 = new ImportBatch("Google Ads", "revised_export.csv", timestamp2, "INR", BatchStatus.COMPLETED);
        batch2.setTotalRows(1);
        batch2.setAcceptedRows(1);
        batch2.setUpdatedRows(1);
        batch2 = batchRepository.save(batch2);

        ImportedRecord record2 = new ImportedRecord();
        record2.setBatch(batch2);
        record2.setCampaignIdentityKey("GOOGLE_ADS:NAME:brand-4");
        record2.setIdentityKey(identityKey);
        record2.setPlatform("GOOGLE_ADS");
        record2.setCampaignName("Brand-4");
        record2.setCampaignNameNormalized("brand-4");
        record2.setIdentityConfidence(IdentityConfidence.LOW_NAME);
        record2.setReportingDate(reportingDate);
        record2.setSpend(new BigDecimal("2500.00")); // Updated spend
        record2.setCurrency("INR");
        record2.setClicks(120L); // Updated clicks
        record2.setImpressions(3200L);
        record2.setConversions(new BigDecimal("12.00"));
        record2.setConversionDefinition(ConversionDefinition.UNSPECIFIED);
        record2.setSourceRowNumber(2);
        record2.setImportedAt(timestamp2);
        recordRepository.save(record2);

        // Verify: Batch 2 wins because timestamp T2 > T1. No duplicate double-counting!
        Optional<EffectiveRecord> effectiveV2 = effectiveRecordRepository.findByIdentityKey(identityKey);
        assertThat(effectiveV2).isPresent();
        assertThat(effectiveV2.get().getBatchId()).isEqualTo(batch2.getId());
        assertThat(effectiveV2.get().getSpend()).isEqualByComparingTo("2500.00");
        assertThat(effectiveV2.get().getClicks()).isEqualTo(120L);

        // 3. Delete newest Batch 2
        batchRepository.delete(batch2);

        // 4. Verify RESTORATION: Deleting newest restores older Batch 1!
        Optional<EffectiveRecord> restoredV1 = effectiveRecordRepository.findByIdentityKey(identityKey);
        assertThat(restoredV1).isPresent();
        assertThat(restoredV1.get().getBatchId()).isEqualTo(batch1.getId());
        assertThat(restoredV1.get().getSpend()).isEqualByComparingTo("1000.00");
        assertThat(restoredV1.get().getClicks()).isEqualTo(50L);

        // 5. Delete Batch 1
        batchRepository.delete(batch1);

        // 6. Verify: No effective records remain
        Optional<EffectiveRecord> emptyResult = effectiveRecordRepository.findByIdentityKey(identityKey);
        assertThat(emptyResult).isEmpty();
    }
}
