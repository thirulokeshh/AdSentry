package com.adsentry.normalization.domain;

import com.adsentry.importing.domain.IdentityConfidence;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Generates stable campaign identity keys and record deduplication keys.
 *
 * Requirements:
 * 1. Campaign Identity is STABLE across dates: it aggregates across dates.
 *    Platform + (Campaign ID if available, else normalized campaign name).
 *    Date is NOT part of campaign identity.
 * 2. Record Deduplication Identity is used to deduplicate rows at the daily/record level:
 *    Campaign Identity Key + Date + Dimensions Key + Conversion Definition.
 */
@Component
public class IdentityKeyGenerator {

    private static final Pattern NON_ALPHANUMERIC_OR_HYPHEN = Pattern.compile("[^a-zA-Z0-9\\-]+");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");

    public String normalizeCampaignName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "unnamed-campaign";
        }
        String clean = rawName.trim().toLowerCase();
        String replaced = NON_ALPHANUMERIC_OR_HYPHEN.matcher(clean).replaceAll("-");
        String collapsed = MULTIPLE_HYPHENS.matcher(replaced).replaceAll("-");
        // Trim leading and trailing hyphens
        collapsed = collapsed.replaceAll("^-|-$", "");
        return collapsed.isBlank() ? "unnamed-campaign" : collapsed;
    }

    public IdentityConfidence determineConfidence(String campaignId) {
        if (campaignId != null && !campaignId.trim().isBlank()) {
            return IdentityConfidence.HIGH_ID;
        }
        return IdentityConfidence.LOW_NAME;
    }

    public String buildCampaignIdentityKey(Platform platform, String campaignId, String campaignNameNormalized) {
        String plat = (platform != null ? platform.name() : Platform.UNSUPPORTED.name());
        if (campaignId != null && !campaignId.trim().isBlank()) {
            return plat + ":ID:" + campaignId.trim();
        }
        return plat + ":NAME:" + (campaignNameNormalized != null ? campaignNameNormalized : "unnamed-campaign");
    }

    public String buildRecordIdentityKey(String campaignIdentityKey,
                                         LocalDate reportingDate,
                                         String dimensionsKey,
                                         ConversionDefinition conversionDefinition) {
        String dateStr = (reportingDate != null ? reportingDate.toString() : "nodate");
        String dimStr = (dimensionsKey != null && !dimensionsKey.isBlank()) ? dimensionsKey.trim().toLowerCase() : "default";
        String convStr = (conversionDefinition != null ? conversionDefinition.name() : ConversionDefinition.UNSPECIFIED.name());
        return campaignIdentityKey + "|" + dateStr + "|" + dimStr + "|" + convStr;
    }
}
