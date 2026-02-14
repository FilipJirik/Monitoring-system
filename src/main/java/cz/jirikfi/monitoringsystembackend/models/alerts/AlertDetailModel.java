package cz.jirikfi.monitoringsystembackend.models.alerts;

import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
public class AlertDetailModel {
    private UUID id;
    private UUID deviceId;
    private String deviceName;
    private MetricType metricType;
    private AlertSeverity severity;
    private Double metricValue;
    private Double thresholdValue;
    private String message;
    private Boolean isResolved;
    private Instant createdAt;
    private Instant resolvedAt;
}