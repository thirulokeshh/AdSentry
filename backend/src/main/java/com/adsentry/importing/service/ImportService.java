package com.adsentry.importing.service;

import com.adsentry.common.exception.ApiException;
import com.adsentry.common.exception.ErrorCode;
import com.adsentry.importing.domain.BatchStatus;
import com.adsentry.importing.domain.EffectiveRecord;
import com.adsentry.importing.domain.ImportBatch;
import com.adsentry.importing.domain.ImportRowError;
import com.adsentry.importing.domain.ImportedRecord;
import com.adsentry.importing.dto.ImportBatchResponse;
import com.adsentry.importing.dto.ImportConfirmRequest;
import com.adsentry.importing.dto.RowErrorPreviewDto;
import com.adsentry.importing.repository.EffectiveRecordRepository;
import com.adsentry.importing.repository.ImportBatchRepository;
import com.adsentry.importing.repository.ImportRowErrorRepository;
import com.adsentry.importing.repository.ImportedRecordRepository;
import com.adsentry.importing.service.currency.CurrencyDetectionResult;
import com.adsentry.importing.service.currency.CurrencyService;
import com.adsentry.importing.service.csv.CsvParser;
import com.adsentry.importing.service.csv.CsvRow;
import com.adsentry.importing.service.csv.CsvTable;
import com.adsentry.importing.service.mapping.ColumnMapper;
import com.adsentry.importing.service.mapping.ColumnMapping;
import com.adsentry.importing.service.validation.RowValidationResult;
import com.adsentry.importing.service.validation.RowValidator;
import com.adsentry.normalization.domain.ChannelClassifier;
import com.adsentry.normalization.domain.ConversionDefinition;
import com.adsentry.normalization.domain.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Core import pipeline orchestrator:
 * - Executes partial imports with row-level error reporting
 * - Enforces currency and column mapping rules
 * - Tracks duplicate imports and updated records
 * - Manages atomic replacement and version restoration upon batch deletion
 */
@Service
public class ImportService {

    private final CsvParser csvParser;
    private final ColumnMapper columnMapper;
    private final CurrencyService currencyService;
    private final ChannelClassifier channelClassifier;
    private final RowValidator rowValidator;
    private final ImportBatchRepository batchRepository;
    private final ImportedRecordRepository recordRepository;
    private final ImportRowErrorRepository rowErrorRepository;
    private final EffectiveRecordRepository effectiveRecordRepository;
    private final ObjectMapper objectMapper;

