package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.models.alerts.AlertDetailDto;
import cz.jirikfi.monitoringsystembackend.models.alerts.AlertResponseDto;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {
    public AlertDetailDto toDetailModel(Alert entity) {
        if (entity == null) return null;

        return AlertDetailDto.builder()
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

    public AlertResponseDto toResponseModel(Alert entity) {
        if (entity == null) return null;

        return AlertResponseDto.builder()
                .id(entity.getId())
                .deviceName(entity.getDevice().getName())
                .metricType(entity.getMetricType())
                .severity(entity.getSeverity())
                .isResolved(entity.getIsResolved())
                .createdAt(entity.getCreatedAt())
                .build();
    }


}
