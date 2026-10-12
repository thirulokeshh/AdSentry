package com.adsentry.importing.service.mapping;

import com.adsentry.common.exception.ApiException;
import com.adsentry.common.exception.ErrorCode;
import com.adsentry.importing.service.csv.CsvTable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Automatically maps and validates CSV column headers to the canonical model.
 * Enforces rule: Never silently treat budget as actual spend.
 */
@Service
public class ColumnMapper {

    private static final Pattern DATE_PATTERN = Pattern.compile("^(date|day|ad_date|ad date|reporting_date|time)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHANNEL_PATTERN = Pattern.compile("^(channel|platform|network|publisher_platform|publisher)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAMPAIGN_NAME_PATTERN = Pattern.compile("^(campaign|campaign_name|campaign name)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAMPAIGN_ID_PATTERN = Pattern.compile("^(campaign_id|campaign id|campaignid|ad_id|adid)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEND_PATTERN = Pattern.compile("(?i)^.*(spend|cost|amount_spent|amount spent|total_spend).*$");
    private static final Pattern IMPRESSIONS_PATTERN = Pattern.compile("^(impressions|impr|views)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLICKS_PATTERN = Pattern.compile("^(clicks|link_clicks|link clicks)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONVERSIONS_PATTERN = Pattern.compile("^(conversions|results|leads|purchases|checkouts initiated|messaging conversations started)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern REVENUE_PATTERN = Pattern.compile("^(revenue|sale_amount|sales|conversion_value)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern BUDGET_PATTERN = Pattern.compile("(?i).*budget.*");

    public ColumnMapping map(CsvTable table, Map<String, String> userOverrides) {
        ColumnMapping mapping = new ColumnMapping();
        List<String> headers = table.getHeaders();

        // 1. Check for manual overrides first
        if (userOverrides != null) {
            for (Map.Entry<String, String> entry : userOverrides.entrySet()) {
                String targetField = entry.getKey().trim();
                String sourceHeader = entry.getValue() != null ? entry.getValue().trim() : "";

                if (sourceHeader.isBlank()) continue;

                if (!table.hasHeader(sourceHeader)) {
                    throw new ApiException(ErrorCode.VALIDATION_FAILED,
                            "User mapped header '" + sourceHeader + "' not found in CSV headers: " + headers);
                }

                // Never allow mapping budget to spend
                if ("spend".equalsIgnoreCase(targetField) && BUDGET_PATTERN.matcher(sourceHeader).matches()) {
                    throw new ApiException(ErrorCode.VALIDATION_FAILED,
                            "Column '" + sourceHeader + "' appears to be a budget field. Budget must never be treated as actual spend.");
                }

                switch (targetField.toLowerCase()) {
                    case "date", "reportingdate" -> mapping.setDateColumn(sourceHeader);
                    case "channel", "platform" -> mapping.setChannelColumn(sourceHeader);
                    case "campaignname", "campaign" -> mapping.setCampaignNameColumn(sourceHeader);
                    case "campaignid" -> mapping.setCampaignIdColumn(sourceHeader);
                    case "spend", "cost" -> mapping.setSpendColumn(sourceHeader);
                    case "impressions" -> mapping.setImpressionsColumn(sourceHeader);
                    case "clicks" -> mapping.setClicksColumn(sourceHeader);
                    case "conversions" -> mapping.setConversionsColumn(sourceHeader);
                    case "revenue" -> mapping.setRevenueColumn(sourceHeader);
                }
            }
        }

        // 2. Auto-detect unmapped fields
        for (String header : headers) {
            String lower = header.trim().toLowerCase();

            // Date
            if (mapping.getDateColumn() == null && DATE_PATTERN.matcher(lower).matches()) {
                mapping.setDateColumn(header);
            }
            // Channel
            if (mapping.getChannelColumn() == null && CHANNEL_PATTERN.matcher(lower).matches()) {
                mapping.setChannelColumn(header);
            }
            // Campaign ID
            if (mapping.getCampaignIdColumn() == null && CAMPAIGN_ID_PATTERN.matcher(lower).matches()) {
                mapping.setCampaignIdColumn(header);
            }
            // Campaign Name
            if (mapping.getCampaignNameColumn() == null && CAMPAIGN_NAME_PATTERN.matcher(lower).matches()) {
                mapping.setCampaignNameColumn(header);
            }
            // Spend (never match budget)
            if (mapping.getSpendColumn() == null && SPEND_PATTERN.matcher(lower).matches() && !BUDGET_PATTERN.matcher(lower).matches()) {
                mapping.setSpendColumn(header);
            }
            // Impressions
            if (mapping.getImpressionsColumn() == null && IMPRESSIONS_PATTERN.matcher(lower).matches()) {
                mapping.setImpressionsColumn(header);
            }
            // Clicks
            if (mapping.getClicksColumn() == null && CLICKS_PATTERN.matcher(lower).matches()) {
                mapping.setClicksColumn(header);
            }
            // Conversions
            if (mapping.getConversionsColumn() == null && CONVERSIONS_PATTERN.matcher(lower).matches()) {
                mapping.setConversionsColumn(header);
            }
            // Revenue
            if (mapping.getRevenueColumn() == null && REVENUE_PATTERN.matcher(lower).matches()) {
                mapping.setRevenueColumn(header);
            }
            // Dimensions
            if (isDimensionHeader(lower)) {
                if (!mapping.getDimensionColumns().contains(header)) {
                    mapping.getDimensionColumns().add(header);
                }
            }
        }

        return mapping;
    }

    public void validateRequired(ColumnMapping mapping) {
        if (mapping.getDateColumn() == null) {
            throw new ApiException(ErrorCode.MISSING_REQUIRED_COLUMN,
                    "Required reporting date column is missing from CSV. Please map the date column.");
        }
        if (mapping.getSpendColumn() == null) {
            throw new ApiException(ErrorCode.MISSING_REQUIRED_COLUMN,
                    "Required spend column is missing from CSV. Please map the actual spend column.");
        }
        if (mapping.getCampaignNameColumn() == null && mapping.getCampaignIdColumn() == null) {
            throw new ApiException(ErrorCode.MISSING_REQUIRED_COLUMN,
                    "Either campaign name or campaign ID must be mapped to establish campaign identity.");
        }
    }

    private boolean isDimensionHeader(String lower) {
        return lower.equals("device") || lower.equals("location") || lower.equals("placement") || lower.equals("keyword");
    }
}
