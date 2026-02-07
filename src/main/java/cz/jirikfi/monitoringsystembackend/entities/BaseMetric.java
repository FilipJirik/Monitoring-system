package cz.jirikfi.monitoringsystembackend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseMetric {
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