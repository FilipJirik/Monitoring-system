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
public class AlertResponseDto {
    private UUID id;
    private String deviceName;
    private MetricType metricType;
    private AlertSeverity severity;
    private Boolean isResolved;
    private Instant createdAt;
}