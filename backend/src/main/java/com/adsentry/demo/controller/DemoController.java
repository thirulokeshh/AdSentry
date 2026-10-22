package com.adsentry.demo.controller;

import com.adsentry.analytics.dto.CampaignPerformanceResponse;
import com.adsentry.analytics.dto.OverviewMetricsResponse;
import com.adsentry.demo.service.DemoService;
import com.adsentry.recommendation.dto.RecommendationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/overview")
    public ResponseEntity<OverviewMetricsResponse> getDemoOverview() {
        return ResponseEntity.ok(demoService.getDemoOverview());
    }

    @GetMapping("/campaigns")
    public ResponseEntity<CampaignPerformanceResponse> getDemoCampaigns() {
        return ResponseEntity.ok(demoService.getDemoCampaigns());
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationDto>> getDemoRecommendations() {
        return ResponseEntity.ok(demoService.getDemoRecommendations());
    }
}
