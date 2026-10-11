package com.adsentry.importing.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "import_row_errors")
public class ImportRowError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;

    @Column(name = "source_row_number", nullable = false)
    private int sourceRowNumber;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "error_code", nullable = false, length = 50)
    private String errorCode;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    public ImportRowError() {
    }

    public ImportRowError(
            ImportBatch batch,
            int sourceRowNumber,
            String fieldName,
            String errorCode,
            String errorMessage
    ) {
        this.batch = batch;
        this.sourceRowNumber = sourceRowNumber;
        this.fieldName = fieldName;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImportBatch getBatch() {
        return batch;
    }

    public void setBatch(ImportBatch batch) {
        this.batch = batch;
    }

    public int getSourceRowNumber() {
        return sourceRowNumber;
    }

    public void setSourceRowNumber(int sourceRowNumber) {
        this.sourceRowNumber = sourceRowNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
