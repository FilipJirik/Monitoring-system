package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRepository;
import cz.jirikfi.monitoringsystembackend.services.background.NotificationJobService;
import cz.jirikfi.monitoringsystembackend.utils.MetricUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertProcessingService {
    private final AlertRepository alertRepository;
    private final ThresholdCacheService thresholdCacheService;
    private final NotificationJobService notificationJobService;

    private final JobScheduler jobScheduler;

    @Async
    @Transactional
    public void checkThresholdsAsync(Metrics metric) {
        UUID deviceId = metric.getDevice().getId();

        // Read from cache
        List<AlertThreshold> thresholds = thresholdCacheService.getThresholdsByDevice(deviceId);

        if (thresholds == null || thresholds.isEmpty()) {
            return;
        }

        for (AlertThreshold threshold : thresholds) {
            processThreshold(metric, threshold);
        }
    }

    private void processThreshold(Metrics metric, AlertThreshold threshold) {
        Double currentValue = MetricUtil.getValue(metric, threshold.getMetricType());

        if (currentValue == null)
            return;

        boolean isBreached = MetricUtil.isThresholdBreached(currentValue, threshold);

        // Check for the existing active alert for this specific rule
        Optional<Alert> activeAlert = alertRepository
                .findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(
                        metric.getDevice().getId(),
                        threshold.getMetricType()
                );

        if (isBreached && activeAlert.isEmpty()) { // Scenario: New Problem detected
            createAlertAndNotify(metric, threshold, currentValue);
        }
        else if (!isBreached && activeAlert.isPresent()) { // Scenario: Problem recovered
            resolveAlert(activeAlert.get(), currentValue);
        }

        //  Do not send another notification if already breached
    }

    public void createAlertAndNotify(Metrics metric, AlertThreshold threshold, Double currentValue) {
        Alert alert = Alert.builder()
                .device(metric.getDevice())
                .metricType(threshold.getMetricType())
                .severity(threshold.getSeverity())
                .thresholdValue(threshold.getThresholdValue())
                .metricValue(currentValue)
                .message(threshold.getMetricType() + "value " + currentValue + " exceeded the threshold of " + threshold.getThresholdValue())
                .isResolved(false)
                .createdAt(Instant.now())
                .build();

        alertRepository.save(alert);

        log.warn("NEW ALERT: Device {} - {} value is {} (Threshold: {})",
                metric.getDevice().getId(), threshold.getMetricType(), currentValue, threshold.getThresholdValue());

        jobScheduler.enqueue(() -> notificationJobService.processAlertNotifications(alert.getId()));
    }

    public void resolveAlert(Alert alert, Double currentValue) {
        alert.setIsResolved(true);
        alert.setResolvedAt(Instant.now());
        alert.setMetricValue(currentValue);

        alertRepository.save(alert);

        log.info("ALERT RESOLVED: Device {} - {} is back to normal (Value: {})",
                alert.getDevice().getId(), alert.getMetricType(), currentValue);

        final UUID alertId = alert.getId();
        jobScheduler.enqueue(() -> notificationJobService.processAlertNotifications(alertId));
    }
}
