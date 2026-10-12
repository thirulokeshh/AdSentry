package com.adsentry.importing.service.mapping;

import com.adsentry.common.exception.ApiException;
import com.adsentry.common.exception.ErrorCode;
import com.adsentry.importing.service.csv.CsvParser;
import com.adsentry.importing.service.csv.CsvTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColumnMappingTest {

    private final CsvParser parser = new CsvParser();
    private final ColumnMapper mapper = new ColumnMapper();

    @Test
    @DisplayName("Auto-detect standard columns from marketing dataset headers")
    void autoDetect_standardMarketingColumns() throws IOException {
        String csv = "date,channel,campaign,impressions,clicks,spend,conversions,revenue\n2026-03-01,Google Ads,Brand-1,100,10,500,2,1000";
        CsvTable table = parser.parse(csv);

        ColumnMapping mapping = mapper.map(table, null);
        mapper.validateRequired(mapping);

        assertThat(mapping.getDateColumn()).isEqualTo("date");
        assertThat(mapping.getChannelColumn()).isEqualTo("channel");
        assertThat(mapping.getCampaignNameColumn()).isEqualTo("campaign");
        assertThat(mapping.getSpendColumn()).isEqualTo("spend");
        assertThat(mapping.getImpressionsColumn()).isEqualTo("impressions");
        assertThat(mapping.getClicksColumn()).isEqualTo("clicks");
        assertThat(mapping.getConversionsColumn()).isEqualTo("conversions");
        assertThat(mapping.getRevenueColumn()).isEqualTo("revenue");
    }

    @Test
    @DisplayName("Auto-detect Meta Ads Manager headers")
    void autoDetect_metaAdsManagerHeaders() throws IOException {
        String csv = "Day,Campaign name,Impressions,Link clicks,Amount spent (INR),Results\n2026-03-01,Spring Promo,1000,50,750.00,5";
        CsvTable table = parser.parse(csv);

        ColumnMapping mapping = mapper.map(table, null);
        mapper.validateRequired(mapping);

        assertThat(mapping.getDateColumn()).isEqualTo("Day");
        assertThat(mapping.getCampaignNameColumn()).isEqualTo("Campaign name");
        assertThat(mapping.getSpendColumn()).isEqualTo("Amount spent (INR)");
        assertThat(mapping.getImpressionsColumn()).isEqualTo("Impressions");
        assertThat(mapping.getClicksColumn()).isEqualTo("Link clicks");
        assertThat(mapping.getConversionsColumn()).isEqualTo("Results");
    }

    @Test
    @DisplayName("Reject budget column as spend: Budget must never be treated as actual spend")
    void rejectBudget_asActualSpend() throws IOException {
        String csv = "date,campaign,budget\n2026-03-01,Brand-1,50000";
        CsvTable table = parser.parse(csv);

        // Budget should not be auto-mapped to spend
        ColumnMapping mapping = mapper.map(table, null);
        assertThat(mapping.getSpendColumn()).isNull();

        // Attempting to manually map budget to spend must be strictly rejected
        assertThatThrownBy(() -> mapper.map(table, Map.of("spend", "budget")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Budget must never be treated as actual spend");
    }

    @Test
    @DisplayName("Validate required columns fails when date or spend is missing")
    void validateRequired_missingDateOrSpend() throws IOException {
        String csv = "campaign,clicks,conversions\nBrand-1,50,2";
        CsvTable table = parser.parse(csv);

        ColumnMapping mapping = mapper.map(table, null);
        assertThatThrownBy(() -> mapper.validateRequired(mapping))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MISSING_REQUIRED_COLUMN);
    }
}
