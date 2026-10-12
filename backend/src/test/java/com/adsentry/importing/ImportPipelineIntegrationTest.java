package com.adsentry.importing;

import com.adsentry.common.exception.ApiException;
import com.adsentry.importing.domain.BatchStatus;
import com.adsentry.importing.domain.EffectiveRecord;
import com.adsentry.importing.domain.ImportBatch;
import com.adsentry.importing.dto.ImportBatchResponse;
import com.adsentry.importing.dto.ImportConfirmRequest;
import com.adsentry.importing.dto.ImportPreviewRequest;
import com.adsentry.importing.dto.ImportPreviewResponse;
import com.adsentry.importing.repository.EffectiveRecordRepository;
import com.adsentry.importing.repository.ImportBatchRepository;
import com.adsentry.importing.repository.ImportRowErrorRepository;
import com.adsentry.importing.repository.ImportedRecordRepository;
import com.adsentry.importing.service.ImportPreviewService;
import com.adsentry.importing.service.ImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ImportPipelineIntegrationTest {

    @Autowired
    private ImportPreviewService previewService;

    @Autowired
    private ImportService importService;

    @Autowired
    private ImportBatchRepository batchRepository;

    @Autowired
    private ImportedRecordRepository recordRepository;

    @Autowired
    private ImportRowErrorRepository rowErrorRepository;

    @Autowired
    private EffectiveRecordRepository effectiveRecordRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        recordRepository.deleteAll();
        rowErrorRepository.deleteAll();
        batchRepository.deleteAll();
    }

    @Test
    @DisplayName("CSV Preview does NOT persist batches or records to the database")
    void preview_doesNotPersistRecords() throws IOException {
        ClassPathResource resource = new ClassPathResource("fixtures/sample-marketing.csv");
        try (InputStream in = resource.getInputStream()) {
            ImportPreviewRequest request = new ImportPreviewRequest();
            request.setConfirmedCurrency("INR");

            ImportPreviewResponse response = previewService.preview("sample-marketing.csv", in, request);

            assertThat(response).isNotNull();
            assertThat(response.getTotalRows()).isEqualTo(260);
            assertThat(response.getEstimatedAcceptedRows()).isEqualTo(60); // 34 Google Ads + 26 Meta Ads
            assertThat(response.getEstimatedRejectedRows()).isEqualTo(200); // 200 non-supported channel rows
            assertThat(response.getDetectedCurrency()).isEqualTo("INR");
            assertThat(response.getWarnings()).isNotEmpty();
        }

        // Database must remain completely clean
        assertThat(batchRepository.count()).isEqualTo(0);
        assertThat(recordRepository.count()).isEqualTo(0);
        assertThat(rowErrorRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Partial import with real dataset: Accepts supported Google/Meta Ads rows, isolates row errors for unsupported channels")
    void importBatch_partialImportWithRealDataset() throws IOException {
        ClassPathResource resource = new ClassPathResource("fixtures/sample-marketing.csv");
        ImportBatchResponse response;
        try (InputStream in = resource.getInputStream()) {
            ImportConfirmRequest request = new ImportConfirmRequest();
            request.setConfirmedCurrency("INR");

            response = importService.importBatch("sample-marketing.csv", in, request);
        }

        assertThat(response.getStatus()).isEqualTo(BatchStatus.COMPLETED.name());
        assertThat(response.getTotalRows()).isEqualTo(260);
        assertThat(response.getAcceptedRows()).isEqualTo(60);
        assertThat(response.getRejectedRows()).isEqualTo(200);

        // Verification of database state
        assertThat(batchRepository.count()).isEqualTo(1);
        assertThat(recordRepository.findByBatchId(response.getId())).hasSize(60);
        assertThat(rowErrorRepository.countByBatchId(response.getId())).isEqualTo(200);

        // Verify effective records view exposes the 60 valid records
        List<EffectiveRecord> effectiveRecords = effectiveRecordRepository.findAll();
        assertThat(effectiveRecords).hasSize(60);

        // Verify stable campaign identity across dates
        List<EffectiveRecord> brand4Records = effectiveRecordRepository.findByCampaignIdentityKey("META_ADS:NAME:brand-4");
        assertThat(brand4Records).isNotEmpty();
        assertThat(brand4Records.get(0).getCampaignName()).isEqualTo("Brand-4");
    }

    @Test
    @DisplayName("Duplicate imports: Ingesting identical file twice does not double-count metrics and tracks duplicate counts")
    void importBatch_duplicateImportsDoNotDoubleCount() throws IOException {
        String csv = """
                date,channel,campaign,impressions,clicks,spend,conversions
                2026-03-01,Google Ads,Brand-1,1000,50,500.00,5
                2026-03-02,Google Ads,Brand-1,1200,60,600.00,6
                """;

        // Import Batch 1
        ImportBatchResponse batch1 = importService.importBatch(
                "export_v1.csv",
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                createRequest("INR")
        );
        assertThat(batch1.getAcceptedRows()).isEqualTo(2);
        assertThat(effectiveRecordRepository.count()).isEqualTo(2);

        // Import Batch 2 with identical rows
        ImportBatchResponse batch2 = importService.importBatch(
                "export_v2_duplicate.csv",
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                createRequest("INR")
        );
        assertThat(batch2.getAcceptedRows()).isEqualTo(2);
        assertThat(batch2.getDuplicateRows()).isEqualTo(2); // Tracked as duplicates

        // Crucial Check: Effective records count is STILL 2 (NO double counting!)
        List<EffectiveRecord> effectiveRecords = effectiveRecordRepository.findAll();
        assertThat(effectiveRecords).hasSize(2);
    }

    @Test
    @DisplayName("Overlapping imports & conflict resolution: Newer batch wins, and deleting winning batch restores older version")
    void overlappingBatches_newestWins_andDeletingRestoresOlderVersion() throws IOException {
        String csvV1 = """
                date,channel,campaign,impressions,clicks,spend,conversions
                2026-03-01,Google Ads,Brand-1,1000,50,1000.00,5
                """;

        String csvV2 = """
                date,channel,campaign,impressions,clicks,spend,conversions
                2026-03-01,Google Ads,Brand-1,1500,80,2500.00,10
                """;

        // 1. Ingest Batch 1 (Old version: Spend = 1000.00)
        ImportBatchResponse batch1 = importService.importBatch(
                "initial.csv",
                new ByteArrayInputStream(csvV1.getBytes(StandardCharsets.UTF_8)),
                createRequest("INR")
        );
        String recordKey = "GOOGLE_ADS:NAME:brand-1|2026-03-01|default|UNSPECIFIED";

        Optional<EffectiveRecord> effV1 = effectiveRecordRepository.findByIdentityKey(recordKey);
        assertThat(effV1).isPresent();
        assertThat(effV1.get().getBatchId()).isEqualTo(batch1.getId());
        assertThat(effV1.get().getSpend()).isEqualByComparingTo("1000.00");

        // 2. Ingest Batch 2 (Newer conflicting version: Spend = 2500.00)
        ImportBatchResponse batch2 = importService.importBatch(
                "revised.csv",
                new ByteArrayInputStream(csvV2.getBytes(StandardCharsets.UTF_8)),
                createRequest("INR")
        );
        assertThat(batch2.getUpdatedRows()).isEqualTo(1);

        // Batch 2 must WIN
        Optional<EffectiveRecord> effV2 = effectiveRecordRepository.findByIdentityKey(recordKey);
        assertThat(effV2).isPresent();
        assertThat(effV2.get().getBatchId()).isEqualTo(batch2.getId());
        assertThat(effV2.get().getSpend()).isEqualByComparingTo("2500.00");

        // 3. Delete Batch 2 -> Batch 1 MUST be restored!
        importService.deleteBatch(batch2.getId());

        Optional<EffectiveRecord> restoredV1 = effectiveRecordRepository.findByIdentityKey(recordKey);
        assertThat(restoredV1).isPresent();
        assertThat(restoredV1.get().getBatchId()).isEqualTo(batch1.getId());
        assertThat(restoredV1.get().getSpend()).isEqualByComparingTo("1000.00");

        // 4. Delete Batch 1 -> 0 effective records
        importService.deleteBatch(batch1.getId());
        assertThat(effectiveRecordRepository.findByIdentityKey(recordKey)).isEmpty();
    }

    @Test
    @DisplayName("Atomic replacement: replaceBatch replaces records of existing batch atomically")
    void replaceBatch_atomicReplacement() throws IOException {
        String originalCsv = """
                date,channel,campaign,spend
                2026-03-01,Google Ads,Brand-A,100.00
                """;
        ImportBatchResponse originalBatch = importService.importBatch(
                "file1.csv",
                new ByteArrayInputStream(originalCsv.getBytes(StandardCharsets.UTF_8)),
                createRequest("INR")
        );

        String replacementCsv = """
                date,channel,campaign,spend
                2026-03-01,Google Ads,Brand-A,999.00
                2026-03-02,Google Ads,Brand-B,500.00
                """;

        ImportBatchResponse replacedBatch = importService.replaceBatch(
                originalBatch.getId(),
                "file1_replaced.csv",
                new ByteArrayInputStream(replacementCsv.getBytes(StandardCharsets.UTF_8)),
                createRequest("INR")
        );

        assertThat(replacedBatch.getId()).isEqualTo(originalBatch.getId());
        assertThat(replacedBatch.getAcceptedRows()).isEqualTo(2);

        // Effective records should now reflect the replacement data
        List<EffectiveRecord> records = effectiveRecordRepository.findAll();
        assertThat(records).hasSize(2);
        assertThat(records).anyMatch(r -> r.getSpend().compareTo(new BigDecimal("999.00")) == 0);
    }

    @Test
    @DisplayName("Transaction rollback: If replacement fails validation or execution, existing batch and records are preserved")
    void transactionRollback_preservesExistingDataOnFailure() throws IOException {
        String originalCsv = """
                date,channel,campaign,spend
                2026-03-01,Google Ads,Brand-A,100.00
                """;
        ImportBatchResponse originalBatch = importService.importBatch(
                "file1.csv",
                new ByteArrayInputStream(originalCsv.getBytes(StandardCharsets.UTF_8)),
                createRequest("INR")
        );

        // Malformed replacement that triggers an exception during replacement
        String invalidReplacementCsv = """
                date,channel,campaign,spend
                invalid-date,Google Ads,Brand-A,-500.00
                """;

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> {
            tx.execute(status -> {
                try {
                    // Attempt replace with empty file which throws ApiException
                    importService.replaceBatch(
                            originalBatch.getId(),
                            "empty.csv",
                            new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)),
                            createRequest("INR")
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        }).isInstanceOf(ApiException.class);

        // Verify: Original batch and record are preserved intact!
        ImportBatch preservedBatch = batchRepository.findById(originalBatch.getId()).orElseThrow();
        assertThat(preservedBatch.getAcceptedRows()).isEqualTo(1);
        assertThat(recordRepository.findByBatchId(originalBatch.getId())).hasSize(1);
    }

    private ImportConfirmRequest createRequest(String currency) {
        ImportConfirmRequest req = new ImportConfirmRequest();
        req.setConfirmedCurrency(currency);
        return req;
    }
}
