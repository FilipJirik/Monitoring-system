package cz.jirikfi.monitoringsystembackend.entities;

import cz.jirikfi.monitoringsystembackend.utils.UuidGenerator;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "metrics", indexes =
        @Index(name = "idx_metrics_device_timestamp", columnList = "device_id, timestamp")
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Metrics extends BaseMetric {
    @Id
    @Builder.Default
    private UUID id = UuidGenerator.v7();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;
}