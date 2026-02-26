package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.enums.ThresholdOperator;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsSavedEvent;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import cz.jirikfi.monitoringsystembackend.services.background.NotificationJobService;
import cz.jirikfi.monitoringsystembackend.utils.MetricUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
    private final MetricsRepository metricsRepository;

    private final JobScheduler jobScheduler;


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMetricsSaved(MetricsSavedEvent event) {

        Metrics metric = metricsRepository.findById(event.metricId()).orElseThrow();

        checkThresholds(metric);
    }

    public void checkThresholds(Metrics metric) {
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

    private String buildAlertMessage(AlertThreshold threshold, Double currentValue) {
        String operatorSymbol;
        String comparisonText = switch (threshold.getOperator()) {
            case GREATER_THAN -> {
                operatorSymbol = ">";
                yield "exceeded";
            }
            case LESS_THAN -> {
                operatorSymbol = "<";
                yield "fell below";
            }
            case EQUAL -> {
                operatorSymbol = "==";
                yield "equaled";
            }
            case IS_NULL -> {
                operatorSymbol = "";
                yield "is missing (null)";
            }
            case IS_NOT_NULL -> {
                operatorSymbol = "";
                yield "is present (not null)";
            }
            default -> {
                operatorSymbol = "";
                yield "breached";
            }
        };
        
        if (threshold.getOperator() == ThresholdOperator.IS_NULL || threshold.getOperator() == ThresholdOperator.IS_NOT_NULL) {
            return String.format("%s value %s",
                    threshold.getMetricType().getLabel(),
                    comparisonText);
        }

        return String.format("%s value (%s) %s the threshold %s %s",
                threshold.getMetricType().getLabel(),
                currentValue,
                comparisonText,
                operatorSymbol,
                threshold.getThresholdValue());
    }

    public void createAlertAndNotify(Metrics metric, AlertThreshold threshold, Double currentValue) {
        String message = buildAlertMessage(threshold, currentValue);

        Alert alert = Alert.builder()
                .device(metric.getDevice())
                .metricType(threshold.getMetricType())
                .severity(threshold.getSeverity())
                .thresholdValue(threshold.getThresholdValue())
                .metricValue(currentValue)
                .message(message)
                .isResolved(false)
                .createdAt(Instant.now())
                .build();

        alertRepository.save(alert);

        log.warn("NEW ALERT: Device {} - {}", metric.getDevice().getName(), message);

        jobScheduler.enqueue(() -> notificationJobService.processAlertNotifications(alert.getId()));
    }

    public void resolveAlert(Alert alert, Double currentValue) {
        alert.setIsResolved(true);
        alert.setResolvedAt(Instant.now());
        alert.setMetricValue(currentValue); // Update with the value that caused resolution
        alert.setMessage(String.format("%s value (%s) is back to normal.",
                alert.getMetricType().getLabel(),
                currentValue)); // Set a clear resolved message

        alertRepository.save(alert);

        log.info("ALERT RESOLVED: Device {} - {} is back to normal (Value: {})",
                alert.getDevice().getName(), alert.getMetricType().getLabel(), currentValue);

        final UUID alertId = alert.getId();
        jobScheduler.enqueue(() -> notificationJobService.processAlertNotifications(alertId));
    }
}
