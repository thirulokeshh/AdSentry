package com.adsentry.analytics.dto;

import java.util.ArrayList;
import java.util.List;

public class MetricsCalculationResult {

    private MetricSummaryDto metrics;
    private List<String> warnings = new ArrayList<>();

    public MetricsCalculationResult() {
    }

    public MetricsCalculationResult(MetricSummaryDto metrics, List<String> warnings) {
        this.metrics = metrics;
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public MetricSummaryDto getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricSummaryDto metrics) {
        this.metrics = metrics;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
}
