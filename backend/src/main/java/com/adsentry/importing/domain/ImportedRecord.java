package com.adsentry.importing.domain;

import com.adsentry.normalization.domain.ConversionDefinition;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "imported_records")
public class ImportedRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;

    @Column(name = "identity_key", nullable = false, length = 128)
    private String identityKey;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(name = "campaign_id", length = 100)
    private String campaignId;

    @Column(name = "campaign_name", nullable = false)
    private String campaignName;

    @Column(name = "campaign_name_normalized", nullable = false)
    private String campaignNameNormalized;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_confidence", nullable = false, length = 20)
    private IdentityConfidence identityConfidence;

    @Column(name = "reporting_date", nullable = false)
    private LocalDate reportingDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal spend;

    @Column(nullable = false, length = 10)
    private String currency;

    private Long impressions;

    private Long clicks;

    @Column(precision = 12, scale = 2)
    private BigDecimal conversions;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversion_definition", nullable = false, length = 50)
    private ConversionDefinition conversionDefinition = ConversionDefinition.UNSPECIFIED;

    @Column(name = "dimensions_json", columnDefinition = "TEXT")
    private String dimensionsJson;

    @Column(name = "source_row_number", nullable = false)
    private int sourceRowNumber;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    public ImportedRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImportBatch getBatch() {
        return batch;
    }

    public void setBatch(ImportBatch batch) {
        this.batch = batch;
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public void setIdentityKey(String identityKey) {
        this.identityKey = identityKey;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public String getCampaignNameNormalized() {
        return campaignNameNormalized;
    }

    public void setCampaignNameNormalized(String campaignNameNormalized) {
        this.campaignNameNormalized = campaignNameNormalized;
    }

    public IdentityConfidence getIdentityConfidence() {
        return identityConfidence;
    }

    public void setIdentityConfidence(IdentityConfidence identityConfidence) {
        this.identityConfidence = identityConfidence;
    }

    public LocalDate getReportingDate() {
        return reportingDate;
    }

    public void setReportingDate(LocalDate reportingDate) {
        this.reportingDate = reportingDate;
    }

    public BigDecimal getSpend() {
        return spend;
    }

    public void setSpend(BigDecimal spend) {
        this.spend = spend;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getImpressions() {
        return impressions;
    }

    public void setImpressions(Long impressions) {
        this.impressions = impressions;
    }

    public Long getClicks() {
        return clicks;
    }

    public void setClicks(Long clicks) {
        this.clicks = clicks;
    }

    public BigDecimal getConversions() {
        return conversions;
    }

    public void setConversions(BigDecimal conversions) {
        this.conversions = conversions;
    }

    public ConversionDefinition getConversionDefinition() {
        return conversionDefinition;
    }

    public void setConversionDefinition(ConversionDefinition conversionDefinition) {
        this.conversionDefinition = conversionDefinition;
    }

    public String getDimensionsJson() {
        return dimensionsJson;
    }

    public void setDimensionsJson(String dimensionsJson) {
        this.dimensionsJson = dimensionsJson;
    }

    public int getSourceRowNumber() {
        return sourceRowNumber;
    }

    public void setSourceRowNumber(int sourceRowNumber) {
        this.sourceRowNumber = sourceRowNumber;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }
}
