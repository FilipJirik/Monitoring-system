package cz.jirikfi.monitoringsystembackend.Entities;

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
    @GeneratedValue
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

    @Column(name = "disk_usage")
    private Double diskUsage;

    @Column(name = "network_usage")
    private Long networkUsage;

    @Column(name = "gpu_usage")
    private Double gpuUsage;

    private Double battery;
}