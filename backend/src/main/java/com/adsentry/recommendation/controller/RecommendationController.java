package com.adsentry.recommendation.controller;

import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RecommendationStatus;
import com.adsentry.recommendation.domain.RuleIdentifier;
import com.adsentry.recommendation.dto.AnalysisRunRequest;
import com.adsentry.recommendation.dto.AnalysisRunResponse;
import com.adsentry.recommendation.dto.RecommendationDto;
import com.adsentry.recommendation.dto.UpdateRecommendationStatusRequest;
import com.adsentry.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationDto>> getRecommendations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) RecommendationStatus status,
            @RequestParam(required = false) RecommendationPriority priority,
            @RequestParam(required = false) RuleIdentifier rule) {
        List<RecommendationDto> recommendations = recommendationService.getRecommendations(
                startDate, endDate, platform, status, priority, rule
        );
        return ResponseEntity.ok(recommendations);
    }

    @PatchMapping("/recommendations/{id}/status")
    public ResponseEntity<RecommendationDto> updateRecommendationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecommendationStatusRequest request) {
        RecommendationDto updated = recommendationService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/analysis/run")
    public ResponseEntity<AnalysisRunResponse> runAnalysis(
            @Valid @RequestBody(required = false) AnalysisRunRequest request) {
        AnalysisRunResponse response = recommendationService.runAnalysis(
                request != null ? request : new AnalysisRunRequest()
        );
        return ResponseEntity.ok(response);
    }
}
