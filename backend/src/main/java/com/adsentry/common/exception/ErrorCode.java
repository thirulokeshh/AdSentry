package com.adsentry.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST("Invalid request parameters or payload", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("Validation failed for input data", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_CURRENCY("Unsupported currency. Only INR is supported in MVP", HttpStatus.BAD_REQUEST),
    CURRENCY_AMBIGUITY("Currency is ambiguous or contains conflicting currency indicators", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_CHANNEL("Unsupported advertising channel detected", HttpStatus.BAD_REQUEST),
    AMBIGUOUS_DATE_FORMAT("Ambiguous date format detected; explicit confirmation required", HttpStatus.BAD_REQUEST),
    INVALID_DATE("Invalid or unparseable reporting date", HttpStatus.BAD_REQUEST),
    MALFORMED_ROW("Malformed row structure or unparseable row data", HttpStatus.BAD_REQUEST),
    NEGATIVE_VALUE("Metric value cannot be negative", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_COLUMN("Required column is missing from CSV mapping", HttpStatus.BAD_REQUEST),
    INCOMPATIBLE_CONVERSION_DEFINITION("Conversion definition is incompatible or unspecified", HttpStatus.BAD_REQUEST),
    BATCH_NOT_FOUND("The requested import batch was not found", HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND("The requested resource was not found", HttpStatus.NOT_FOUND),
    SETTING_NOT_FOUND("The requested application setting was not found", HttpStatus.NOT_FOUND),
    RECOMMENDATION_NOT_FOUND("The requested recommendation was not found", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("An unexpected internal error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String defaultMessage, HttpStatus httpStatus) {
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
