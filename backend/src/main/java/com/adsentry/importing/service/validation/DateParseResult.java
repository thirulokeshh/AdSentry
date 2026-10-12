package com.adsentry.importing.service.validation;

import java.time.LocalDate;

public class DateParseResult {

    private final LocalDate date;
    private final boolean ambiguous;
    private final boolean valid;
    private final String errorMessage;

    public static DateParseResult success(LocalDate date) {
        return new DateParseResult(date, false, true, null);
    }

    public static DateParseResult ambiguous(String raw) {
        return new DateParseResult(null, true, false,
                "Ambiguous date format for value '" + raw + "'. Could be DD/MM/YYYY or MM/DD/YYYY. Please specify explicit date format.");
    }

    public static DateParseResult invalid(String raw, String reason) {
        return new DateParseResult(null, false, false,
                "Invalid reporting date '" + raw + "': " + reason);
    }

    private DateParseResult(LocalDate date, boolean ambiguous, boolean valid, String errorMessage) {
        this.date = date;
        this.ambiguous = ambiguous;
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isAmbiguous() {
        return ambiguous;
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
