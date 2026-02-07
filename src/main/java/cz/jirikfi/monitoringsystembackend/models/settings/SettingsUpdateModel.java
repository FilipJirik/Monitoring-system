package cz.jirikfi.monitoringsystembackend.models.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SettingsUpdateModel {
    @NotNull(message = "Raw data retention days is required")
    @Min(value = 1, message = "Raw data must be kept for at least 1 day")
    @Max(value = 365, message = "Raw data retention cannot exceed 1 year")
    private Integer rawDataRetentionDays;

    @NotNull(message = "Hourly data retention days is required")
    @Min(value = 7, message = "Hourly data must be kept for at least 7 days")
    @Max(value = 1825, message = "Hourly data retention cannot exceed 5 years")
    private Integer hourlyDataRetentionDays;

    @NotNull(message = "Daily data retention days is required")
    @Min(value = 30, message = "Daily data must be kept for at least 30 days")
    private Integer dailyDataRetentionDays;

    @NotNull(message = "Device offline threshold is required")
    @Min(value = 30, message = "Threshold must be at least 30 seconds")
    @Max(value = 86400, message = "Threshold cannot be more than 24 hours")
    private Integer deviceOfflineThresholdSeconds;
}
