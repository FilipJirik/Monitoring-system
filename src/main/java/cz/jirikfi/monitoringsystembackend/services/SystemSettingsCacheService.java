package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.configurations.CacheConfig;
import cz.jirikfi.monitoringsystembackend.entities.SystemSettings;
import cz.jirikfi.monitoringsystembackend.exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.exceptions.InternalErrorException;
import cz.jirikfi.monitoringsystembackend.models.settings.SettingsUpdateRequestDto;
import cz.jirikfi.monitoringsystembackend.repositories.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingsCacheService {

    private final SystemSettingsRepository settingsRepository;

    @Cacheable(value = CacheConfig.SYSTEM_SETTINGS, key = "'global'")
    @Transactional(readOnly = true)
    public SystemSettings getSettings() {
        return settingsRepository.findById(SystemSettings.ID)
                .orElseThrow(() -> new InternalErrorException("Default system settings not initialized by DataSeeder"));
    }
    @CacheEvict(value = CacheConfig.SYSTEM_SETTINGS, key = "'global'")
    @Transactional
    public void updateSettings(SettingsUpdateRequestDto model) {

        validateRetentionLogic(model);

        SystemSettings settings = SystemSettings.builder()
                .id(SystemSettings.ID) // Force ID 1
                .rawDataRetentionDays(model.getRawDataRetentionDays())
                .hourlyDataRetentionDays(model.getHourlyDataRetentionDays())
                .dailyDataRetentionDays(model.getDailyDataRetentionDays())
                .deviceOfflineThresholdSeconds(model.getDeviceOfflineThresholdSeconds())
                .build();

        settingsRepository.save(settings);
    }

    private void validateRetentionLogic(SettingsUpdateRequestDto request) {
        // Hourly retention must be >= Raw retention
        if (request.getHourlyDataRetentionDays() < request.getRawDataRetentionDays()) {
            throw new BadRequestException("Hourly retention cannot be less than Raw retention.");
        }
        // Daily retention must be >= Hourly retention
        if (request.getDailyDataRetentionDays() < request.getHourlyDataRetentionDays()) {
            throw new BadRequestException("Daily retention cannot be less than Hourly retention.");
        }
    }
}