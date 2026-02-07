package cz.jirikfi.monitoringsystembackend.entities;

import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.enums.ThresholdOperator;
import cz.jirikfi.monitoringsystembackend.utils.UuidGenerator;
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
    private UUID id = UuidGenerator.v7();

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
