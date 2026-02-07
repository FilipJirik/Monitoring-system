package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.models.thresholds.ThresholdResponse;
import org.springframework.stereotype.Component;

@Component
public class AlertThresholdMapper {

    public ThresholdResponse toResponse(AlertThreshold threshold) {
        return ThresholdResponse.builder()
                .id(threshold.getId())
                .deviceId(threshold.getDevice().getId())
                .metricType(threshold.getMetricType())
                .operator(threshold.getOperator())
                .thresholdValue(threshold.getThresholdValue())
                .severity(threshold.getSeverity())
                .build();
    }
}
