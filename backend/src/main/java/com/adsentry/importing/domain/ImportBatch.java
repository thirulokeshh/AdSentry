package com.adsentry.importing.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "import_batches")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "import_timestamp", nullable = false)
    private Instant importTimestamp;

    @Column(name = "reporting_start_date")
    private LocalDate reportingStartDate;

    @Column(name = "reporting_end_date")
    private LocalDate reportingEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchStatus status;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "accepted_rows", nullable = false)
    private int acceptedRows;

    @Column(name = "rejected_rows", nullable = false)
    private int rejectedRows;

    @Column(name = "duplicate_rows", nullable = false)
    private int duplicateRows;

    @Column(name = "updated_rows", nullable = false)
    private int updatedRows;

    @Column(name = "column_mapping_json", columnDefinition = "TEXT")
    private String columnMappingJson;

    @Column(name = "validation_summary", columnDefinition = "TEXT")
    private String validationSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportedRecord> records = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportRowError> errors = new ArrayList<>();

    public ImportBatch() {
    }

    public ImportBatch(
            String platform,
            String originalFilename,
            Instant importTimestamp,
            String currency,
            BatchStatus status
    ) {
        this.platform = platform;
        this.originalFilename = originalFilename;
        this.importTimestamp = importTimestamp;
        this.currency = currency;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Instant getImportTimestamp() {
        return importTimestamp;
    }

    public void setImportTimestamp(Instant importTimestamp) {
        this.importTimestamp = importTimestamp;
    }

    public LocalDate getReportingStartDate() {
        return reportingStartDate;
    }

    public void setReportingStartDate(LocalDate reportingStartDate) {
        this.reportingStartDate = reportingStartDate;
    }

    public LocalDate getReportingEndDate() {
        return reportingEndDate;
    }

    public void setReportingEndDate(LocalDate reportingEndDate) {
        this.reportingEndDate = reportingEndDate;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getAcceptedRows() {
        return acceptedRows;
    }

    public void setAcceptedRows(int acceptedRows) {
        this.acceptedRows = acceptedRows;
    }

    public int getRejectedRows() {
        return rejectedRows;
    }

    public void setRejectedRows(int rejectedRows) {
        this.rejectedRows = rejectedRows;
    }

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public void setDuplicateRows(int duplicateRows) {
        this.duplicateRows = duplicateRows;
    }

    public int getUpdatedRows() {
        return updatedRows;
    }

    public void setUpdatedRows(int updatedRows) {
        this.updatedRows = updatedRows;
    }

    public String getColumnMappingJson() {
        return columnMappingJson;
    }

    public void setColumnMappingJson(String columnMappingJson) {
        this.columnMappingJson = columnMappingJson;
    }

    public String getValidationSummary() {
        return validationSummary;
    }

    public void setValidationSummary(String validationSummary) {
        this.validationSummary = validationSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<ImportedRecord> getRecords() {
        return records;
    }

    public void setRecords(List<ImportedRecord> records) {
        this.records = records;
    }

    public List<ImportRowError> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportRowError> errors) {
        this.errors = errors;
    }
}
