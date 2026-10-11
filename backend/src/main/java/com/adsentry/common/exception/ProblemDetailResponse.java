package com.adsentry.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetailResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        Instant timestamp,
        List<ValidationErrorItem> errors
) {
    public static ProblemDetailResponse of(
            String title,
            int status,
            String detail,
            String instance,
            String code,
            List<ValidationErrorItem> errors
    ) {
        return new ProblemDetailResponse(
                "about:blank",
                title,
                status,
                detail,
                instance,
                code,
                Instant.now(),
                errors
        );
    }
}
