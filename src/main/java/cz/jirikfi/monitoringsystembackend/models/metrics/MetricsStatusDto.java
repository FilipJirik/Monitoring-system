package cz.jirikfi.monitoringsystembackend.models.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsStatusDto {
    private boolean isOnline;
    private Instant lastSeen;
    private Long uptimeSeconds;

    private Double currentCpuUsage;
    private Long currentCpuFreqMhz;
    private Double currentRamUsage;
    private Double currentDiskUsage;
    private Double currentCpuTemp;
    private Double currentNetworkInKbps;
    private Double currentNetworkOutKbps;
    private Integer processCount;
    private Integer tcpConnectionsCount;
    private Integer listeningPortsCount;
}