    public ImportService(CsvParser csvParser,
                         ColumnMapper columnMapper,
                         CurrencyService currencyService,
                         ChannelClassifier channelClassifier,
                         RowValidator rowValidator,
                         ImportBatchRepository batchRepository,
                         ImportedRecordRepository recordRepository,
                         ImportRowErrorRepository rowErrorRepository,
                         EffectiveRecordRepository effectiveRecordRepository,
                         ObjectMapper objectMapper) {
        this.csvParser = csvParser;
        this.columnMapper = columnMapper;
        this.currencyService = currencyService;
        this.channelClassifier = channelClassifier;
        this.rowValidator = rowValidator;
        this.batchRepository = batchRepository;
        this.recordRepository = recordRepository;
        this.rowErrorRepository = rowErrorRepository;
        this.effectiveRecordRepository = effectiveRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportBatchResponse importBatch(String filename, InputStream inputStream, ImportConfirmRequest request) throws IOException {
        CsvTable table = csvParser.parse(inputStream);
        if (table.getRowCount() == 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Cannot import empty CSV file.");
        }

        // 1. Column mapping
        ColumnMapping mapping = columnMapper.map(table, request != null ? request.getColumnMappingOverrides() : null);

        // 2. Currency confirmation & validation
        CurrencyDetectionResult currencyResult = currencyService.detectCurrency(
                table,
                mapping.getSpendColumn(),
                request != null ? request.getConfirmedCurrency() : null
        );

        if (!currencyResult.isSupported()) {
            throw new ApiException(ErrorCode.UNSUPPORTED_CURRENCY, currencyResult.getErrorMessage());
        }
        if (currencyResult.isAmbiguous()) {
            throw new ApiException(ErrorCode.CURRENCY_AMBIGUITY, currencyResult.getErrorMessage());
        }

        columnMapper.validateRequired(mapping);

        // 3. Platform detection
        Platform defaultPlatform = determinePlatform(table, mapping, filename, request);

        // 4. Create batch in PROCESSING status
        Instant importTimestamp = Instant.now();
        String platformDisplayName = defaultPlatform != Platform.UNSUPPORTED ? defaultPlatform.getDisplayName() : "Multi-Platform";
        ImportBatch batch = new ImportBatch(
                platformDisplayName,
                filename != null ? filename : "import.csv",
                importTimestamp,
                CurrencyService.SUPPORTED_CURRENCY,
                BatchStatus.PROCESSING
        );
        batch = batchRepository.save(batch);

        // 5. Process rows (partial import support)
        ProcessRowsResult rowsResult = processRows(table, mapping, defaultPlatform, batch, request);

        // 6. Save records and row errors
        if (!rowsResult.validRecords.isEmpty()) {
            recordRepository.saveAll(rowsResult.validRecords);
        }
        if (!rowsResult.errors.isEmpty()) {
            rowErrorRepository.saveAll(rowsResult.errors);
        }

        // 7. Finalize batch statistics
        batch.setTotalRows(table.getRowCount());
        batch.setAcceptedRows(rowsResult.validRecords.size());
        batch.setRejectedRows(rowsResult.errors.size());
        batch.setDuplicateRows(rowsResult.duplicateCount);
        batch.setUpdatedRows(rowsResult.updatedCount);
        batch.setReportingStartDate(rowsResult.startDate);
        batch.setReportingEndDate(rowsResult.endDate);
        batch.setColumnMappingJson(mapping.toJson(objectMapper));
        batch.setValidationSummary(String.format("Accepted %d valid records (%d updated, %d identical duplicates). Rejected %d invalid rows.",
                rowsResult.validRecords.size(), rowsResult.updatedCount, rowsResult.duplicateCount, rowsResult.errors.size()));
        batch.setStatus(BatchStatus.COMPLETED);

        batch = batchRepository.save(batch);

        return mapToResponse(batch, rowsResult.sampleErrors);
    }

    @Transactional
    public ImportBatchResponse replaceBatch(Long batchId, String filename, InputStream inputStream, ImportConfirmRequest request) throws IOException {
        ImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ApiException(ErrorCode.BATCH_NOT_FOUND, "Batch with ID " + batchId + " not found."));

        // Delete existing records and errors of this batch atomically
        recordRepository.deleteByBatchId(batchId);
        rowErrorRepository.deleteByBatchId(batchId);

        // Parse new CSV
        CsvTable table = csvParser.parse(inputStream);
        if (table.getRowCount() == 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Replacement CSV file is empty.");
        }

        ColumnMapping mapping = columnMapper.map(table, request != null ? request.getColumnMappingOverrides() : null);

        CurrencyDetectionResult currencyResult = currencyService.detectCurrency(
                table,
                mapping.getSpendColumn(),
                request != null ? request.getConfirmedCurrency() : null
        );

        if (!currencyResult.isSupported()) {
            throw new ApiException(ErrorCode.UNSUPPORTED_CURRENCY, currencyResult.getErrorMessage());
        }
        if (currencyResult.isAmbiguous()) {
            throw new ApiException(ErrorCode.CURRENCY_AMBIGUITY, currencyResult.getErrorMessage());
        }

        columnMapper.validateRequired(mapping);

        Platform defaultPlatform = determinePlatform(table, mapping, filename, request);

        // Reprocess rows for this existing batch
        batch.setOriginalFilename(filename != null ? filename : batch.getOriginalFilename());
        batch.setImportTimestamp(Instant.now());
        batch.setStatus(BatchStatus.PROCESSING);

        ProcessRowsResult rowsResult = processRows(table, mapping, defaultPlatform, batch, request);

        if (!rowsResult.validRecords.isEmpty()) {
            recordRepository.saveAll(rowsResult.validRecords);
        }
        if (!rowsResult.errors.isEmpty()) {
            rowErrorRepository.saveAll(rowsResult.errors);
        }

        batch.setTotalRows(table.getRowCount());
        batch.setAcceptedRows(rowsResult.validRecords.size());
        batch.setRejectedRows(rowsResult.errors.size());
        batch.setDuplicateRows(rowsResult.duplicateCount);
        batch.setUpdatedRows(rowsResult.updatedCount);
        batch.setReportingStartDate(rowsResult.startDate);
        batch.setReportingEndDate(rowsResult.endDate);
        batch.setColumnMappingJson(mapping.toJson(objectMapper));
        batch.setValidationSummary(String.format("Replaced batch with %d accepted records (%d updated). %d rows rejected.",
                rowsResult.validRecords.size(), rowsResult.updatedCount, rowsResult.errors.size()));
        batch.setStatus(BatchStatus.COMPLETED);

        batch = batchRepository.save(batch);

        return mapToResponse(batch, rowsResult.sampleErrors);
    }

