package com.adsentry.recommendation.repository;

import com.adsentry.recommendation.domain.Recommendation;
import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RecommendationStatus;
import com.adsentry.recommendation.domain.RuleIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    Optional<Recommendation> findByFingerprint(String fingerprint);
    List<Recommendation> findByStatus(RecommendationStatus status);
    List<Recommendation> findByPriority(RecommendationPriority priority);
    List<Recommendation> findByRuleIdentifier(RuleIdentifier ruleIdentifier);
    List<Recommendation> findByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(LocalDate start, LocalDate end);
}
