package com.adsentry.importing.domain;

import com.adsentry.normalization.domain.ConversionDefinition;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "effective_records_view")
public class EffectiveRecord {

    @Id
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "campaign_identity_key", nullable = false, length = 128)
    private String campaignIdentityKey;

    @Column(name = "identity_key", nullable = false, length = 255)
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
    private ConversionDefinition conversionDefinition;

    @Column(name = "dimensions_json", columnDefinition = "TEXT")
    private String dimensionsJson;

    @Column(name = "source_row_number", nullable = false)
    private int sourceRowNumber;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    public EffectiveRecord() {
    }

    public Long getId() {
        return id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public String getCampaignIdentityKey() {
        return campaignIdentityKey;
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public String getPlatform() {
        return platform;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public String getCampaignNameNormalized() {
        return campaignNameNormalized;
    }

    public IdentityConfidence getIdentityConfidence() {
        return identityConfidence;
    }

    public LocalDate getReportingDate() {
        return reportingDate;
    }

    public BigDecimal getSpend() {
        return spend;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getImpressions() {
        return impressions;
    }

    public Long getClicks() {
        return clicks;
    }

    public BigDecimal getConversions() {
        return conversions;
    }

    public ConversionDefinition getConversionDefinition() {
        return conversionDefinition;
    }

    public String getDimensionsJson() {
        return dimensionsJson;
    }

    public int getSourceRowNumber() {
        return sourceRowNumber;
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}
