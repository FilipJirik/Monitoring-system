package cz.jirikfi.monitoringsystembackend.models.settings;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettingsResponseDto {
    private Integer rawDataRetentionDays;
    private Integer hourlyDataRetentionDays;
    private Integer dailyDataRetentionDays;
    private Integer deviceOfflineThresholdSeconds;
}
