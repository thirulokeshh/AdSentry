package com.adsentry.demo.service;

import com.adsentry.analytics.dto.CampaignPerformanceDto;
import com.adsentry.analytics.dto.CampaignPerformanceResponse;
import com.adsentry.analytics.dto.MetricSummaryDto;
import com.adsentry.analytics.dto.OverviewMetricsResponse;
import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RecommendationStatus;
import com.adsentry.recommendation.domain.RuleIdentifier;
import com.adsentry.recommendation.dto.RecommendationDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Isolated demo mode service.
 * Returns curated, realistic marketing figures and recommendations without touching
 * database repositories or contaminating live imports and historical records.
 */
@Service
public class DemoService {

    private static final LocalDate DEMO_START = LocalDate.of(2026, 3, 1);
    private static final LocalDate DEMO_END = LocalDate.of(2026, 3, 31);

    public OverviewMetricsResponse getDemoOverview() {
        OverviewMetricsResponse overview = new OverviewMetricsResponse();
        overview.setCurrency("INR");
        overview.setStartDate(DEMO_START);
        overview.setEndDate(DEMO_END);
        overview.setPlatformFilter("ALL");
        overview.setTotalCampaigns(5);
        overview.setTotalRecords(155);

        MetricSummaryDto summary = new MetricSummaryDto(
                new BigDecimal("148250.00"), // Spend
                385000L,                    // Impressions
                14200L,                     // Clicks
                new BigDecimal("345"),      // Conversions
                "PURCHASE",                 // Conversion definition
                new BigDecimal("3.69"),     // CTR (%)
                new BigDecimal("10.44"),    // CPC (INR)
                new BigDecimal("429.71"),   // CPA (INR)
                new BigDecimal("2.43"),     // Conversion Rate (%)
                new BigDecimal("385.06")    // CPM (INR)
        );
        overview.setMetrics(summary);
        overview.setWarnings(List.of("Demo mode active — all metrics are simulated reference data."));
        return overview;
    }

    public CampaignPerformanceResponse getDemoCampaigns() {
        CampaignPerformanceResponse response = new CampaignPerformanceResponse();
        response.setCurrency("INR");
        response.setStartDate(DEMO_START);
        response.setEndDate(DEMO_END);
        response.setPlatformFilter("ALL");
        response.setTotalCampaigns(5);
        response.setWarnings(List.of("Demo mode active — campaigns are simulated reference figures."));

        List<CampaignPerformanceDto> list = new ArrayList<>();

        // Campaign 1: Brand Search Core (Google)
        list.add(new CampaignPerformanceDto(
                "google_ads_brand_core_in",
                "Brand_Search_Core_IN",
                "Google Ads",
                "HIGH_ID",
                31,
                new MetricSummaryDto(
                        new BigDecimal("32000.00"), 68000L, 4100L,
                        new BigDecimal("175"), "PURCHASE",
                        new BigDecimal("6.03"), new BigDecimal("7.80"),
                        new BigDecimal("182.86"), new BigDecimal("4.27"),
                        new BigDecimal("470.59")
                ),
                List.of()
        ));

        // Campaign 2: Summer Promo Retargeting (Meta)
        list.add(new CampaignPerformanceDto(
                "meta_ads_summer_promo_retargeting",
                "Summer_Promo_Retargeting",
                "Meta Ads",
                "HIGH_ID",
                30,
                new MetricSummaryDto(
                        new BigDecimal("44500.00"), 142000L, 5200L,
                        new BigDecimal("105"), "PURCHASE",
                        new BigDecimal("3.66"), new BigDecimal("8.56"),
                        new BigDecimal("423.81"), new BigDecimal("2.02"),
                        new BigDecimal("313.38")
                ),
                List.of()
        ));

        // Campaign 3: Competitor Keywords Test (Google) - Triggers Rule A (Zero conversions)
        list.add(new CampaignPerformanceDto(
                "google_ads_competitor_keywords_test",
                "Competitor_Keywords_Test",
                "Google Ads",
                "HIGH_ID",
                22,
                new MetricSummaryDto(
                        new BigDecimal("8750.00"), 25000L, 780L,
                        BigDecimal.ZERO, "PURCHASE",
                        new BigDecimal("3.12"), new BigDecimal("11.22"),
                        null, BigDecimal.ZERO, // CPA null because 0 conversions
                        new BigDecimal("350.00")
                ),
                List.of("Zero conversions recorded for entire reporting period.")
        ));

        // Campaign 4: Broad Lookalike Prospecting (Meta) - Triggers Rule B (High CPA)
        list.add(new CampaignPerformanceDto(
                "meta_ads_broad_lookalike_prospecting",
                "Broad_Lookalike_Prospecting",
                "Meta Ads",
                "HIGH_ID",
                28,
                new MetricSummaryDto(
                        new BigDecimal("42000.00"), 110000L, 2900L,
                        new BigDecimal("28"), "PURCHASE",
                        new BigDecimal("2.64"), new BigDecimal("14.48"),
                        new BigDecimal("1500.00"), new BigDecimal("0.97"),
                        new BigDecimal("381.82")
                ),
                List.of("CPA (₹1,500.00) is 3x higher than target threshold (₹500.00).")
        ));

        // Campaign 5: Discovery Catalog Feed (Google)
        list.add(new CampaignPerformanceDto(
                "google_ads_discovery_catalog_feed",
                "Discovery_Catalog_Feed",
                "Google Ads",
                "HIGH_ID",
                25,
                new MetricSummaryDto(
                        new BigDecimal("21000.00"), 40000L, 1220L,
                        new BigDecimal("37"), "PURCHASE",
                        new BigDecimal("3.05"), new BigDecimal("17.21"),
                        new BigDecimal("567.57"), new BigDecimal("3.03"),
                        new BigDecimal("525.00")
                ),
                List.of()
        ));

        response.setCampaigns(list);
        return response;
    }

