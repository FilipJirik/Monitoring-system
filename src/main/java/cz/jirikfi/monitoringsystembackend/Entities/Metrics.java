package cz.jirikfi.monitoringsystembackend.Entities;

import cz.jirikfi.monitoringsystembackend.Entities.Enums.MetricStatus;
import cz.jirikfi.monitoringsystembackend.Services.GenerateUUIDService;
import jakarta.persistence.*;
import lombok.*;


import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "metrics")
public class Metrics {
    @Id
    @Builder.Default
    private UUID id = GenerateUUIDService.v7();

    @ManyToOne(optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "ram_usage")
    private Double ramUsage;

    @Column(name = "ram_total_mb")
    private Long ramTotalMb;

    @Column(name = "disk_usage")
    private Double diskUsage;

    @Column(name = "disk_total_gb")
    private Long diskTotalGb;

    @Column(name = "network_in_bytes")
    private Long networkInBytes;

    @Column(name = "network_out_bytes")
    private Long networkOutBytes;

    @Column(name = "gpu_usage")
    private Double gpuUsage;

    private Double battery;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20)
    private MetricStatus status = MetricStatus.OK;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}