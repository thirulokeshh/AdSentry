package com.adsentry.recommendation.service;

import com.adsentry.analytics.dto.MetricsCalculationResult;
import com.adsentry.analytics.service.MetricsCalculator;
import com.adsentry.common.exception.ApiException;
import com.adsentry.common.exception.ErrorCode;
import com.adsentry.importing.domain.EffectiveRecord;
import com.adsentry.importing.repository.EffectiveRecordRepository;
import com.adsentry.normalization.domain.ConversionDefinition;
import com.adsentry.normalization.domain.Platform;
import com.adsentry.recommendation.domain.Recommendation;
import com.adsentry.recommendation.domain.RecommendationPriority;
import com.adsentry.recommendation.domain.RecommendationStatus;
import com.adsentry.recommendation.domain.RuleIdentifier;
import com.adsentry.recommendation.dto.AnalysisRunRequest;
import com.adsentry.recommendation.dto.AnalysisRunResponse;
import com.adsentry.recommendation.dto.RecommendationDto;
import com.adsentry.recommendation.event.AnalysisRecalculationEvent;
import com.adsentry.recommendation.repository.RecommendationRepository;
import com.adsentry.settings.domain.AppSetting;
import com.adsentry.settings.repository.AppSettingRepository;
import com.adsentry.settings.service.AppSettingsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final EffectiveRecordRepository effectiveRecordRepository;
    private final RecommendationRepository recommendationRepository;
    private final AppSettingRepository appSettingRepository;
    private final MetricsCalculator metricsCalculator;
    private final ObjectMapper objectMapper;
    private final List<RecommendationRule> rules;

    public RecommendationService(EffectiveRecordRepository effectiveRecordRepository,
                                 RecommendationRepository recommendationRepository,
                                 AppSettingRepository appSettingRepository,
                                 MetricsCalculator metricsCalculator,
                                 ObjectMapper objectMapper,
                                 List<RecommendationRule> rules) {
        this.effectiveRecordRepository = effectiveRecordRepository;
        this.recommendationRepository = recommendationRepository;
        this.appSettingRepository = appSettingRepository;
        this.metricsCalculator = metricsCalculator;
        this.objectMapper = objectMapper;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> getRecommendations(LocalDate startDate,
                                                     LocalDate endDate,
                                                     String platform,
                                                     RecommendationStatus status,
                                                     RecommendationPriority priority,
                                                     RuleIdentifier rule) {
        String normalizedPlatform = resolvePlatformFilter(platform);
        List<Recommendation> all = recommendationRepository.findAll();

        return all.stream()
                .filter(r -> startDate == null || !r.getPeriodStart().isBefore(startDate))
                .filter(r -> endDate == null || !r.getPeriodEnd().isAfter(endDate))
                .filter(r -> normalizedPlatform == null || normalizedPlatform.equalsIgnoreCase(r.getPlatform()))
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> priority == null || r.getPriority() == priority)
                .filter(r -> rule == null || r.getRuleIdentifier() == rule)
                .sorted(Comparator.comparing(Recommendation::getCreatedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RecommendationDto updateStatus(Long id, RecommendationStatus newStatus) {
        if (newStatus == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Recommendation status cannot be null");
        }
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RECOMMENDATION_NOT_FOUND, "Recommendation not found with ID: " + id));

        rec.setStatus(newStatus);
        rec.setUpdatedAt(Instant.now());
        Recommendation saved = recommendationRepository.save(rec);
        return toDto(saved);
    }

    @Transactional
    public AnalysisRunResponse runAnalysis(AnalysisRunRequest request) {
        String platform = request != null ? request.getPlatform() : null;
        String normalizedPlatform = resolvePlatformFilter(platform);

        DateRange dateRange = resolveDateRange(
                request != null ? request.getStartDate() : null,
                request != null ? request.getEndDate() : null,
                normalizedPlatform
        );

        if (dateRange.isEmpty()) {
            return new AnalysisRunResponse(
                    request != null ? request.getStartDate() : null,
                    request != null ? request.getEndDate() : null,
                    normalizedPlatform,
                    Instant.now(),
                    0,
                    0,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    List.of("No campaign data available to analyze for the selected criteria."),
                    Collections.emptyList()
            );
        }

        LocalDate periodStart = dateRange.start();
        LocalDate periodEnd = dateRange.end();

        // 1. Resolve thresholds (saved defaults + optional per-analysis overrides)
        RecommendationThresholds thresholds = resolveThresholds(request);

        // 2. Fetch records
        List<EffectiveRecord> records = fetchRecords(periodStart, periodEnd, normalizedPlatform);

        long totalCalendarDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        long overallActiveDays = records.stream().map(EffectiveRecord::getReportingDate).distinct().count();
        BigDecimal overallCoveragePct = totalCalendarDays > 0
                ? BigDecimal.valueOf(overallActiveDays).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCalendarDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        boolean overallSufficient = overallActiveDays > 0 && overallCoveragePct.compareTo(BigDecimal.valueOf(50)) >= 0;

        List<String> warnings = new ArrayList<>();
        if (overallActiveDays < totalCalendarDays) {
            long missingDays = totalCalendarDays - overallActiveDays;
            warnings.add(String.format("%d of %d calendar days had no recorded activity. Absent dates describe observed data limitations and are not assumed to be zero.",
                    missingDays, totalCalendarDays));
        }
        if (!overallSufficient && overallActiveDays > 0) {
            warnings.add(String.format("Data coverage across the period is below 50%% (%s%%). Findings may be unreliable.",
                    overallCoveragePct.toPlainString()));
        }

        // 3. Group records by campaign
        Map<String, List<EffectiveRecord>> byCampaign = records.stream()
                .collect(Collectors.groupingBy(EffectiveRecord::getCampaignIdentityKey));

        List<RecommendationCandidate> candidates = new ArrayList<>();

        for (Map.Entry<String, List<EffectiveRecord>> entry : byCampaign.entrySet()) {
            String campaignIdentityKey = entry.getKey();
            List<EffectiveRecord> campRecords = entry.getValue();

            EffectiveRecord first = campRecords.get(0);
            String campaignName = first.getCampaignName();
            Platform p = Platform.fromString(first.getPlatform());
            String campPlatform = (p != Platform.UNSUPPORTED) ? p.getDisplayName() : first.getPlatform();
            String identityConfidence = campRecords.stream()
                    .anyMatch(r -> "LOW".equalsIgnoreCase(String.valueOf(r.getIdentityConfidence()))) ? "LOW" : "HIGH";

            long campActiveDays = campRecords.stream().map(EffectiveRecord::getReportingDate).distinct().count();
            BigDecimal campCovPct = totalCalendarDays > 0
                    ? BigDecimal.valueOf(campActiveDays).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalCalendarDays), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            boolean campSufficient = campActiveDays > 0 && campCovPct.compareTo(BigDecimal.valueOf(50)) >= 0;

            // Conversion inspection across campaign records
            boolean hasMissingConversions = false;
            boolean hasUnspecifiedConversions = false;
            Set<ConversionDefinition> explicitDefs = new LinkedHashSet<>();
            BigDecimal compatibleConvSum = BigDecimal.ZERO;

            for (EffectiveRecord r : campRecords) {
                if (r.getConversions() == null) {
                    hasMissingConversions = true;
                } else {
                    ConversionDefinition def = r.getConversionDefinition() != null
                            ? r.getConversionDefinition()
                            : ConversionDefinition.UNSPECIFIED;

                    if (def == ConversionDefinition.UNSPECIFIED) {
                        hasUnspecifiedConversions = true;
                    } else if (def.isCpaRuleEligible()) {
                        explicitDefs.add(def);
                        compatibleConvSum = compatibleConvSum.add(r.getConversions());
                    }
                }
            }

            boolean hasMixedConversions = explicitDefs.size() > 1;
            ConversionDefinition compatibleDef = explicitDefs.size() == 1 ? explicitDefs.iterator().next() : null;
            BigDecimal finalCompatibleConversions = (compatibleDef != null && !hasMixedConversions)
                    ? compatibleConvSum.setScale(2, RoundingMode.HALF_UP)
                    : null;

            MetricsCalculationResult calcResult = metricsCalculator.calculate(campRecords);

            CampaignAnalysisContext context = new CampaignAnalysisContext(
                    campaignIdentityKey,
                    campaignName,
                    campPlatform,
                    identityConfidence,
                    periodStart,
                    periodEnd,
                    (int) campActiveDays,
                    (int) totalCalendarDays,
                    campCovPct,
                    campSufficient,
                    campRecords,
                    calcResult.getMetrics(),
                    hasMissingConversions,
                    hasUnspecifiedConversions,
                    hasMixedConversions,
                    compatibleDef,
                    finalCompatibleConversions
            );

            // Evaluate recommendation rules (a campaign cannot trigger multiple rules, avoiding double-counting spend)
            for (RecommendationRule rule : rules) {
                Optional<RecommendationCandidate> candidateOpt = rule.evaluate(context, thresholds);
                if (candidateOpt.isPresent()) {
                    candidates.add(candidateOpt.get());
                    break;
                }
            }
        }

        // 4. Synchronize with database: preserve status, reset on material change, retire stale findings
        List<Recommendation> savedRecs = syncRecommendations(candidates, periodStart, periodEnd, normalizedPlatform);

        Map<String, Object> thresholdsMap = new LinkedHashMap<>();
        thresholdsMap.put("globalCpaTarget", thresholds.globalCpaTarget());
        thresholdsMap.put("minSpendThreshold", thresholds.minSpendThreshold());
        thresholdsMap.put("priorityHighSpendCutoff", thresholds.priorityHighSpendCutoff());
        thresholdsMap.put("priorityHighCpaMultiplier", thresholds.priorityHighCpaMultiplier());

        Map<String, Object> coverageMap = new LinkedHashMap<>();
        coverageMap.put("totalCalendarDays", totalCalendarDays);
        coverageMap.put("activeDays", overallActiveDays);
        coverageMap.put("coveragePercentage", overallCoveragePct);
        coverageMap.put("sufficientCoverage", overallSufficient);

        List<RecommendationDto> dtos = savedRecs.stream().map(this::toDto).toList();

        return new AnalysisRunResponse(
                periodStart,
                periodEnd,
                normalizedPlatform,
                Instant.now(),
                byCampaign.size(),
                dtos.size(),
                thresholdsMap,
                coverageMap,
                warnings,
                dtos
        );
    }

    @EventListener
    @Transactional
    public void onRecalculationEvent(AnalysisRecalculationEvent event) {
        log.info("Triggering recommendation recalculation due to event: {}", event.getReason());
        recalculateLatestAnalysis();
    }

    @Transactional
    public void recalculateLatestAnalysis() {
        LocalDate minDate = effectiveRecordRepository.findMinReportingDate();
        LocalDate maxDate = effectiveRecordRepository.findMaxReportingDate();

        if (minDate == null || maxDate == null) {
            // No data exists, retire all existing recommendations
            log.info("No active records remain in effective dataset. Retiring all existing recommendations.");
            recommendationRepository.deleteAll();
            return;
        }

        runAnalysis(new AnalysisRunRequest(minDate, maxDate, null, null, null, null, null));
    }

    @Transactional
    public List<Recommendation> syncRecommendations(List<RecommendationCandidate> candidates,
                                                    LocalDate periodStart,
                                                    LocalDate periodEnd,
                                                    String platformFilter) {
        // Fetch existing recommendations for this scope (filter by platform if specified)
        List<Recommendation> existingInScope = recommendationRepository.findAll().stream()
                .filter(r -> platformFilter == null || platformFilter.equalsIgnoreCase(r.getPlatform()))
                .toList();

        Map<String, Recommendation> existingByFingerprint = existingInScope.stream()
                .collect(Collectors.toMap(Recommendation::getFingerprint, r -> r, (a, b) -> a));

        Set<String> newFingerprints = new HashSet<>();
        List<Recommendation> toSave = new ArrayList<>();

        for (RecommendationCandidate candidate : candidates) {
            String fingerprint = generateFingerprint(
                    candidate.ruleIdentifier(),
                    candidate.campaignIdentityKey()
            );
            newFingerprints.add(fingerprint);

            Recommendation existing = existingByFingerprint.get(fingerprint);

            if (existing == null) {
                // New recommendation
                Recommendation newRec = new Recommendation();
                newRec.setFingerprint(fingerprint);
                newRec.setRuleIdentifier(candidate.ruleIdentifier());
                newRec.setPlatform(candidate.platform());
                newRec.setCampaignName(candidate.campaignName());
                newRec.setCampaignIdentityKey(candidate.campaignIdentityKey());
                newRec.setIdentityReliability(candidate.identityReliability());
                newRec.setPeriodStart(periodStart);
                newRec.setPeriodEnd(periodEnd);
                newRec.setPriority(candidate.priority());
                newRec.setTitle(candidate.title());
                newRec.setExplanation(candidate.explanation());
                newRec.setSuggestedAction(candidate.suggestedAction());
                newRec.setSupportingMetricsJson(toJson(candidate.supportingMetrics()));
                newRec.setTriggerCondition(candidate.triggerCondition());
                newRec.setThresholdsUsedJson(toJson(candidate.thresholdsUsed()));
                newRec.setStatus(RecommendationStatus.NEW);
                newRec.setCreatedAt(Instant.now());
                newRec.setUpdatedAt(Instant.now());
                toSave.add(newRec);
            } else {
                // Existing recommendation - check for material change
                boolean materialChange = isMaterialChange(existing, candidate);

                if (materialChange) {
                    // Reset to NEW when the underlying issue materially changes
                    existing.setStatus(RecommendationStatus.NEW);
                }
                // Otherwise preserve existing status (REVIEWED, DISMISSED, etc.)

                existing.setPeriodStart(periodStart);
                existing.setPeriodEnd(periodEnd);
                existing.setPriority(candidate.priority());
                existing.setTitle(candidate.title());
                existing.setExplanation(candidate.explanation());
                existing.setSuggestedAction(candidate.suggestedAction());
                existing.setSupportingMetricsJson(toJson(candidate.supportingMetrics()));
                existing.setTriggerCondition(candidate.triggerCondition());
                existing.setThresholdsUsedJson(toJson(candidate.thresholdsUsed()));
                existing.setUpdatedAt(Instant.now());
                toSave.add(existing);
            }
        }

        // Retire stale recommendations whose rule no longer triggers in this period
        List<Recommendation> toRetire = existingInScope.stream()
                .filter(r -> !newFingerprints.contains(r.getFingerprint()))
                .toList();

        if (!toRetire.isEmpty()) {
            log.info("Retiring {} obsolete recommendations for period {} to {}",
                    toRetire.size(), periodStart, periodEnd);
            recommendationRepository.deleteAll(toRetire);
        }

        return recommendationRepository.saveAll(toSave);
    }

    private boolean isMaterialChange(Recommendation existing, RecommendationCandidate candidate) {
        // 1. Priority change is always a material change
        if (existing.getPriority() != candidate.priority()) {
            return true;
        }

        // 2. Significant metric shift (>= 20% difference in primary metric)
        try {
            Map<String, Object> prevMetrics = objectMapper.readValue(
                    existing.getSupportingMetricsJson(),
                    new TypeReference<Map<String, Object>>() {}
            );

            if (candidate.ruleIdentifier() == RuleIdentifier.RULE_A_ZERO_CONVERSIONS) {
                Object prevSpendObj = prevMetrics.get("spend");
                if (prevSpendObj != null && candidate.spend() != null) {
                    BigDecimal prevSpend = new BigDecimal(prevSpendObj.toString());
                    if (prevSpend.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal diff = candidate.spend().subtract(prevSpend).abs();
                        BigDecimal changePct = diff.divide(prevSpend, 4, RoundingMode.HALF_UP);
                        if (changePct.compareTo(new BigDecimal("0.20")) >= 0) {
                            return true;
                        }
                    }
                }
            } else if (candidate.ruleIdentifier() == RuleIdentifier.RULE_B_HIGH_CPA) {
                Object prevCpaObj = prevMetrics.get("cpa");
                if (prevCpaObj != null && candidate.cpa() != null) {
                    BigDecimal prevCpa = new BigDecimal(prevCpaObj.toString());
                    if (prevCpa.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal diff = candidate.cpa().subtract(prevCpa).abs();
                        BigDecimal changePct = diff.divide(prevCpa, 4, RoundingMode.HALF_UP);
                        if (changePct.compareTo(new BigDecimal("0.20")) >= 0) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse previous supporting metrics for material change check: {}", e.getMessage());
        }

        return false;
    }

    public static String generateFingerprint(RuleIdentifier rule, String campaignIdentityKey) {
        String raw = rule.name() + ":" + campaignIdentityKey;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("rec-");
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    public static String generateFingerprint(RuleIdentifier rule,
                                             String campaignIdentityKey,
                                             LocalDate start,
                                             LocalDate end) {
        return generateFingerprint(rule, campaignIdentityKey);
    }

    private RecommendationThresholds resolveThresholds(AnalysisRunRequest request) {
        BigDecimal cpaTarget = request != null && request.getGlobalCpaTargetOverride() != null
                ? request.getGlobalCpaTargetOverride()
                : getDecimalSetting(AppSettingsService.KEY_GLOBAL_CPA_TARGET, new BigDecimal("500.00"));

        BigDecimal minSpend = request != null && request.getMinSpendThresholdOverride() != null
                ? request.getMinSpendThresholdOverride()
                : getDecimalSetting(AppSettingsService.KEY_MIN_SPEND_THRESHOLD, new BigDecimal("1000.00"));

        BigDecimal highSpendCutoff = request != null && request.getPriorityHighSpendCutoffOverride() != null
                ? request.getPriorityHighSpendCutoffOverride()
                : getDecimalSetting(AppSettingsService.KEY_PRIORITY_HIGH_SPEND_CUTOFF, new BigDecimal("5000.00"));

        BigDecimal highCpaMultiplier = request != null && request.getPriorityHighCpaMultiplierOverride() != null
                ? request.getPriorityHighCpaMultiplierOverride()
                : getDecimalSetting(AppSettingsService.KEY_PRIORITY_HIGH_CPA_MULTIPLIER, new BigDecimal("2.0"));

        return RecommendationThresholds.of(cpaTarget, minSpend, highSpendCutoff, highCpaMultiplier);
    }

    private BigDecimal getDecimalSetting(String key, BigDecimal defaultValue) {
        return appSettingRepository.findBySettingKey(key)
                .map(AppSetting::getSettingValue)
                .map(val -> {
                    try {
                        return new BigDecimal(val);
                    } catch (Exception e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private List<EffectiveRecord> fetchRecords(LocalDate start, LocalDate end, String platform) {
        List<String> variants = getPlatformVariants(platform);
        if (variants != null && !variants.isEmpty()) {
            return effectiveRecordRepository.findByPlatformInAndReportingDateBetween(variants, start, end);
        }
        return effectiveRecordRepository.findByReportingDateBetween(start, end);
    }

    private List<String> getPlatformVariants(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Platform p = Platform.fromString(raw);
        if (p != Platform.UNSUPPORTED) {
            return List.of(p.name(), p.getDisplayName());
        }
        return List.of(raw.trim());
    }

    private String resolvePlatformFilter(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Platform p = Platform.fromString(raw);
        if (p != Platform.UNSUPPORTED) return p.getDisplayName();
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

    private RecommendationDto toDto(Recommendation rec) {
        Map<String, Object> metrics = Collections.emptyMap();
        Map<String, Object> thresholds = Collections.emptyMap();
        try {
            if (rec.getSupportingMetricsJson() != null) {
                metrics = objectMapper.readValue(rec.getSupportingMetricsJson(), new TypeReference<Map<String, Object>>() {});
            }
            if (rec.getThresholdsUsedJson() != null) {
                thresholds = objectMapper.readValue(rec.getThresholdsUsedJson(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON for recommendation {}: {}", rec.getId(), e.getMessage());
        }

        return new RecommendationDto(
                rec.getId(),
                rec.getFingerprint(),
                rec.getRuleIdentifier(),
                rec.getRuleIdentifier() != null ? rec.getRuleIdentifier().getDescription() : null,
                rec.getPlatform(),
                rec.getCampaignName(),
                rec.getCampaignIdentityKey(),
                rec.getIdentityReliability(),
                rec.getPeriodStart(),
                rec.getPeriodEnd(),
                rec.getPriority(),
                rec.getTitle(),
                rec.getExplanation(),
                rec.getSuggestedAction(),
                metrics,
                rec.getTriggerCondition(),
                thresholds,
                rec.getStatus(),
                rec.getCreatedAt(),
                rec.getUpdatedAt()
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private record DateRange(LocalDate start, LocalDate end, boolean isEmpty) {}
}
