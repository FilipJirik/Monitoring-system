package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.BaseMetric;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.entities.SystemSettings;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.MetricPeriod;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.mappers.MetricsMapper;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsStatusDto;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsHistoryDto;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsDailyRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsHourlyRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
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
    private final SystemSettingsCacheService settingsService;
    private final AuthorizationService authorizationService;
    private final MetricsMapper metricsMapper;

    @Transactional(readOnly = true)
    public MetricsStatusDto getLatestMetrics(UserPrincipal principal, UUID deviceId) {
        // Lightweight check - we don't need the Device entity, just working with metrics
        authorizationService.verifyReadAccess(deviceId, principal);

        SystemSettings settings = settingsService.getSettings();

        Metrics latest = metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(deviceId)
                .orElse(null);

        boolean isOnline = false;
        if (latest != null) {
            Instant thresholdTime = Instant.now().minusSeconds(settings.getDeviceOfflineThresholdSeconds());
            isOnline = latest.getTimestamp().isAfter(thresholdTime);
        }

        return metricsMapper.toStatusModel(latest, isOnline);
    }

    @Transactional(readOnly = true)
    public MetricsHistoryDto getMetricsHistory(UserPrincipal principal, UUID deviceId, MetricType type, MetricPeriod period) {
        authorizationService.verifyReadAccess(deviceId, principal);

        SystemSettings settings = settingsService.getSettings();

        Instant from = period.getStartInstant();
        Instant to = Instant.now();

        List<? extends BaseMetric> data = selectDataSource(deviceId, from, to, period, settings);

        return metricsMapper.toHistoryModel(data, type);
    }

    private List<? extends BaseMetric> selectDataSource(UUID deviceId, Instant from, Instant to, MetricPeriod period, SystemSettings settings) {
        long requestedHours = period.getTotalHours();
        long rawRetentionHours = settings.getRawDataRetentionDays() * 24L;
        long hourlyRetentionHours = settings.getHourlyDataRetentionDays() * 24L;

        if (requestedHours <= rawRetentionHours) {
            return metricsRepository.findAllByDeviceIdAndTimestampBetweenOrderByTimestampAsc(deviceId, from, to);
        } else if (requestedHours <= hourlyRetentionHours) {
            return metricHourlyRepository.findAllByDeviceIdAndTimestampBetweenOrderByTimestampAsc(deviceId, from, to);
        } else {
            return metricDailyRepository.findAllByDeviceIdAndTimestampBetweenOrderByTimestampAsc(deviceId, from, to);
        }
    }
}
