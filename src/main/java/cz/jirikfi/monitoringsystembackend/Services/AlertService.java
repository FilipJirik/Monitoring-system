package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Alert;
import cz.jirikfi.monitoringsystembackend.Entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.Entities.Enums.MetricType;
import cz.jirikfi.monitoringsystembackend.Entities.Metrics;
import cz.jirikfi.monitoringsystembackend.Repositories.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class AlertService {

    private final ThresholdCacheService thresholdCache;
    private final AlertRepository alertRepository;

    @Async
    @Transactional
    public void checkThresholdsAsync(Metrics metric) {
        UUID deviceId = metric.getDevice().getId();

        List<AlertThreshold> thresholds = thresholdCache.getThresholds(deviceId);

        if (thresholds == null || thresholds.isEmpty()) {
            return;
        }

        for (AlertThreshold threshold : thresholds) {
            processThreshold(metric, threshold);
        }
    }

    private void processThreshold(Metrics metric, AlertThreshold threshold) {
        Double currentValue = getMetricValue(metric, threshold.getMetricType());

        if (currentValue == null)
            return;

        boolean isBreached = isThresholdBreached(currentValue, threshold);

        Optional<Alert> activeAlert = alertRepository
                .findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(
                        metric.getDevice().getId(),
                        threshold.getMetricType()
                );

        if (isBreached && activeAlert.isEmpty()) {
            createAlert(metric, threshold, currentValue);
        } else if (!isBreached && activeAlert.isPresent()) {
            resolveAlert(activeAlert.get(), currentValue);
        }
    }

    private void createAlert(Metrics metric, AlertThreshold threshold, Double currentValue) {
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
    }

    private void resolveAlert(Alert alert, Double currentValue) {
        alert.setIsResolved(true);
        alert.setResolvedAt(Instant.now());
        alert.setMetricValue(currentValue);

        alertRepository.save(alert);

        log.info("ALERT RESOLVED: Device {} - {} is back to normal (Value: {})",
                alert.getDevice().getId(), alert.getMetricType(), currentValue);
    }

    private boolean isThresholdBreached(Double value, AlertThreshold threshold) {
        return switch (threshold.getOperator()) {
            case GREATER_THAN -> value > threshold.getThresholdValue();
            case LESS_THAN -> value < threshold.getThresholdValue();
            case EQUAL -> value.equals(threshold.getThresholdValue());
        };
    }

    private Double getMetricValue(Metrics metric, MetricType type) {
        return switch (type) {
            case CPU_USAGE -> metric.getCpuUsagePercent();
            case CPU_TEMP -> metric.getCpuTempCelsius();
            case CPU_FREQ -> metric.getCpuFreqAvgMhz() != null ? metric.getCpuFreqAvgMhz().doubleValue() : null;
            case RAM_USAGE -> metric.getRamUsageMb() != null ? metric.getRamUsageMb().doubleValue() : null;
            case DISK_USAGE -> metric.getDiskUsagePercent();
            case NETWORK_IN -> metric.getNetworkInKbps();
            case NETWORK_OUT -> metric.getNetworkOutKbps();
            case UPTIME -> metric.getUptimeSeconds() != null ? metric.getUptimeSeconds().doubleValue() : null;
        };
    }
}
