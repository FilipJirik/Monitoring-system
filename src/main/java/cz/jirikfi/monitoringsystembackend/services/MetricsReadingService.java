package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.BaseMetric;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.entities.SystemSettings;
import cz.jirikfi.monitoringsystembackend.enums.MetricPeriod;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.models.metrics.DataPoint;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsStatusModel;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsHistoryModel;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsDailyRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsHourlyRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import cz.jirikfi.monitoringsystembackend.utils.MetricUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MetricsReadingService {

    private final MetricsRepository metricsRepository;
    private final MetricsHourlyRepository metricHourlyRepository;
    private final MetricsDailyRepository metricDailyRepository;
    private final SystemSettingsService settingsService;


    @Transactional(readOnly = true)
    public MetricsStatusModel getLatestMetrics(UUID deviceId) {
        // Read cached settings
        SystemSettings settings = settingsService.getSettings();

        Metrics latest = metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(deviceId)
                .orElse(null);

        if (latest == null) {
            return MetricsStatusModel.builder().isOnline(false).build();
        }

        // Check online status using configured threshold
        boolean isOnline = latest.getTimestamp()
                .isAfter(Instant.now().minusSeconds(settings.getDeviceOfflineThresholdSeconds()));

        return MetricsStatusModel.builder()
                .isOnline(isOnline)
                .lastSeen(latest.getTimestamp())
                .uptimeSeconds(latest.getUptimeSeconds())
                .currentCpuUsage(latest.getCpuUsagePercent())
                .currentCpuTemp(latest.getCpuTempCelsius())
                .currentRamUsage(latest.getRamUsageMb() != null ? latest.getRamUsageMb().doubleValue() : 0.0)
                .currentDiskUsage(latest.getDiskUsagePercent())
                .build();
    }

    @Transactional(readOnly = true)
    public MetricsHistoryModel getMetricsHistory(UUID deviceId, MetricType type, MetricPeriod period) {
        SystemSettings settings = settingsService.getSettings();

        Instant from = period.getStartInstant();
        Instant to = Instant.now();
        List<? extends BaseMetric> data;

        long requestedHours = period.getTotalHours();
        long rawRetentionHours = settings.getRawDataRetentionDays() * 24L;
        long hourlyRetentionHours = settings.getHourlyDataRetentionDays() * 24L;

        if (requestedHours <= rawRetentionHours) {
            // High detail needed -> RAW
            data = metricsRepository.findAllByDeviceIdAndTimestampBetweenOrderByTimestampAsc(deviceId, from, to);
        } else if (requestedHours <= hourlyRetentionHours) {
            // Medium detail needed -> HOURLY
            data = metricHourlyRepository.findAllByDeviceIdAndTimestampBetweenOrderByTimestampAsc(deviceId, from, to);
        } else {
            // Long term overview -> DAILY
            data = metricDailyRepository.findAllByDeviceIdAndTimestampBetweenOrderByTimestampAsc(deviceId, from, to);
        }

        List<DataPoint> points = data.stream()
                .map(m -> new DataPoint(m.getTimestamp(), MetricUtil.getValue(m, type)))
                .toList();

        return MetricsHistoryModel.builder()
                .type(type)
                .label(type.getLabel())
                .unit(type.getUnit())
                .data(points)
                .build();
    }
}
