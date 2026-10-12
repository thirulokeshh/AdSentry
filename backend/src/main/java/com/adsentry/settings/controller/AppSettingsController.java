package com.adsentry.settings.controller;

import com.adsentry.settings.dto.AppSettingsDto;
import com.adsentry.settings.service.AppSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class AppSettingsController {

    private final AppSettingsService settingsService;

    public AppSettingsController(AppSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<AppSettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<AppSettingsDto> updateSettings(@Valid @RequestBody AppSettingsDto request) {
        return ResponseEntity.ok(settingsService.updateSettings(request));
    }
}
