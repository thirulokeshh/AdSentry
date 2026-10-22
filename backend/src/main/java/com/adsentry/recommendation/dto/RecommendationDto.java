package com.adsentry.recommendation.dto;

import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RecommendationStatus;
import com.adsentry.recommendation.domain.RuleIdentifier;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public class RecommendationDto {

    private Long id;
    private String fingerprint;
    private RuleIdentifier ruleIdentifier;
    private String ruleDescription;
    private String platform;
    private String campaignName;
    private String campaignIdentityKey;
    private String identityReliability;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private RecommendationPriority priority;
    private String title;
    private String explanation;
    private String suggestedAction;
    private Map<String, Object> supportingMetrics;
    private String triggerCondition;
    private Map<String, Object> thresholdsUsed;
    private RecommendationStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public RecommendationDto() {
    }

    public RecommendationDto(Long id,
                             String fingerprint,
                             RuleIdentifier ruleIdentifier,
                             String ruleDescription,
                             String platform,
                             String campaignName,
                             String campaignIdentityKey,
                             String identityReliability,
                             LocalDate periodStart,
                             LocalDate periodEnd,
                             RecommendationPriority priority,
                             String title,
                             String explanation,
                             String suggestedAction,
                             Map<String, Object> supportingMetrics,
                             String triggerCondition,
                             Map<String, Object> thresholdsUsed,
                             RecommendationStatus status,
                             Instant createdAt,
                             Instant updatedAt) {
        this.id = id;
        this.fingerprint = fingerprint;
        this.ruleIdentifier = ruleIdentifier;
        this.ruleDescription = ruleDescription;
        this.platform = platform;
        this.campaignName = campaignName;
        this.campaignIdentityKey = campaignIdentityKey;
        this.identityReliability = identityReliability;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.priority = priority;
        this.title = title;
        this.explanation = explanation;
        this.suggestedAction = suggestedAction;
        this.supportingMetrics = supportingMetrics;
        this.triggerCondition = triggerCondition;
        this.thresholdsUsed = thresholdsUsed;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getRuleDescription() {
        return ruleDescription;
    }

    public void setRuleDescription(String ruleDescription) {
        this.ruleDescription = ruleDescription;
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

    public Map<String, Object> getSupportingMetrics() {
        return supportingMetrics;
    }

    public void setSupportingMetrics(Map<String, Object> supportingMetrics) {
        this.supportingMetrics = supportingMetrics;
    }

    public String getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(String triggerCondition) {
        this.triggerCondition = triggerCondition;
    }

    public Map<String, Object> getThresholdsUsed() {
        return thresholdsUsed;
    }

    public void setThresholdsUsed(Map<String, Object> thresholdsUsed) {
        this.thresholdsUsed = thresholdsUsed;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
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
