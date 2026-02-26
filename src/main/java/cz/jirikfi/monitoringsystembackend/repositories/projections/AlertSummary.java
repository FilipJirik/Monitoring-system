package cz.jirikfi.monitoringsystembackend.repositories.projections;

import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;

import java.time.Instant;
import java.util.UUID;

public interface AlertSummary {
    UUID getId();
    MetricType getMetricType();
    AlertSeverity getSeverity();
    Boolean getIsResolved();
    Instant getCreatedAt();
    DeviceSummary getDevice();

    interface DeviceSummary {
        String getName();
    }
}
