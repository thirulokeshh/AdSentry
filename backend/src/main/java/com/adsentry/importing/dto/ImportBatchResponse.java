package com.adsentry.importing.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImportBatchResponse {

    private Long id;
    private String platform;
    private String originalFilename;
    private Instant importTimestamp;
    private LocalDate reportingStartDate;
    private LocalDate reportingEndDate;
    private String status;
    private String currency;
    private int totalRows;
    private int acceptedRows;
    private int rejectedRows;
    private int duplicateRows;
    private int updatedRows;
    private Map<String, Object> columnMapping;
    private String validationSummary;
    private List<RowErrorPreviewDto> errors = new ArrayList<>();

    public ImportBatchResponse() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public Map<String, Object> getColumnMapping() {
        return columnMapping;
    }

    public void setColumnMapping(Map<String, Object> columnMapping) {
        this.columnMapping = columnMapping;
    }

    public String getValidationSummary() {
        return validationSummary;
    }

    public void setValidationSummary(String validationSummary) {
        this.validationSummary = validationSummary;
    }

    public List<RowErrorPreviewDto> getErrors() {
        return errors;
    }

    public void setErrors(List<RowErrorPreviewDto> errors) {
        this.errors = errors;
    }
}
