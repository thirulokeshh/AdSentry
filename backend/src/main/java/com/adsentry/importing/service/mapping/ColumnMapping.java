package com.adsentry.importing.service.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnMapping {

    private String dateColumn;
    private String channelColumn;
    private String campaignNameColumn;
    private String campaignIdColumn;
    private String spendColumn;
    private String impressionsColumn;
    private String clicksColumn;
    private String conversionsColumn;
    private String revenueColumn;
    private List<String> dimensionColumns = new ArrayList<>();

    public ColumnMapping() {
    }

    public String getDateColumn() {
        return dateColumn;
    }

    public void setDateColumn(String dateColumn) {
        this.dateColumn = dateColumn;
    }

    public String getChannelColumn() {
        return channelColumn;
    }

    public void setChannelColumn(String channelColumn) {
        this.channelColumn = channelColumn;
    }

    public String getCampaignNameColumn() {
        return campaignNameColumn;
    }

    public void setCampaignNameColumn(String campaignNameColumn) {
        this.campaignNameColumn = campaignNameColumn;
    }

    public String getCampaignIdColumn() {
        return campaignIdColumn;
    }

    public void setCampaignIdColumn(String campaignIdColumn) {
        this.campaignIdColumn = campaignIdColumn;
    }

    public String getSpendColumn() {
        return spendColumn;
    }

    public void setSpendColumn(String spendColumn) {
        this.spendColumn = spendColumn;
    }

    public String getImpressionsColumn() {
        return impressionsColumn;
    }

    public void setImpressionsColumn(String impressionsColumn) {
        this.impressionsColumn = impressionsColumn;
    }

    public String getClicksColumn() {
        return clicksColumn;
    }

    public void setClicksColumn(String clicksColumn) {
        this.clicksColumn = clicksColumn;
    }

    public String getConversionsColumn() {
        return conversionsColumn;
    }

    public void setConversionsColumn(String conversionsColumn) {
        this.conversionsColumn = conversionsColumn;
    }

    public String getRevenueColumn() {
        return revenueColumn;
    }

    public void setRevenueColumn(String revenueColumn) {
        this.revenueColumn = revenueColumn;
    }

    public List<String> getDimensionColumns() {
        return dimensionColumns;
    }

    public void setDimensionColumns(List<String> dimensionColumns) {
        this.dimensionColumns = dimensionColumns != null ? dimensionColumns : new ArrayList<>();
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        if (dateColumn != null) map.put("date", dateColumn);
        if (channelColumn != null) map.put("channel", channelColumn);
        if (campaignNameColumn != null) map.put("campaignName", campaignNameColumn);
        if (campaignIdColumn != null) map.put("campaignId", campaignIdColumn);
        if (spendColumn != null) map.put("spend", spendColumn);
        if (impressionsColumn != null) map.put("impressions", impressionsColumn);
        if (clicksColumn != null) map.put("clicks", clicksColumn);
        if (conversionsColumn != null) map.put("conversions", conversionsColumn);
        if (revenueColumn != null) map.put("revenue", revenueColumn);
        if (!dimensionColumns.isEmpty()) map.put("dimensions", String.join(",", dimensionColumns));
        return map;
    }

    public String toJson(ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
