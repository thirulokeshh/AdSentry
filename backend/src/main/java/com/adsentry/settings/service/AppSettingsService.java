package com.adsentry.settings.service;

import com.adsentry.common.exception.ApiException;
import com.adsentry.common.exception.ErrorCode;
import com.adsentry.settings.domain.AppSetting;
import com.adsentry.settings.dto.AppSettingsDto;
import com.adsentry.settings.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class AppSettingsService {

    public static final String KEY_GLOBAL_CPA_TARGET = "global_cpa_target";
    public static final String KEY_MIN_SPEND_THRESHOLD = "min_spend_threshold";
    public static final String KEY_PRIORITY_HIGH_SPEND_CUTOFF = "priority_high_spend_cutoff";
    public static final String KEY_PRIORITY_HIGH_CPA_MULTIPLIER = "priority_high_cpa_multiplier";

    private final AppSettingRepository settingRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public AppSettingsService(AppSettingRepository settingRepository,
                              org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.settingRepository = settingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public AppSettingsDto getSettings() {
        BigDecimal cpaTarget = getDecimalSetting(KEY_GLOBAL_CPA_TARGET, new BigDecimal("500.00"));
        BigDecimal minSpend = getDecimalSetting(KEY_MIN_SPEND_THRESHOLD, new BigDecimal("1000.00"));
        BigDecimal highSpendCutoff = getDecimalSetting(KEY_PRIORITY_HIGH_SPEND_CUTOFF, new BigDecimal("5000.00"));
        BigDecimal highCpaMultiplier = getDecimalSetting(KEY_PRIORITY_HIGH_CPA_MULTIPLIER, new BigDecimal("2.0"));

        Instant lastUpdated = settingRepository.findBySettingKey(KEY_GLOBAL_CPA_TARGET)
                .map(AppSetting::getUpdatedAt)
                .orElse(Instant.now());

        return new AppSettingsDto(cpaTarget, minSpend, highSpendCutoff, highCpaMultiplier, lastUpdated);
    }

    @Transactional
    public AppSettingsDto updateSettings(AppSettingsDto dto) {
        if (dto.getGlobalCpaTarget() == null || dto.getGlobalCpaTarget().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "globalCpaTarget must be a non-negative INR amount.");
        }
        if (dto.getMinSpendThreshold() == null || dto.getMinSpendThreshold().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "minSpendThreshold must be a non-negative INR amount.");
        }

        saveSetting(KEY_GLOBAL_CPA_TARGET, dto.getGlobalCpaTarget().toPlainString(), "Configured target CPA in INR");
        saveSetting(KEY_MIN_SPEND_THRESHOLD, dto.getMinSpendThreshold().toPlainString(), "Configured minimum spend threshold in INR");

        if (dto.getPriorityHighSpendCutoff() != null) {
            saveSetting(KEY_PRIORITY_HIGH_SPEND_CUTOFF, dto.getPriorityHighSpendCutoff().toPlainString(), "Configured high priority spend cutoff in INR");
        }
        if (dto.getPriorityHighCpaMultiplier() != null) {
            saveSetting(KEY_PRIORITY_HIGH_CPA_MULTIPLIER, dto.getPriorityHighCpaMultiplier().toPlainString(), "Configured high priority CPA multiplier");
        }

        settingRepository.flush();

        eventPublisher.publishEvent(new com.adsentry.recommendation.event.AnalysisRecalculationEvent("SETTINGS_UPDATED"));

        return getSettings();
    }

    private BigDecimal getDecimalSetting(String key, BigDecimal defaultValue) {
        return settingRepository.findBySettingKey(key)
                .map(s -> {
                    try {
                        return new BigDecimal(s.getSettingValue());
                    } catch (Exception e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private void saveSetting(String key, String value, String description) {
        AppSetting setting = settingRepository.findBySettingKey(key)
                .orElse(new AppSetting(key, value, description));
        setting.setSettingValue(value);
        setting.setDescription(description);
        setting.setUpdatedAt(Instant.now());
        settingRepository.save(setting);
    }
}
