package com.adsentry.recommendation.event;

public class AnalysisRecalculationEvent {

    private final String reason;

    public AnalysisRecalculationEvent(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
