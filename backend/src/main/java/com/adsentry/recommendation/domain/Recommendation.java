package com.adsentry.recommendation.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_identifier", nullable = false, length = 50)
    private RuleIdentifier ruleIdentifier;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(name = "campaign_name", nullable = false)
    private String campaignName;

    @Column(name = "campaign_identity_key", nullable = false, length = 128)
    private String campaignIdentityKey;

    @Column(name = "identity_reliability", nullable = false, length = 20)
    private String identityReliability;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationPriority priority;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "suggested_action", nullable = false, columnDefinition = "TEXT")
    private String suggestedAction;

    @Column(name = "supporting_metrics_json", nullable = false, columnDefinition = "TEXT")
    private String supportingMetricsJson;

    @Column(name = "trigger_condition", nullable = false, columnDefinition = "TEXT")
    private String triggerCondition;

    @Column(name = "thresholds_used_json", nullable = false, columnDefinition = "TEXT")
    private String thresholdsUsedJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status = RecommendationStatus.NEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Recommendation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public RuleIdentifier getRuleIdentifier() {
        return ruleIdentifier;
    }

    public void setRuleIdentifier(RuleIdentifier ruleIdentifier) {
        this.ruleIdentifier = ruleIdentifier;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public String getCampaignIdentityKey() {
        return campaignIdentityKey;
    }

    public void setCampaignIdentityKey(String campaignIdentityKey) {
        this.campaignIdentityKey = campaignIdentityKey;
    }

    public String getIdentityReliability() {
        return identityReliability;
    }

    public void setIdentityReliability(String identityReliability) {
        this.identityReliability = identityReliability;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public RecommendationPriority getPriority() {
        return priority;
    }

    public void setPriority(RecommendationPriority priority) {
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    public String getSupportingMetricsJson() {
        return supportingMetricsJson;
    }

    public void setSupportingMetricsJson(String supportingMetricsJson) {
        this.supportingMetricsJson = supportingMetricsJson;
    }

    public String getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(String triggerCondition) {
        this.triggerCondition = triggerCondition;
    }

    public String getThresholdsUsedJson() {
        return thresholdsUsedJson;
    }

    public void setThresholdsUsedJson(String thresholdsUsedJson) {
        this.thresholdsUsedJson = thresholdsUsedJson;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
