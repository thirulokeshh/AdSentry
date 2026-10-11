package com.adsentry.normalization.domain;

import java.util.Map;
import java.util.Set;

public record ChannelClassificationResult(
        long totalRecords,
        long supportedRecords,
        long unsupportedRecords,
        Map<String, Long> supportedBreakdown,
        Map<String, Long> unsupportedBreakdown,
        Set<String> unsupportedChannelNames
) {}
