package com.adsentry.normalization.domain;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Component
public class ChannelClassifier {

    private static final Set<String> SUPPORTED_CHANNELS = Set.of(
            "google ads",
            "meta ads",
            "facebook ads",
            "google",
            "meta"
    );

    public boolean isSupported(String channel) {
        if (channel == null || channel.isBlank()) {
            return false;
        }
        return SUPPORTED_CHANNELS.contains(channel.trim().toLowerCase());
    }

    public Platform classify(String channel) {
        if (channel == null || channel.isBlank()) {
            return Platform.UNSUPPORTED;
        }
        String clean = channel.trim().toLowerCase();
        if (clean.contains("google")) {
            return Platform.GOOGLE_ADS;
        }
        if (clean.contains("meta") || clean.contains("facebook")) {
            return Platform.META_ADS;
        }
        return Platform.UNSUPPORTED;
    }

    public ChannelClassificationResult summarizeChannels(List<String> rawChannels) {
        long supportedCount = 0;
        long unsupportedCount = 0;
        Map<String, Long> supportedBreakdown = new HashMap<>();
        Map<String, Long> unsupportedBreakdown = new HashMap<>();
        Set<String> unsupportedNames = new TreeSet<>();

        for (String raw : rawChannels) {
            if (raw == null || raw.isBlank()) {
                unsupportedCount++;
                unsupportedBreakdown.merge("Blank / Missing", 1L, Long::sum);
                unsupportedNames.add("Blank / Missing");
                continue;
            }

            String trimmed = raw.trim();
            if (isSupported(trimmed)) {
                supportedCount++;
                supportedBreakdown.merge(trimmed, 1L, Long::sum);
            } else {
                unsupportedCount++;
                unsupportedBreakdown.merge(trimmed, 1L, Long::sum);
                unsupportedNames.add(trimmed);
            }
        }

        return new ChannelClassificationResult(
                rawChannels.size(),
                supportedCount,
                unsupportedCount,
                supportedBreakdown,
                unsupportedBreakdown,
                unsupportedNames
        );
    }
}
