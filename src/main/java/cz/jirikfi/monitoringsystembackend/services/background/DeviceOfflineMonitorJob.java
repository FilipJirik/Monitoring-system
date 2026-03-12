package cz.jirikfi.monitoringsystembackend.services.background;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.entities.SystemSettings;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRepository;
import cz.jirikfi.monitoringsystembackend.repositories.AlertThresholdRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import cz.jirikfi.monitoringsystembackend.services.AlertProcessingService;
import cz.jirikfi.monitoringsystembackend.services.SystemSettingsCacheService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceOfflineMonitorJob {

    private final AlertThresholdRepository thresholdRepository;
    private final MetricsRepository metricsRepository;
    private final AlertRepository alertRepository;
    private final AlertProcessingService alertProcessingService;
    private final SystemSettingsCacheService settingsService;
    private final JobScheduler jobScheduler;

    @PostConstruct
    public void scheduleRecurrentJob() {
        jobScheduler.scheduleRecurrently("device-offline-check", Cron.minutely(), this::checkDevicesOnlineStatus);
        log.info("DeviceOfflineMonitorJob recurring job scheduled (every minute).");
    }

    @Job(name = "Check Devices Online Status")
    @Transactional
    public void checkDevicesOnlineStatus() {
        SystemSettings settings = settingsService.getSettings();
        int offlineThresholdSeconds = settings.getDeviceOfflineThresholdSeconds();

        List<AlertThreshold> offlineThresholds = thresholdRepository.findAllByMetricType(MetricType.DEVICE_OFFLINE);

        if (offlineThresholds.isEmpty()) {
            return;
        }

        log.debug("Checking {} devices for offline status (threshold: {}s)", offlineThresholds.size(),
                offlineThresholdSeconds);

        for (AlertThreshold threshold : offlineThresholds) {
            try {
                processDevice(threshold, offlineThresholdSeconds);
            } catch (Exception e) {
                log.error("Error checking offline status for device {}", threshold.getDevice().getId(), e);
            }
        }
    }

    private void processDevice(AlertThreshold threshold, int offlineThresholdSeconds) {
        Device device = threshold.getDevice();

        Optional<Metrics> latestMetric = metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(device.getId());

        boolean isOffline = latestMetric.isEmpty()
                || Duration.between(latestMetric.get().getTimestamp(), Instant.now())
                        .getSeconds() > offlineThresholdSeconds;

        Optional<Alert> activeAlert = alertRepository
                .findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(device.getId(), MetricType.DEVICE_OFFLINE);

        if (isOffline && activeAlert.isEmpty()) {
            log.info("Device {} detected as OFFLINE", device.getName());
            alertProcessingService.createAlertAndNotify(device, threshold, 0.0);
        } else if (!isOffline && activeAlert.isPresent()) {
            log.info("Device {} is back ONLINE", device.getName());
            alertProcessingService.resolveAlert(activeAlert.get(), 0.0);
        }
    }
}