    @Transactional
    public void deleteBatch(Long batchId) {
        ImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ApiException(ErrorCode.BATCH_NOT_FOUND, "Batch with ID " + batchId + " not found."));

        // Cascade delete via database & repository restores previously winning versions in effective_records_view
        recordRepository.deleteByBatchId(batchId);
        rowErrorRepository.deleteByBatchId(batchId);
        batchRepository.delete(batch);
    }

    @Transactional(readOnly = true)
    public ImportBatchResponse getBatch(Long batchId) {
        ImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ApiException(ErrorCode.BATCH_NOT_FOUND, "Batch with ID " + batchId + " not found."));
        List<ImportRowError> errors = rowErrorRepository.findByBatchIdOrderBySourceRowNumberAsc(batchId);
        List<RowErrorPreviewDto> sampleErrors = errors.stream()
                .limit(20)
                .map(e -> new RowErrorPreviewDto(e.getSourceRowNumber(), e.getFieldName(), e.getErrorCode(), e.getErrorMessage()))
                .toList();
        return mapToResponse(batch, sampleErrors);
    }

    @Transactional(readOnly = true)
    public List<ImportBatchResponse> listBatches() {
        return batchRepository.findAllByOrderByImportTimestampDesc().stream()
                .map(b -> mapToResponse(b, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<RowErrorPreviewDto> getBatchErrors(Long batchId, Pageable pageable) {
        if (!batchRepository.existsById(batchId)) {
            throw new ApiException(ErrorCode.BATCH_NOT_FOUND, "Batch with ID " + batchId + " not found.");
        }
        return rowErrorRepository.findByBatchId(batchId, pageable)
                .map(e -> new RowErrorPreviewDto(e.getSourceRowNumber(), e.getFieldName(), e.getErrorCode(), e.getErrorMessage()));
    }

    private ProcessRowsResult processRows(CsvTable table,
                                          ColumnMapping mapping,
                                          Platform defaultPlatform,
                                          ImportBatch batch,
                                          ImportConfirmRequest request) {
        List<ImportedRecord> validRecords = new ArrayList<>();
        List<ImportRowError> errors = new ArrayList<>();
        List<RowErrorPreviewDto> sampleErrors = new ArrayList<>();

        int duplicateCount = 0;
        int updatedCount = 0;
        LocalDate startDate = null;
        LocalDate endDate = null;

        String dateFormat = (request != null ? request.getDateFormat() : null);
        ConversionDefinition explicitConvDef = (request != null && request.getConversionDefinition() != null)
                ? ConversionDefinition.fromString(request.getConversionDefinition())
                : ConversionDefinition.UNSPECIFIED;

        for (CsvRow row : table.getRows()) {
            RowValidationResult result = rowValidator.validate(row, mapping, defaultPlatform, dateFormat, explicitConvDef);

            if (result.isValid()) {
                // Check for existing effective record with matching record identity key
                Optional<EffectiveRecord> existingOpt = effectiveRecordRepository.findByIdentityKey(result.getRecordIdentityKey());
                if (existingOpt.isPresent()) {
                    EffectiveRecord existing = existingOpt.get();
                    if (isMetricsIdentical(existing, result)) {
                        duplicateCount++;
                    } else {
                        updatedCount++;
                    }
                }

                ImportedRecord record = new ImportedRecord();
                record.setBatch(batch);
                record.setCampaignIdentityKey(result.getCampaignIdentityKey());
                record.setIdentityKey(result.getRecordIdentityKey());
                record.setPlatform(result.getPlatform().name());
                record.setCampaignId(result.getCampaignId());
                record.setCampaignName(result.getCampaignName());
                record.setCampaignNameNormalized(result.getCampaignNameNormalized());
                record.setIdentityConfidence(result.getIdentityConfidence());
                record.setReportingDate(result.getReportingDate());
                record.setSpend(result.getSpend());
                record.setCurrency(result.getCurrency());
                record.setImpressions(result.getImpressions());
                record.setClicks(result.getClicks());
                record.setConversions(result.getConversions());
                record.setConversionDefinition(result.getConversionDefinition());
                record.setDimensionsJson(result.getDimensionsJson());
                record.setSourceRowNumber(result.getSourceRowNumber());
                record.setImportedAt(batch.getImportTimestamp());

                validRecords.add(record);

                LocalDate rDate = result.getReportingDate();
                if (startDate == null || rDate.isBefore(startDate)) startDate = rDate;
                if (endDate == null || rDate.isAfter(endDate)) endDate = rDate;
            } else {
                ImportRowError rowError = new ImportRowError(
                        batch,
                        result.getSourceRowNumber(),
                        result.getErrorField(),
                        result.getErrorCode().name(),
                        result.getErrorMessage()
                );
                errors.add(rowError);

                if (sampleErrors.size() < 20) {
                    sampleErrors.add(new RowErrorPreviewDto(
                            result.getSourceRowNumber(),
                            result.getErrorField(),
                            result.getErrorCode().name(),
                            result.getErrorMessage()
                    ));
                }
            }
        }

        return new ProcessRowsResult(validRecords, errors, sampleErrors, duplicateCount, updatedCount, startDate, endDate);
    }

    private boolean isMetricsIdentical(EffectiveRecord existing, RowValidationResult current) {
        boolean spendEqual = existing.getSpend().compareTo(current.getSpend()) == 0;
        boolean impEqual = Objects.equals(existing.getImpressions(), current.getImpressions());
        boolean clicksEqual = Objects.equals(existing.getClicks(), current.getClicks());
        boolean convEqual = (existing.getConversions() == null && current.getConversions() == null) ||
                (existing.getConversions() != null && current.getConversions() != null &&
                        existing.getConversions().compareTo(current.getConversions()) == 0);
        return spendEqual && impEqual && clicksEqual && convEqual;
    }

    private Platform determinePlatform(CsvTable table, ColumnMapping mapping, String filename, ImportConfirmRequest request) {
        if (request != null && request.getConfirmedPlatform() != null && !request.getConfirmedPlatform().isBlank()) {
            return Platform.fromString(request.getConfirmedPlatform());
        }
        if (mapping.getChannelColumn() != null) {
            return Platform.GOOGLE_ADS; // fallback if channel column present; rows classify individually
        }
        String lowerFn = (filename != null ? filename.toLowerCase() : "");
        if (lowerFn.contains("google")) return Platform.GOOGLE_ADS;
        if (lowerFn.contains("meta") || lowerFn.contains("facebook")) return Platform.META_ADS;
        return Platform.GOOGLE_ADS;
    }

    private ImportBatchResponse mapToResponse(ImportBatch batch, List<RowErrorPreviewDto> sampleErrors) {
        ImportBatchResponse response = new ImportBatchResponse();
        response.setId(batch.getId());
        response.setPlatform(batch.getPlatform());
        response.setOriginalFilename(batch.getOriginalFilename());
        response.setImportTimestamp(batch.getImportTimestamp());
        response.setReportingStartDate(batch.getReportingStartDate());
        response.setReportingEndDate(batch.getReportingEndDate());
        response.setStatus(batch.getStatus().name());
        response.setCurrency(batch.getCurrency());
        response.setTotalRows(batch.getTotalRows());
        response.setAcceptedRows(batch.getAcceptedRows());
        response.setRejectedRows(batch.getRejectedRows());
        response.setDuplicateRows(batch.getDuplicateRows());
        response.setUpdatedRows(batch.getUpdatedRows());
        response.setValidationSummary(batch.getValidationSummary());
        response.setErrors(sampleErrors);

        if (batch.getColumnMappingJson() != null) {
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = objectMapper.readValue(batch.getColumnMappingJson(), java.util.Map.class);
                response.setColumnMapping(map);
            } catch (Exception ignored) {
            }
        }
        return response;
    }

    private static class ProcessRowsResult {
        final List<ImportedRecord> validRecords;
        final List<ImportRowError> errors;
        final List<RowErrorPreviewDto> sampleErrors;
        final int duplicateCount;
        final int updatedCount;
        final LocalDate startDate;
        final LocalDate endDate;

        ProcessRowsResult(List<ImportedRecord> validRecords,
                          List<ImportRowError> errors,
                          List<RowErrorPreviewDto> sampleErrors,
                          int duplicateCount,
                          int updatedCount,
                          LocalDate startDate,
                          LocalDate endDate) {
            this.validRecords = validRecords;
            this.errors = errors;
            this.sampleErrors = sampleErrors;
            this.duplicateCount = duplicateCount;
            this.updatedCount = updatedCount;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
}
