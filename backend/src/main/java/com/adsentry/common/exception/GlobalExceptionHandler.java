package com.adsentry.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetailResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request
    ) {
        log.warn("API Exception [{}]: {}", ex.getErrorCode().name(), ex.getMessage());
        ProblemDetailResponse response = ProblemDetailResponse.of(
                ex.getHttpStatus().getReasonPhrase(),
                ex.getHttpStatus().value(),
                ex.getMessage(),
                request.getRequestURI(),
                ex.getErrorCode().name(),
                ex.getErrors()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorItem> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ValidationErrorItem(
                        fieldError.getField(),
                        fieldError.getRejectedValue(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        log.warn("Validation failed on {}: {} field error(s)", request.getRequestURI(), validationErrors.size());
        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                HttpStatus.BAD_REQUEST.value(),
                "Input validation failed. Please check the 'errors' array for details.",
                request.getRequestURI(),
                ErrorCode.VALIDATION_FAILED.name(),
                validationErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetailResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorItem> validationErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ValidationErrorItem(
                        violation.getPropertyPath().toString(),
                        violation.getInvalidValue(),
                        violation.getMessage()
                ))
                .toList();

        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                HttpStatus.BAD_REQUEST.value(),
                "Constraint violation occurred.",
                request.getRequestURI(),
                ErrorCode.VALIDATION_FAILED.name(),
                validationErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetailResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Malformed JSON payload on {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON request body.",
                request.getRequestURI(),
                ErrorCode.INVALID_REQUEST.name(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetailResponse> handleNotFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                HttpStatus.NOT_FOUND.value(),
                "Endpoint resource was not found: " + ex.getResourcePath(),
                request.getRequestURI(),
                ErrorCode.RESOURCE_NOT_FOUND.name(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        // Internal errors are logged with full stack trace, but sanitized for the client
        log.error("Unhandled internal server error on {}", request.getRequestURI(), ex);
        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected internal error occurred. Please contact support or check server logs.",
                request.getRequestURI(),
                ErrorCode.INTERNAL_SERVER_ERROR.name(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
