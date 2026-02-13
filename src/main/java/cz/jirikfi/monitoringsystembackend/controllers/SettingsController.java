package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.SystemSettings;
import cz.jirikfi.monitoringsystembackend.mappers.SystemSettingsMapper;
import cz.jirikfi.monitoringsystembackend.models.settings.SettingsUpdateModel;
import cz.jirikfi.monitoringsystembackend.models.settings.SettingsResponseModel;
import cz.jirikfi.monitoringsystembackend.services.SystemSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SystemSettingsService systemSettingsService;
    private final SystemSettingsMapper systemSettingsMapper;

    // GET /api/settings GET current global settings
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SettingsResponseModel> getSettings() {
        SystemSettings settings = systemSettingsService.getSettings();
        return ResponseEntity.ok(systemSettingsMapper.toResponse(settings));
    }

    // PUT /api/settings change global settings
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeSettings(@Valid @RequestBody SettingsUpdateModel model){
        systemSettingsService.updateSettings(model);
        return ResponseEntity.noContent().build();
    }
}
