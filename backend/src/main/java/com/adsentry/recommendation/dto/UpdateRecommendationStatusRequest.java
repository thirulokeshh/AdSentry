package com.adsentry.recommendation.dto;

import com.adsentry.recommendation.domain.RecommendationStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateRecommendationStatusRequest {

    @NotNull(message = "status is required")
    private RecommendationStatus status;

    public UpdateRecommendationStatusRequest() {
    }

    public UpdateRecommendationStatusRequest(RecommendationStatus status) {
        this.status = status;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }
}
