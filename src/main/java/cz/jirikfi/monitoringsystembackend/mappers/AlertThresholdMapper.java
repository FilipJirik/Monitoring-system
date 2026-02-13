package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.models.thresholds.CreateThresholdModel;
import cz.jirikfi.monitoringsystembackend.models.thresholds.ThresholdResponseModel;
import cz.jirikfi.monitoringsystembackend.models.thresholds.UpdateThresholdModel;
import org.springframework.stereotype.Component;

@Component
public class AlertThresholdMapper {

    public ThresholdResponseModel toResponse(AlertThreshold entity) {
        return ThresholdResponseModel.builder()
                .id(entity.getId())
                // deviceId zde není potřeba
                .metricType(entity.getMetricType())
                .operator(entity.getOperator())
                .thresholdValue(entity.getThresholdValue())
                .severity(entity.getSeverity())
                .build();
    }

    public AlertThreshold createToEntity(CreateThresholdModel model, Device device) {
        return AlertThreshold.builder()
                .device(device)
                .metricType(model.getMetricType())
                .operator(model.getOperator())
                .thresholdValue(model.getThresholdValue())
                .severity(model.getSeverity())
                .build();
    }

    public void updateEntity(AlertThreshold entity, UpdateThresholdModel model) {
        entity.setMetricType(model.getMetricType());
        entity.setOperator(model.getOperator());
        entity.setThresholdValue(model.getThresholdValue());
        entity.setSeverity(model.getSeverity());
    }
}