package com.adsentry.importing.service;

import com.adsentry.common.exception.ApiException;
import com.adsentry.common.exception.ErrorCode;
import com.adsentry.importing.dto.ImportPreviewRequest;
import com.adsentry.importing.dto.ImportPreviewResponse;
import com.adsentry.importing.dto.RowErrorPreviewDto;
import com.adsentry.importing.dto.RowPreviewDto;
import com.adsentry.importing.repository.EffectiveRecordRepository;
import com.adsentry.importing.service.currency.CurrencyDetectionResult;
import com.adsentry.importing.service.currency.CurrencyService;
import com.adsentry.importing.service.csv.CsvParser;
import com.adsentry.importing.service.csv.CsvRow;
import com.adsentry.importing.service.csv.CsvTable;
import com.adsentry.importing.service.mapping.ColumnMapper;
import com.adsentry.importing.service.mapping.ColumnMapping;
import com.adsentry.importing.service.validation.RowValidationResult;
import com.adsentry.importing.service.validation.RowValidator;
import com.adsentry.normalization.domain.ChannelClassificationResult;
import com.adsentry.normalization.domain.ChannelClassifier;
import com.adsentry.normalization.domain.ConversionDefinition;
import com.adsentry.normalization.domain.Platform;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates rich previews of CSV files before committing to database.
 * Never persists any records or batches to the database.
 */
@Service
public class ImportPreviewService {

    private final CsvParser csvParser;
    private final ColumnMapper columnMapper;
    private final CurrencyService currencyService;
    private final ChannelClassifier channelClassifier;
    private final RowValidator rowValidator;
    private final EffectiveRecordRepository effectiveRecordRepository;

    public ImportPreviewService(CsvParser csvParser,
                                ColumnMapper columnMapper,
                                CurrencyService currencyService,
                                ChannelClassifier channelClassifier,
                                RowValidator rowValidator,
                                EffectiveRecordRepository effectiveRecordRepository) {
        this.csvParser = csvParser;
        this.columnMapper = columnMapper;
        this.currencyService = currencyService;
        this.channelClassifier = channelClassifier;
        this.rowValidator = rowValidator;
        this.effectiveRecordRepository = effectiveRecordRepository;
    }

    public ImportPreviewResponse preview(String filename, InputStream inputStream, ImportPreviewRequest request) throws IOException {
        CsvTable table = csvParser.parse(inputStream);

        if (table.getRowCount() == 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "The provided CSV file is empty or contains only a header.");
        }

        // 1. Column Mapping
        ColumnMapping mapping = columnMapper.map(table, request != null ? request.getColumnMappingOverrides() : null);

        // 2. Currency Detection & Validation (reject unsupported currency immediately)
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

        List<String> warnings = new ArrayList<>(currencyResult.getWarnings());

        // 3. Platform Detection
        Platform defaultPlatform = Platform.UNSUPPORTED;
        String detectedPlatformName = "Unspecified";
        double platformConfidence = 0.5;

        if (request != null && request.getConfirmedPlatform() != null && !request.getConfirmedPlatform().isBlank()) {
            defaultPlatform = Platform.fromString(request.getConfirmedPlatform());
            detectedPlatformName = defaultPlatform.getDisplayName();
            platformConfidence = 1.0;
        } else if (mapping.getChannelColumn() != null) {
            List<String> rawChannels = new ArrayList<>();
            for (CsvRow r : table.getRows()) {
                rawChannels.add(r.get(mapping.getChannelColumn()));
            }
            ChannelClassificationResult channelSummary = channelClassifier.summarizeChannels(rawChannels);

            if (channelSummary.unsupportedRecords() > 0) {
                warnings.add(String.format("Found %d rows across non-supported channels (%s). Partial import will filter them out.",
                        channelSummary.unsupportedRecords(),
                        String.join(", ", channelSummary.unsupportedChannelNames())));
            }

            boolean hasGoogle = channelSummary.supportedBreakdown().containsKey("Google Ads")
                    || channelSummary.supportedBreakdown().containsKey("google");
            boolean hasMeta = channelSummary.supportedBreakdown().containsKey("Meta Ads")
                    || channelSummary.supportedBreakdown().containsKey("facebook ads")
                    || channelSummary.supportedBreakdown().containsKey("meta");

            if (hasGoogle && hasMeta) {
                detectedPlatformName = "Google Ads & Meta Ads (Multi-Channel)";
                platformConfidence = 0.95;
                defaultPlatform = Platform.GOOGLE_ADS; // fallback for non-channel rows
            } else if (hasGoogle) {
                detectedPlatformName = Platform.GOOGLE_ADS.getDisplayName();
                defaultPlatform = Platform.GOOGLE_ADS;
                platformConfidence = 0.98;
            } else if (hasMeta) {
                detectedPlatformName = Platform.META_ADS.getDisplayName();
                defaultPlatform = Platform.META_ADS;
                platformConfidence = 0.98;
            } else {
                detectedPlatformName = "Unsupported Channels Only";
                platformConfidence = 0.0;
            }
        } else {
            // Channel column not present: inspect filename / headers
            String lowerFn = (filename != null ? filename.toLowerCase() : "");
            if (lowerFn.contains("google")) {
                defaultPlatform = Platform.GOOGLE_ADS;
                detectedPlatformName = Platform.GOOGLE_ADS.getDisplayName();
                platformConfidence = 0.90;
            } else if (lowerFn.contains("meta") || lowerFn.contains("facebook")) {
                defaultPlatform = Platform.META_ADS;
                detectedPlatformName = Platform.META_ADS.getDisplayName();
                platformConfidence = 0.90;
            } else {
                detectedPlatformName = "Generic / Requires Confirmation";
                platformConfidence = 0.50;
            }
        }

