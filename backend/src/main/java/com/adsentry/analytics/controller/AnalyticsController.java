package com.adsentry.analytics.controller;

import com.adsentry.analytics.dto.CampaignPerformanceResponse;
import com.adsentry.analytics.dto.DataCoverageResponse;
import com.adsentry.analytics.dto.OverviewMetricsResponse;
import com.adsentry.analytics.dto.PeriodComparisonResponse;
import com.adsentry.analytics.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<OverviewMetricsResponse> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String platform) {
        OverviewMetricsResponse response = analyticsService.getOverview(startDate, endDate, platform);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/campaigns")
    public ResponseEntity<CampaignPerformanceResponse> getCampaigns(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String platform) {
        CampaignPerformanceResponse response = analyticsService.getCampaignPerformance(startDate, endDate, platform);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comparison")
    public ResponseEntity<PeriodComparisonResponse> getComparison(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String platform) {
        PeriodComparisonResponse response = analyticsService.getComparison(startDate, endDate, platform);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/coverage")
    public ResponseEntity<DataCoverageResponse> getCoverage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String platform) {
        DataCoverageResponse response = analyticsService.getDataCoverage(startDate, endDate, platform);
        return ResponseEntity.ok(response);
    }
}
