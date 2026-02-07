package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.SystemSettings;
import cz.jirikfi.monitoringsystembackend.models.settings.SettingsResponseModel;
import org.springframework.stereotype.Component;

@Component
public class SystemSettingsMapper {

    public SettingsResponseModel toResponse(SystemSettings settings) {
        return SettingsResponseModel.builder()
                .rawDataRetentionDays(settings.getRawDataRetentionDays())
                .hourlyDataRetentionDays(settings.getHourlyDataRetentionDays())
                .dailyDataRetentionDays(settings.getDailyDataRetentionDays())
                .deviceOfflineThresholdSeconds(settings.getDeviceOfflineThresholdSeconds())
                .build();
    }
}
