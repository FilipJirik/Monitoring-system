package cz.jirikfi.monitoringsystembackend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseMetric implements Persistable<UUID> {
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

    @Override
    public boolean isNew() { // used to inform Hibernate that it will never be updated - faster
        return true;
    }
}