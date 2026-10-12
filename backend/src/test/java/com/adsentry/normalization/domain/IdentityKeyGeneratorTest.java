package com.adsentry.normalization.domain;

import com.adsentry.importing.domain.IdentityConfidence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityKeyGeneratorTest {

    private final IdentityKeyGenerator generator = new IdentityKeyGenerator();

    @Test
    @DisplayName("Campaign identity is stable across dates and aggregates across reporting periods")
    void campaignIdentity_stableAcrossDates() {
        String normalizedName = generator.normalizeCampaignName("Brand - 4");
        assertThat(normalizedName).isEqualTo("brand-4");

        // Stable campaign identity key without date
        String campaignKey1 = generator.buildCampaignIdentityKey(Platform.GOOGLE_ADS, null, normalizedName);
        String campaignKey2 = generator.buildCampaignIdentityKey(Platform.GOOGLE_ADS, null, normalizedName);

        assertThat(campaignKey1).isEqualTo("GOOGLE_ADS:NAME:brand-4");
        assertThat(campaignKey2).isEqualTo("GOOGLE_ADS:NAME:brand-4");
        assertThat(campaignKey1).doesNotContain("2026"); // Date is NOT part of campaign identity

        // Platform ID based identity
        String idBasedKey = generator.buildCampaignIdentityKey(Platform.GOOGLE_ADS, "987654", normalizedName);
        assertThat(idBasedKey).isEqualTo("GOOGLE_ADS:ID:987654");
        assertThat(generator.determineConfidence("987654")).isEqualTo(IdentityConfidence.HIGH_ID);
        assertThat(generator.determineConfidence(null)).isEqualTo(IdentityConfidence.LOW_NAME);
    }

    @Test
    @DisplayName("Record deduplication key includes date, dimensions, and conversion definition")
    void recordIdentity_includesDateAndDimensions() {
        String campaignKey = "GOOGLE_ADS:NAME:brand-4";
        LocalDate date1 = LocalDate.of(2026, 3, 1);
        LocalDate date2 = LocalDate.of(2026, 3, 2);

        String recordKeyDate1 = generator.buildRecordIdentityKey(campaignKey, date1, "device=mobile", ConversionDefinition.UNSPECIFIED);
        String recordKeyDate2 = generator.buildRecordIdentityKey(campaignKey, date2, "device=mobile", ConversionDefinition.UNSPECIFIED);

        // Different dates must produce distinct record deduplication keys
        assertThat(recordKeyDate1).isEqualTo("GOOGLE_ADS:NAME:brand-4|2026-03-01|device=mobile|UNSPECIFIED");
        assertThat(recordKeyDate2).isEqualTo("GOOGLE_ADS:NAME:brand-4|2026-03-02|device=mobile|UNSPECIFIED");
        assertThat(recordKeyDate1).isNotEqualTo(recordKeyDate2);

        // Same date and dimensions produce identical record key (enabling deduplication / versioning)
        String recordKeyDate1Duplicate = generator.buildRecordIdentityKey(campaignKey, date1, "device=mobile", ConversionDefinition.UNSPECIFIED);
        assertThat(recordKeyDate1Duplicate).isEqualTo(recordKeyDate1);
    }

    @Test
    @DisplayName("Normalize campaign name collapses spaces, punctuation, and hyphens safely")
    void normalizeCampaignName_cleansWhitespaceAndPunctuation() {
        assertThat(generator.normalizeCampaignName("  Brand - 4  ")).isEqualTo("brand-4");
        assertThat(generator.normalizeCampaignName("Summer_Sale__2026!!")).isEqualTo("summer-sale-2026");
        assertThat(generator.normalizeCampaignName("")).isEqualTo("unnamed-campaign");
        assertThat(generator.normalizeCampaignName(null)).isEqualTo("unnamed-campaign");
    }
}
