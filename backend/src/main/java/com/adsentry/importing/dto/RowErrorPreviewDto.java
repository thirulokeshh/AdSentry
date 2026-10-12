package com.adsentry.importing.dto;

public class RowErrorPreviewDto {

    private int sourceRowNumber;
    private String fieldName;
    private String errorCode;
    private String errorMessage;

    public RowErrorPreviewDto() {
    }

    public RowErrorPreviewDto(int sourceRowNumber, String fieldName, String errorCode, String errorMessage) {
        this.sourceRowNumber = sourceRowNumber;
        this.fieldName = fieldName;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getSourceRowNumber() {
        return sourceRowNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
