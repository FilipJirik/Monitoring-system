package cz.jirikfi.monitoringsystembackend.Entities;

import cz.jirikfi.monitoringsystembackend.Services.GenerateUUIDService;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "metrics", indexes = {
        @Index(name = "idx_metrics_device_timestamp", columnList = "device_id, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metrics {
    @Id
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private UUID id = GenerateUUIDService.v7();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    @Column(name = "cpu_temp_celsius")
    private Double cpuTempCelsius;

    @Column(name = "cpu_freq_avg_mhz")
    private Long cpuFreqAvgMhz;

    @Column(name = "ram_usage_mb")
    private Long ramUsageMb;

    @Column(name = "disk_usage_percent")
    private Double diskUsagePercent;

    @Column(name = "network_in_kbps")
    private Double networkInKbps;

    @Column(name = "network_out_kbps")
    private Double networkOutKbps;

    @Column(name = "uptime_seconds")
    private Long uptimeSeconds;
}