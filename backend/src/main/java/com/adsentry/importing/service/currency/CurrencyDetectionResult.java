package com.adsentry.importing.service.currency;

import java.util.ArrayList;
import java.util.List;

public class CurrencyDetectionResult {

    private final String detectedCurrency;
    private final boolean supported;
    private final boolean confirmed;
    private final boolean ambiguous;
    private final String detectionSource;
    private final List<String> warnings;
    private final String errorMessage;

    public CurrencyDetectionResult(String detectedCurrency,
                                   boolean supported,
                                   boolean confirmed,
                                   boolean ambiguous,
                                   String detectionSource,
                                   List<String> warnings,
                                   String errorMessage) {
        this.detectedCurrency = detectedCurrency;
        this.supported = supported;
        this.confirmed = confirmed;
        this.ambiguous = ambiguous;
        this.detectionSource = detectionSource;
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.errorMessage = errorMessage;
    }

    public String getDetectedCurrency() {
        return detectedCurrency;
    }

    public boolean isSupported() {
        return supported;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isAmbiguous() {
        return ambiguous;
    }

    public String getDetectionSource() {
        return detectionSource;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
