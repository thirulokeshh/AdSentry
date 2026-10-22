package com.adsentry.recommendation.service;

import com.adsentry.recommendation.domain.RuleIdentifier;
import java.util.Optional;

public interface RecommendationRule {

    RuleIdentifier getIdentifier();

    Optional<RecommendationCandidate> evaluate(CampaignAnalysisContext context, RecommendationThresholds thresholds);
}
