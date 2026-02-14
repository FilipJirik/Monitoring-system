package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.models.alerts.AlertDetailModel;
import cz.jirikfi.monitoringsystembackend.models.alerts.AlertResponseModel;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {
    public AlertDetailModel toDetailModel(Alert entity) {
        if (entity == null) return null;

        return AlertDetailModel.builder()
                .id(entity.getId())
                .deviceId(entity.getDevice().getId())
                .deviceName(entity.getDevice().getName())
                .metricType(entity.getMetricType())
                .severity(entity.getSeverity())
                .metricValue(entity.getMetricValue())
                .thresholdValue(entity.getThresholdValue())
                .message(entity.getMessage())
                .isResolved(entity.getIsResolved())
                .createdAt(entity.getCreatedAt())
                .resolvedAt(entity.getResolvedAt())
                .build();
    }

    public AlertResponseModel toResponseModel(Alert entity) {
        if (entity == null) return null;

        return AlertResponseModel.builder()
                .id(entity.getId())
                .deviceName(entity.getDevice().getName())
                .metricType(entity.getMetricType())
                .severity(entity.getSeverity())
                .isResolved(entity.getIsResolved())
                .createdAt(entity.getCreatedAt())
                .build();
    }


}
