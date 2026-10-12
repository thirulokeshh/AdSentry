package com.adsentry.importing.controller;

import com.adsentry.common.exception.ApiException;
import com.adsentry.common.exception.ErrorCode;
import com.adsentry.importing.dto.ImportBatchResponse;
import com.adsentry.importing.dto.ImportConfirmRequest;
import com.adsentry.importing.dto.ImportPreviewRequest;
import com.adsentry.importing.dto.ImportPreviewResponse;
import com.adsentry.importing.dto.RowErrorPreviewDto;
import com.adsentry.importing.service.ImportPreviewService;
import com.adsentry.importing.service.ImportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportPreviewService previewService;
    private final ImportService importService;

    public ImportController(ImportPreviewService previewService, ImportService importService) {
        this.previewService = previewService;
        this.importService = importService;
    }

    /**
     * Preview CSV data via multipart upload without committing any records to database.
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportPreviewResponse> previewMultipart(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmedPlatform", required = false) String confirmedPlatform,
            @RequestParam(value = "confirmedCurrency", required = false) String confirmedCurrency,
            @RequestParam(value = "dateFormat", required = false) String dateFormat,
            @RequestParam(value = "conversionDefinition", required = false) String conversionDefinition) throws IOException {

        if (file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Uploaded CSV file cannot be empty.");
        }

        ImportPreviewRequest request = new ImportPreviewRequest();
        request.setConfirmedPlatform(confirmedPlatform);
        request.setConfirmedCurrency(confirmedCurrency);
        request.setDateFormat(dateFormat);
        request.setConversionDefinition(conversionDefinition);

        try (InputStream in = file.getInputStream()) {
            ImportPreviewResponse response = previewService.preview(file.getOriginalFilename(), in, request);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Preview CSV data via JSON payload (containing raw CSV content).
     */
    @PostMapping(value = "/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImportPreviewResponse> previewJson(@RequestBody CsvTextPayload payload) throws IOException {
        if (payload.getCsvContent() == null || payload.getCsvContent().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "CSV content cannot be empty.");
        }

        ImportPreviewRequest request = payload.getRequest() != null ? payload.getRequest() : new ImportPreviewRequest();
        try (InputStream in = new ByteArrayInputStream(payload.getCsvContent().getBytes(StandardCharsets.UTF_8))) {
            ImportPreviewResponse response = previewService.preview(payload.getFilename(), in, request);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Confirm and execute import via multipart upload.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportBatchResponse> importMultipart(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmedPlatform", required = false) String confirmedPlatform,
            @RequestParam(value = "confirmedCurrency", required = false) String confirmedCurrency,
            @RequestParam(value = "dateFormat", required = false) String dateFormat,
            @RequestParam(value = "conversionDefinition", required = false) String conversionDefinition) throws IOException {

        if (file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Uploaded CSV file cannot be empty.");
        }

        ImportConfirmRequest request = new ImportConfirmRequest();
        request.setConfirmedPlatform(confirmedPlatform);
        request.setConfirmedCurrency(confirmedCurrency);
        request.setDateFormat(dateFormat);
        request.setConversionDefinition(conversionDefinition);

        try (InputStream in = file.getInputStream()) {
            ImportBatchResponse response = importService.importBatch(file.getOriginalFilename(), in, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
    }

    /**
     * Confirm and execute import via JSON payload.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImportBatchResponse> importJson(@RequestBody CsvTextPayload payload) throws IOException {
        if (payload.getCsvContent() == null || payload.getCsvContent().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "CSV content cannot be empty.");
        }

        ImportConfirmRequest request = new ImportConfirmRequest();
        if (payload.getRequest() != null) {
            request.setConfirmedPlatform(payload.getRequest().getConfirmedPlatform());
            request.setConfirmedCurrency(payload.getRequest().getConfirmedCurrency());
            request.setColumnMappingOverrides(payload.getRequest().getColumnMappingOverrides());
            request.setDateFormat(payload.getRequest().getDateFormat());
            request.setConversionDefinition(payload.getRequest().getConversionDefinition());
        }

        try (InputStream in = new ByteArrayInputStream(payload.getCsvContent().getBytes(StandardCharsets.UTF_8))) {
            ImportBatchResponse response = importService.importBatch(payload.getFilename(), in, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
    }

    /**
     * List all imported batches.
     */
    @GetMapping
    public ResponseEntity<List<ImportBatchResponse>> listBatches() {
        return ResponseEntity.ok(importService.listBatches());
    }

    /**
     * Inspect a specific batch and summary.
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<ImportBatchResponse> getBatch(@PathVariable("batchId") Long batchId) {
        return ResponseEntity.ok(importService.getBatch(batchId));
    }

    /**
     * Atomically replace a selected batch with new CSV data.
     */
    @PutMapping(value = "/{batchId}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportBatchResponse> replaceMultipart(
            @PathVariable("batchId") Long batchId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmedPlatform", required = false) String confirmedPlatform,
            @RequestParam(value = "confirmedCurrency", required = false) String confirmedCurrency,
            @RequestParam(value = "dateFormat", required = false) String dateFormat,
            @RequestParam(value = "conversionDefinition", required = false) String conversionDefinition) throws IOException {

        ImportConfirmRequest request = new ImportConfirmRequest();
        request.setConfirmedPlatform(confirmedPlatform);
        request.setConfirmedCurrency(confirmedCurrency);
        request.setDateFormat(dateFormat);
        request.setConversionDefinition(conversionDefinition);

        try (InputStream in = file.getInputStream()) {
            ImportBatchResponse response = importService.replaceBatch(batchId, file.getOriginalFilename(), in, request);
            return ResponseEntity.ok(response);
        }
    }

    @PutMapping(value = "/{batchId}/replace", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImportBatchResponse> replaceJson(
            @PathVariable("batchId") Long batchId,
            @RequestBody CsvTextPayload payload) throws IOException {

        ImportConfirmRequest request = new ImportConfirmRequest();
        if (payload.getRequest() != null) {
            request.setConfirmedPlatform(payload.getRequest().getConfirmedPlatform());
            request.setConfirmedCurrency(payload.getRequest().getConfirmedCurrency());
            request.setColumnMappingOverrides(payload.getRequest().getColumnMappingOverrides());
            request.setDateFormat(payload.getRequest().getDateFormat());
            request.setConversionDefinition(payload.getRequest().getConversionDefinition());
        }

        try (InputStream in = new ByteArrayInputStream(payload.getCsvContent().getBytes(StandardCharsets.UTF_8))) {
            ImportBatchResponse response = importService.replaceBatch(batchId, payload.getFilename(), in, request);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Delete selected batch and restore any superseded versions.
     */
    @DeleteMapping("/{batchId}")
    public ResponseEntity<Void> deleteBatch(@PathVariable("batchId") Long batchId) {
        importService.deleteBatch(batchId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get row-level errors for a batch with pagination.
     */
    @GetMapping("/{batchId}/errors")
    public ResponseEntity<Page<RowErrorPreviewDto>> getBatchErrors(
            @PathVariable("batchId") Long batchId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(importService.getBatchErrors(batchId, pageable));
    }

    public static class CsvTextPayload {
        private String filename;
        private String csvContent;
        private ImportPreviewRequest request;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getCsvContent() {
            return csvContent;
        }

        public void setCsvContent(String csvContent) {
            this.csvContent = csvContent;
        }

        public ImportPreviewRequest getRequest() {
            return request;
        }

        public void setRequest(ImportPreviewRequest request) {
            this.request = request;
        }
    }
}
