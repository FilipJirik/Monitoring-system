package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.SystemSettings;
import cz.jirikfi.monitoringsystembackend.models.settings.SettingsResponseDto;
import org.springframework.stereotype.Component;

@Component
public class SystemSettingsMapper {

    public SettingsResponseDto toResponse(SystemSettings settings) {
        return SettingsResponseDto.builder()
                .rawDataRetentionDays(settings.getRawDataRetentionDays())
                .hourlyDataRetentionDays(settings.getHourlyDataRetentionDays())
                .dailyDataRetentionDays(settings.getDailyDataRetentionDays())
                .deviceOfflineThresholdSeconds(settings.getDeviceOfflineThresholdSeconds())
                .build();
    }
}