        // 4. Conversion Definition check
        ConversionDefinition explicitConvDef = ConversionDefinition.UNSPECIFIED;
        if (request != null && request.getConversionDefinition() != null) {
            explicitConvDef = ConversionDefinition.fromString(request.getConversionDefinition());
        }
        if (explicitConvDef == ConversionDefinition.UNSPECIFIED && mapping.getConversionsColumn() != null) {
            ConversionDefinition autoConv = ConversionDefinition.detectFromHeaderOrAction(mapping.getConversionsColumn());
            if (autoConv == ConversionDefinition.UNSPECIFIED) {
                warnings.add("Generic conversions column '" + mapping.getConversionsColumn() +
                        "' detected without explicit action type. CPA evaluation will be skipped until confirmed.");
            }
        }

        // 5. Dry-run validation of all rows
        int acceptedCount = 0;
        int rejectedCount = 0;
        int duplicateOrUpdatedCount = 0;
        LocalDate startDate = null;
        LocalDate endDate = null;

        List<RowPreviewDto> sampleAccepted = new ArrayList<>();
        List<RowErrorPreviewDto> sampleRejected = new ArrayList<>();

        String dateFormat = (request != null ? request.getDateFormat() : null);

        for (CsvRow row : table.getRows()) {
            RowValidationResult result = rowValidator.validate(row, mapping, defaultPlatform, dateFormat, explicitConvDef);
            if (result.isValid()) {
                acceptedCount++;
                LocalDate rDate = result.getReportingDate();
                if (startDate == null || rDate.isBefore(startDate)) startDate = rDate;
                if (endDate == null || rDate.isAfter(endDate)) endDate = rDate;

                // Check conflict / duplicate against existing effective records
                if (effectiveRecordRepository.findByIdentityKey(result.getRecordIdentityKey()).isPresent()) {
                    duplicateOrUpdatedCount++;
                }

                if (sampleAccepted.size() < 10) {
                    sampleAccepted.add(new RowPreviewDto(
                            result.getSourceRowNumber(),
                            result.getPlatform().getDisplayName(),
                            result.getCampaignName(),
                            result.getCampaignIdentityKey(),
                            result.getRecordIdentityKey(),
                            result.getReportingDate(),
                            result.getSpend(),
                            result.getImpressions(),
                            result.getClicks(),
                            result.getConversions(),
                            result.getConversionDefinition().name()
                    ));
                }
            } else {
                rejectedCount++;
                if (sampleRejected.size() < 10) {
                    sampleRejected.add(new RowErrorPreviewDto(
                            result.getSourceRowNumber(),
                            result.getErrorField(),
                            result.getErrorCode().name(),
                            result.getErrorMessage()
                    ));
                }
            }
        }

        ImportPreviewResponse response = new ImportPreviewResponse();
        response.setFilename(filename != null ? filename : "upload.csv");
        response.setDetectedPlatform(detectedPlatformName);
        response.setPlatformConfidence(platformConfidence);
        response.setDetectedCurrency(currencyResult.getDetectedCurrency());
        response.setCurrencyConfirmed(currencyResult.isConfirmed());
        response.setColumnMappings(mapping.toMap());
        response.setReportingStartDate(startDate);
        response.setReportingEndDate(endDate);
        response.setTotalRows(table.getRowCount());
        response.setEstimatedAcceptedRows(acceptedCount);
        response.setEstimatedRejectedRows(rejectedCount);
        response.setEstimatedDuplicateOrUpdatedRows(duplicateOrUpdatedCount);
        response.setWarnings(warnings);
        response.setSampleAcceptedRows(sampleAccepted);
        response.setSampleRejectedRows(sampleRejected);

        return response;
    }
}
