package cz.jirikfi.monitoringsystembackend.Entities;

import cz.jirikfi.monitoringsystembackend.Entities.Enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.Entities.Enums.MetricType;
import cz.jirikfi.monitoringsystembackend.Entities.Enums.ThresholdOperator;
import cz.jirikfi.monitoringsystembackend.Services.GenerateUUIDService;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "alert_thresholds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertThreshold {
    @Id
    @Builder.Default
    private UUID id = GenerateUUIDService.v7();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThresholdOperator operator;

    @Column(nullable = false)
    private Double thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;
}
