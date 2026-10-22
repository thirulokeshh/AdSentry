package com.adsentry.analytics.service;

import com.adsentry.analytics.dto.*;
import com.adsentry.common.exception.ApiException;
import com.adsentry.common.exception.ErrorCode;
import com.adsentry.importing.domain.EffectiveRecord;
import com.adsentry.importing.repository.EffectiveRecordRepository;
import com.adsentry.normalization.domain.Platform;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final EffectiveRecordRepository effectiveRecordRepository;
    private final MetricsCalculator metricsCalculator;

    public AnalyticsService(EffectiveRecordRepository effectiveRecordRepository,
                            MetricsCalculator metricsCalculator) {
        this.effectiveRecordRepository = effectiveRecordRepository;
        this.metricsCalculator = metricsCalculator;
    }

    public OverviewMetricsResponse getOverview(LocalDate startDate, LocalDate endDate, String platform) {
        String normalizedPlatform = resolvePlatformFilter(platform);
        DateRange range = resolveDateRange(startDate, endDate, normalizedPlatform);

        OverviewMetricsResponse response = new OverviewMetricsResponse();
        response.setCurrency("INR");
        response.setPlatformFilter(normalizedPlatform);

        if (range.isEmpty()) {
            response.setStartDate(startDate);
            response.setEndDate(endDate);
            response.setTotalCampaigns(0);
            response.setTotalRecords(0);
            MetricsCalculationResult emptyCalc = metricsCalculator.calculate(Collections.emptyList());
            response.setMetrics(emptyCalc.getMetrics());
            response.setWarnings(emptyCalc.getWarnings());
            return response;
        }

        response.setStartDate(range.start());
        response.setEndDate(range.end());

        List<EffectiveRecord> records = fetchRecords(range.start(), range.end(), normalizedPlatform);
        response.setTotalRecords(records.size());

        Set<String> uniqueCampaigns = records.stream()
                .map(EffectiveRecord::getCampaignIdentityKey)
                .collect(Collectors.toSet());
        response.setTotalCampaigns(uniqueCampaigns.size());

        MetricsCalculationResult calcResult = metricsCalculator.calculate(records);
        response.setMetrics(calcResult.getMetrics());

        List<String> warnings = new ArrayList<>(calcResult.getWarnings());

        // Coverage warning check
        long calendarDays = ChronoUnit.DAYS.between(range.start(), range.end()) + 1;
        long activeDays = records.stream().map(EffectiveRecord::getReportingDate).distinct().count();
        if (activeDays < calendarDays) {
            long missingDays = calendarDays - activeDays;
            warnings.add(String.format("%d of %d calendar days had no recorded activity. Absent dates describe observed data limitations and are not assumed to be zero.",
                    missingDays, calendarDays));
        }

        response.setWarnings(warnings);
        return response;
    }

    public CampaignPerformanceResponse getCampaignPerformance(LocalDate startDate, LocalDate endDate, String platform) {
        String normalizedPlatform = resolvePlatformFilter(platform);
        DateRange range = resolveDateRange(startDate, endDate, normalizedPlatform);

        CampaignPerformanceResponse response = new CampaignPerformanceResponse();
        response.setCurrency("INR");
        response.setPlatformFilter(normalizedPlatform);

        if (range.isEmpty()) {
            response.setStartDate(startDate);
            response.setEndDate(endDate);
            response.setTotalCampaigns(0);
            response.setCampaigns(Collections.emptyList());
            response.setWarnings(List.of("No campaign records available for the selected criteria."));
            return response;
        }

        response.setStartDate(range.start());
        response.setEndDate(range.end());

        List<EffectiveRecord> records = fetchRecords(range.start(), range.end(), normalizedPlatform);

        // Group records by stable campaign identity key
        Map<String, List<EffectiveRecord>> byCampaign = records.stream()
                .collect(Collectors.groupingBy(EffectiveRecord::getCampaignIdentityKey));

        response.setTotalCampaigns(byCampaign.size());

        long totalCalendarDays = ChronoUnit.DAYS.between(range.start(), range.end()) + 1;
        List<CampaignPerformanceDto> campaignDtos = new ArrayList<>();
        List<String> globalWarnings = new ArrayList<>();

        for (Map.Entry<String, List<EffectiveRecord>> entry : byCampaign.entrySet()) {
            String campaignIdentityKey = entry.getKey();
            List<EffectiveRecord> campRecords = entry.getValue();

            // Representative campaign properties from winning records
            EffectiveRecord first = campRecords.get(0);
            String campaignName = first.getCampaignName();
            Platform p = Platform.fromString(first.getPlatform());
            String campPlatform = (p != Platform.UNSUPPORTED) ? p.getDisplayName() : first.getPlatform();
            String identityConfidence = campRecords.stream()
                    .anyMatch(r -> "LOW".equalsIgnoreCase(String.valueOf(r.getIdentityConfidence()))) ? "LOW" : "HIGH";

            long activeDays = campRecords.stream().map(EffectiveRecord::getReportingDate).distinct().count();

            MetricsCalculationResult calcResult = metricsCalculator.calculate(campRecords);

            List<String> campWarnings = new ArrayList<>(calcResult.getWarnings());
            if (activeDays < totalCalendarDays) {
                campWarnings.add(String.format("Active on %d of %d period days.", activeDays, totalCalendarDays));
            }

            CampaignPerformanceDto dto = new CampaignPerformanceDto(
                    campaignIdentityKey,
                    campaignName,
                    campPlatform,
                    identityConfidence,
                    (int) activeDays,
                    calcResult.getMetrics(),
                    campWarnings
            );
            campaignDtos.add(dto);
        }

        // Sort by spend descending
        campaignDtos.sort((a, b) -> {
            BigDecimal spendA = (a.getMetrics() != null && a.getMetrics().getSpend() != null) ? a.getMetrics().getSpend() : BigDecimal.ZERO;
            BigDecimal spendB = (b.getMetrics() != null && b.getMetrics().getSpend() != null) ? b.getMetrics().getSpend() : BigDecimal.ZERO;
            return spendB.compareTo(spendA);
        });

        response.setCampaigns(campaignDtos);
        response.setWarnings(globalWarnings);
        return response;
    }

    public DataCoverageResponse getDataCoverage(LocalDate startDate, LocalDate endDate, String platform) {
        String normalizedPlatform = resolvePlatformFilter(platform);
        DateRange range = resolveDateRange(startDate, endDate, normalizedPlatform);

        DataCoverageResponse response = new DataCoverageResponse();
        response.setCurrency("INR");
        response.setPlatformFilter(normalizedPlatform);

        if (range.isEmpty()) {
            response.setStartDate(startDate);
            response.setEndDate(endDate);
            response.setTotalCalendarDays(0);
            response.setActiveDays(0);
            response.setMissingDays(0);
            response.setCoveragePercentage(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            response.setSufficientCoverage(false);
            response.setWarnings(List.of("No data points recorded for this dataset."));
            return response;
        }

        response.setStartDate(range.start());
        response.setEndDate(range.end());

        List<EffectiveRecord> records = fetchRecords(range.start(), range.end(), normalizedPlatform);
        int totalCalendarDays = (int) (ChronoUnit.DAYS.between(range.start(), range.end()) + 1);
        int activeDays = (int) records.stream().map(EffectiveRecord::getReportingDate).distinct().count();
        int missingDays = totalCalendarDays - activeDays;

        BigDecimal coveragePercentage = totalCalendarDays > 0
                ? BigDecimal.valueOf(activeDays).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCalendarDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        boolean sufficient = activeDays > 0 && coveragePercentage.compareTo(BigDecimal.valueOf(50)) >= 0;

        List<String> warnings = new ArrayList<>();
        if (activeDays == 0) {
            warnings.add("No data recorded for this period.");
        } else if (missingDays > 0) {
            warnings.add(String.format("%d of %d calendar days had no recorded activity. Absent dates describe observed data limitations and are not assumed to be zero.",
                    missingDays, totalCalendarDays));
        }

        if (!sufficient && activeDays > 0) {
            warnings.add(String.format("Data coverage is below 50%% (%s%%). Metrics across this period may be unreliable.",
                    coveragePercentage.toPlainString()));
        }

        response.setTotalCalendarDays(totalCalendarDays);
        response.setActiveDays(activeDays);
        response.setMissingDays(missingDays);
        response.setCoveragePercentage(coveragePercentage);
        response.setSufficientCoverage(sufficient);
        response.setWarnings(warnings);

        return response;
    }

    public PeriodComparisonResponse getComparison(LocalDate startDate, LocalDate endDate, String platform) {
        String normalizedPlatform = resolvePlatformFilter(platform);
        DateRange currentRange = resolveDateRange(startDate, endDate, normalizedPlatform);

        PeriodComparisonResponse response = new PeriodComparisonResponse();
        response.setCurrency("INR");
        response.setPlatformFilter(normalizedPlatform);

        if (currentRange.isEmpty()) {
            response.setWarnings(List.of("No data available to perform period comparison."));
            response.setComparisonReliable(false);
            return response;
        }

        int durationDays = (int) (ChronoUnit.DAYS.between(currentRange.start(), currentRange.end()) + 1);

        LocalDate prevEnd = currentRange.start().minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(durationDays - 1);

        response.setCurrentStartDate(currentRange.start());
        response.setCurrentEndDate(currentRange.end());
        response.setCurrentTotalDays(durationDays);

        response.setPreviousStartDate(prevStart);
        response.setPreviousEndDate(prevEnd);
        response.setPreviousTotalDays(durationDays);

        // Fetch records for both equal-length periods
        List<EffectiveRecord> currentRecords = fetchRecords(currentRange.start(), currentRange.end(), normalizedPlatform);
        List<EffectiveRecord> prevRecords = fetchRecords(prevStart, prevEnd, normalizedPlatform);

        int currentActiveDays = (int) currentRecords.stream().map(EffectiveRecord::getReportingDate).distinct().count();
        int prevActiveDays = (int) prevRecords.stream().map(EffectiveRecord::getReportingDate).distinct().count();

        response.setCurrentActiveDays(currentActiveDays);
        response.setPreviousActiveDays(prevActiveDays);

        List<String> warnings = new ArrayList<>();

        // Assess coverage sufficiency
        BigDecimal currentCovPct = durationDays > 0
                ? BigDecimal.valueOf(currentActiveDays).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(durationDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal prevCovPct = durationDays > 0
                ? BigDecimal.valueOf(prevActiveDays).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(durationDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        boolean reliable;
        if (currentActiveDays == 0) {
            reliable = false;
            warnings.add("Current period has no recorded data. Period comparison cannot be performed.");
        } else if (prevActiveDays == 0) {
            reliable = false;
            warnings.add(String.format("Adjacent previous period (%s to %s) has no recorded data. Period comparison is suppressed/unreliable.",
                    prevStart, prevEnd));
        } else if (currentCovPct.compareTo(BigDecimal.valueOf(50)) < 0 || prevCovPct.compareTo(BigDecimal.valueOf(50)) < 0) {
            reliable = false;
            warnings.add(String.format("Adjacent period comparison is qualified/unreliable: coverage is insufficient (current: %d/%d days [%s%%], previous: %d/%d days [%s%%]). Absent dates are not assumed to be zero.",
                    currentActiveDays, durationDays, currentCovPct, prevActiveDays, durationDays, prevCovPct));
        } else {
            reliable = true;
            if (currentActiveDays < durationDays || prevActiveDays < durationDays) {
                warnings.add(String.format("Coverage contains dates without recorded activity (current: %d/%d, previous: %d/%d). Absent dates describe observed data limitations and are not assumed to be zero.",
                        currentActiveDays, durationDays, prevActiveDays, durationDays));
            }
        }

        response.setComparisonReliable(reliable);

        // Calculate metrics
        MetricsCalculationResult currentCalc = metricsCalculator.calculate(currentRecords);
        MetricsCalculationResult prevCalc = metricsCalculator.calculate(prevRecords);

        response.setCurrentMetrics(currentCalc.getMetrics());
        response.setPreviousMetrics(prevCalc.getMetrics());

        warnings.addAll(currentCalc.getWarnings());
        warnings.addAll(prevCalc.getWarnings());

        // Check conversion definition compatibility between periods
        String currentDef = currentCalc.getMetrics().getConversionDefinition();
        String prevDef = prevCalc.getMetrics().getConversionDefinition();
        if (currentDef != null && prevDef != null && !currentDef.equals(prevDef)) {
            warnings.add(String.format("Conversion definitions differ between periods (current: %s, previous: %s). Conversion metrics cannot be compared.",
                    currentDef, prevDef));
        }

        // Calculate deltas - when either period is completely empty, deltas are suppressed to avoid treating absent dates as zero
        Map<String, MetricDeltaDto> deltas = new HashMap<>();
        if (prevActiveDays == 0 || currentActiveDays == 0) {
            deltas.put("spend", new MetricDeltaDto(null, null));
            deltas.put("impressions", new MetricDeltaDto(null, null));
            deltas.put("clicks", new MetricDeltaDto(null, null));
            deltas.put("conversions", new MetricDeltaDto(null, null));
            deltas.put("cpa", new MetricDeltaDto(null, null));
            deltas.put("conversionRate", new MetricDeltaDto(null, null));
            deltas.put("ctr", new MetricDeltaDto(null, null));
            deltas.put("cpc", new MetricDeltaDto(null, null));
            deltas.put("cpm", new MetricDeltaDto(null, null));
        } else {
            deltas.put("spend", computeDelta(currentCalc.getMetrics().getSpend(), prevCalc.getMetrics().getSpend()));
            deltas.put("impressions", computeDelta(toBigDecimal(currentCalc.getMetrics().getImpressions()), toBigDecimal(prevCalc.getMetrics().getImpressions())));
            deltas.put("clicks", computeDelta(toBigDecimal(currentCalc.getMetrics().getClicks()), toBigDecimal(prevCalc.getMetrics().getClicks())));

            // Only compare conversions if definitions are compatible
            if (Objects.equals(currentDef, prevDef) && !"MIXED".equals(currentDef)) {
                deltas.put("conversions", computeDelta(currentCalc.getMetrics().getConversions(), prevCalc.getMetrics().getConversions()));
                deltas.put("cpa", computeDelta(currentCalc.getMetrics().getCpa(), prevCalc.getMetrics().getCpa()));
                deltas.put("conversionRate", computeDelta(currentCalc.getMetrics().getConversionRate(), prevCalc.getMetrics().getConversionRate()));
            } else {
                deltas.put("conversions", new MetricDeltaDto(null, null));
                deltas.put("cpa", new MetricDeltaDto(null, null));
                deltas.put("conversionRate", new MetricDeltaDto(null, null));
            }

            deltas.put("ctr", computeDelta(currentCalc.getMetrics().getCtr(), prevCalc.getMetrics().getCtr()));
            deltas.put("cpc", computeDelta(currentCalc.getMetrics().getCpc(), prevCalc.getMetrics().getCpc()));
            deltas.put("cpm", computeDelta(currentCalc.getMetrics().getCpm(), prevCalc.getMetrics().getCpm()));
        }

        response.setDeltas(deltas);
        response.setWarnings(warnings.stream().distinct().toList());
        return response;
    }

    private MetricDeltaDto computeDelta(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) {
            return new MetricDeltaDto(null, null);
        }
        BigDecimal absoluteChange = current.subtract(previous).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentageChange = null;
        if (previous.compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = absoluteChange.multiply(BigDecimal.valueOf(100))
                    .divide(previous, 2, RoundingMode.HALF_UP);
        }
        return new MetricDeltaDto(absoluteChange, percentageChange);
    }

    private BigDecimal toBigDecimal(Long val) {
        return val != null ? BigDecimal.valueOf(val) : null;
    }

    private List<String> getPlatformVariants(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Platform p = Platform.fromString(raw);
        if (p != Platform.UNSUPPORTED) {
            return List.of(p.name(), p.getDisplayName());
        }
        return List.of(raw.trim());
    }

    private List<EffectiveRecord> fetchRecords(LocalDate start, LocalDate end, String platform) {
        List<String> variants = getPlatformVariants(platform);
        if (variants != null && !variants.isEmpty()) {
            return effectiveRecordRepository.findByPlatformInAndReportingDateBetween(variants, start, end);
        }
        return effectiveRecordRepository.findByReportingDateBetween(start, end);
    }

    private String resolvePlatformFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Platform p = Platform.fromString(raw);
        if (p != Platform.UNSUPPORTED) {
            return p.getDisplayName();
        }
        return raw.trim();
    }

    private DateRange resolveDateRange(LocalDate startDate, LocalDate endDate, String platform) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "startDate cannot be after endDate");
        }

        if (startDate != null && endDate != null) {
            return new DateRange(startDate, endDate, false);
        }

        List<String> variants = getPlatformVariants(platform);
        LocalDate minDate = (variants != null && !variants.isEmpty())
                ? effectiveRecordRepository.findMinReportingDateByPlatformIn(variants)
                : effectiveRecordRepository.findMinReportingDate();

        LocalDate maxDate = (variants != null && !variants.isEmpty())
                ? effectiveRecordRepository.findMaxReportingDateByPlatformIn(variants)
                : effectiveRecordRepository.findMaxReportingDate();

        if (minDate == null || maxDate == null) {
            return new DateRange(startDate, endDate, true);
        }

        LocalDate resolvedStart = (startDate != null) ? startDate : minDate;
        LocalDate resolvedEnd = (endDate != null) ? endDate : maxDate;

        return new DateRange(resolvedStart, resolvedEnd, false);
    }

    private record DateRange(LocalDate start, LocalDate end, boolean isEmpty) {}
}