    public List<RecommendationDto> getDemoRecommendations() {
        List<RecommendationDto> recommendations = new ArrayList<>();

        // Recommendation 1: Rule A
        RecommendationDto rec1 = new RecommendationDto(
                101L,
                "demo_fingerprint_rule_a_competitor",
                RuleIdentifier.RULE_A_ZERO_CONVERSIONS,
                RuleIdentifier.RULE_A_ZERO_CONVERSIONS.getDescription(),
                "Google Ads",
                "Competitor_Keywords_Test",
                "google_ads_competitor_keywords_test",
                "HIGH_ID",
                DEMO_START,
                DEMO_END,
                RecommendationPriority.HIGH,
                "Zero Conversions on Competitor_Keywords_Test (Spend: ₹8,750.00)",
                "Campaign spent ₹8,750.00 across 22 active days with confirmed zero conversions, exceeding the minimum spend threshold of ₹1,000.00.",
                "Review search terms report and pause non-converting competitor keywords, or verify conversion tracking tags on destination URLs.",
                Map.of(
                        "spend", new BigDecimal("8750.00"),
                        "conversions", BigDecimal.ZERO,
                        "clicks", 780,
                        "impressions", 25000
                ),
                "Spend >= minSpendThreshold (₹1,000.00) and conversions == 0",
                Map.of(
                        "minSpendThreshold", new BigDecimal("1000.00"),
                        "globalCpaTarget", new BigDecimal("500.00"),
                        "priorityHighSpendCutoff", new BigDecimal("5000.00")
                ),
                RecommendationStatus.NEW,
                Instant.parse("2026-03-31T10:00:00Z"),
                Instant.parse("2026-03-31T10:00:00Z")
        );
        recommendations.add(rec1);

        // Recommendation 2: Rule B
        RecommendationDto rec2 = new RecommendationDto(
                102L,
                "demo_fingerprint_rule_b_lookalike",
                RuleIdentifier.RULE_B_HIGH_CPA,
                RuleIdentifier.RULE_B_HIGH_CPA.getDescription(),
                "Meta Ads",
                "Broad_Lookalike_Prospecting",
                "meta_ads_broad_lookalike_prospecting",
                "HIGH_ID",
                DEMO_START,
                DEMO_END,
                RecommendationPriority.MEDIUM,
                "High CPA on Broad_Lookalike_Prospecting (₹1,500.00 vs ₹500.00 target)",
                "Campaign CPA of ₹1,500.00 is 3.0x higher than target CPA (₹500.00). Total spend is ₹42,000.00 with 28 conversions.",
                "Narrow audience targeting to top-performing demographics/regions or test fresh ad creatives to improve click-to-conversion rates.",
                Map.of(
                        "spend", new BigDecimal("42000.00"),
                        "conversions", new BigDecimal("28"),
                        "cpa", new BigDecimal("1500.00"),
                        "globalCpaTarget", new BigDecimal("500.00")
                ),
                "CPA (₹1,500.00) > globalCpaTarget (₹500.00)",
                Map.of(
                        "globalCpaTarget", new BigDecimal("500.00"),
                        "minSpendThreshold", new BigDecimal("1000.00"),
                        "priorityHighCpaMultiplier", new BigDecimal("2.0")
                ),
                RecommendationStatus.REVIEWED,
                Instant.parse("2026-03-31T10:00:00Z"),
                Instant.parse("2026-03-31T11:15:00Z")
        );
        recommendations.add(rec2);

        return recommendations;
    }
}
