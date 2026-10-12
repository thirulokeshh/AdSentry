package com.adsentry.importing.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImportPreviewResponse {

    private String filename;
    private String detectedPlatform;
    private double platformConfidence;
    private String detectedCurrency;
    private boolean currencyConfirmed;
    private Map<String, String> columnMappings;
    private LocalDate reportingStartDate;
    private LocalDate reportingEndDate;
    private int totalRows;
    private int estimatedAcceptedRows;
    private int estimatedRejectedRows;
    private int estimatedDuplicateOrUpdatedRows;
    private List<String> warnings = new ArrayList<>();
    private List<RowPreviewDto> sampleAcceptedRows = new ArrayList<>();
    private List<RowErrorPreviewDto> sampleRejectedRows = new ArrayList<>();

    public ImportPreviewResponse() {
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDetectedPlatform() {
        return detectedPlatform;
    }

    public void setDetectedPlatform(String detectedPlatform) {
        this.detectedPlatform = detectedPlatform;
    }

    public double getPlatformConfidence() {
        return platformConfidence;
    }

    public void setPlatformConfidence(double platformConfidence) {
        this.platformConfidence = platformConfidence;
    }

    public String getDetectedCurrency() {
        return detectedCurrency;
    }

    public void setDetectedCurrency(String detectedCurrency) {
        this.detectedCurrency = detectedCurrency;
    }

    public boolean isCurrencyConfirmed() {
        return currencyConfirmed;
    }

    public void setCurrencyConfirmed(boolean currencyConfirmed) {
        this.currencyConfirmed = currencyConfirmed;
    }

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public void setColumnMappings(Map<String, String> columnMappings) {
        this.columnMappings = columnMappings;
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

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getEstimatedAcceptedRows() {
        return estimatedAcceptedRows;
    }

    public void setEstimatedAcceptedRows(int estimatedAcceptedRows) {
        this.estimatedAcceptedRows = estimatedAcceptedRows;
    }

    public int getEstimatedRejectedRows() {
        return estimatedRejectedRows;
    }

    public void setEstimatedRejectedRows(int estimatedRejectedRows) {
        this.estimatedRejectedRows = estimatedRejectedRows;
    }

    public int getEstimatedDuplicateOrUpdatedRows() {
        return estimatedDuplicateOrUpdatedRows;
    }

    public void setEstimatedDuplicateOrUpdatedRows(int estimatedDuplicateOrUpdatedRows) {
        this.estimatedDuplicateOrUpdatedRows = estimatedDuplicateOrUpdatedRows;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<RowPreviewDto> getSampleAcceptedRows() {
        return sampleAcceptedRows;
    }

    public void setSampleAcceptedRows(List<RowPreviewDto> sampleAcceptedRows) {
        this.sampleAcceptedRows = sampleAcceptedRows;
    }

    public List<RowErrorPreviewDto> getSampleRejectedRows() {
        return sampleRejectedRows;
    }

    public void setSampleRejectedRows(List<RowErrorPreviewDto> sampleRejectedRows) {
        this.sampleRejectedRows = sampleRejectedRows;
    }
}
