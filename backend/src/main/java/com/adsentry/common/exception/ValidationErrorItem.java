package com.adsentry.common.exception;

public record ValidationErrorItem(
        String field,
        Object rejectedValue,
        String message
) {}
