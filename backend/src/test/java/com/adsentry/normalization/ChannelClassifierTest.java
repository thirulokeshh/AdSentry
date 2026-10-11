package com.adsentry.normalization;

import com.adsentry.normalization.domain.ChannelClassificationResult;
import com.adsentry.normalization.domain.ChannelClassifier;
import com.adsentry.normalization.domain.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelClassifierTest {

    private ChannelClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new ChannelClassifier();
    }

    @Test
    @DisplayName("Supported channels should map to Google Ads and Meta Ads")
    void testSupportedChannels() {
        assertThat(classifier.isSupported("Google Ads")).isTrue();
        assertThat(classifier.isSupported("Meta Ads")).isTrue();
        assertThat(classifier.isSupported("Facebook Ads")).isTrue();

        assertThat(classifier.classify("Google Ads")).isEqualTo(Platform.GOOGLE_ADS);
        assertThat(classifier.classify("Meta Ads")).isEqualTo(Platform.META_ADS);
        assertThat(classifier.classify("Facebook Ads")).isEqualTo(Platform.META_ADS);
    }

    @Test
    @DisplayName("Unsupported channels in marketing data must be detected and flagged")
    void testUnsupportedChannels() {
        assertThat(classifier.isSupported("Organic")).isFalse();
        assertThat(classifier.isSupported("Email")).isFalse();
        assertThat(classifier.isSupported("TikTok")).isFalse();
        assertThat(classifier.isSupported("LinkedIn")).isFalse();
        assertThat(classifier.isSupported("Affiliate")).isFalse();

        assertThat(classifier.classify("Organic")).isEqualTo(Platform.UNSUPPORTED);
        assertThat(classifier.classify("TikTok")).isEqualTo(Platform.UNSUPPORTED);
    }

    @Test
    @DisplayName("Audit and report channels from the actual sample-marketing.csv")
    void testAuditSampleMarketingCsv() throws Exception {
        // Find sample-marketing.csv either in classpath or from parent directory
        Path samplePath = Paths.get("..", "sample-marketing.csv");
        if (!Files.exists(samplePath)) {
            samplePath = Paths.get("sample-marketing.csv");
        }

        assertThat(Files.exists(samplePath)).isTrue();

        List<String> channels = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(samplePath)) {
            String header = reader.readLine(); // date,channel,campaign,impressions,clicks,spend,conversions,revenue
            assertThat(header).isNotNull();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts.length > 1) {
                    channels.add(parts[1].trim());
                }
            }
        }

        ChannelClassificationResult result = classifier.summarizeChannels(channels);

        assertThat(result.totalRecords()).isEqualTo(260);
        assertThat(result.supportedRecords()).isEqualTo(60); // 34 Google Ads + 26 Meta Ads
        assertThat(result.unsupportedRecords()).isEqualTo(200); // 41 Organic + 42 Email + 41 TikTok + 49 LinkedIn + 27 Affiliate

        assertThat(result.supportedBreakdown()).containsEntry("Google Ads", 34L);
        assertThat(result.supportedBreakdown()).containsEntry("Meta Ads", 26L);

        assertThat(result.unsupportedChannelNames()).containsExactlyInAnyOrder(
                "Organic", "Email", "TikTok", "LinkedIn", "Affiliate"
        );
        assertThat(result.unsupportedBreakdown()).containsEntry("Organic", 41L);
        assertThat(result.unsupportedBreakdown()).containsEntry("Email", 42L);
        assertThat(result.unsupportedBreakdown()).containsEntry("TikTok", 41L);
        assertThat(result.unsupportedBreakdown()).containsEntry("LinkedIn", 49L);
        assertThat(result.unsupportedBreakdown()).containsEntry("Affiliate", 27L);
    }
}
