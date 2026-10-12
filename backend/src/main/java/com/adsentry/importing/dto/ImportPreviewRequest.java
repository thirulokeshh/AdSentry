package com.adsentry.importing.dto;

import java.util.Map;

public class ImportPreviewRequest {

    private String confirmedPlatform;
    private String confirmedCurrency;
    private Map<String, String> columnMappingOverrides;
    private String dateFormat;
    private String conversionDefinition;

    public ImportPreviewRequest() {
    }

    public String getConfirmedPlatform() {
        return confirmedPlatform;
    }

    public void setConfirmedPlatform(String confirmedPlatform) {
        this.confirmedPlatform = confirmedPlatform;
    }

    public String getConfirmedCurrency() {
        return confirmedCurrency;
    }

    public void setConfirmedCurrency(String confirmedCurrency) {
        this.confirmedCurrency = confirmedCurrency;
    }

    public Map<String, String> getColumnMappingOverrides() {
        return columnMappingOverrides;
    }

    public void setColumnMappingOverrides(Map<String, String> columnMappingOverrides) {
        this.columnMappingOverrides = columnMappingOverrides;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getConversionDefinition() {
        return conversionDefinition;
    }

    public void setConversionDefinition(String conversionDefinition) {
        this.conversionDefinition = conversionDefinition;
    }
}
