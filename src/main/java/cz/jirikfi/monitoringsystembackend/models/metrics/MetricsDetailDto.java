package cz.jirikfi.monitoringsystembackend.models.metrics;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class MetricsDetailDto {
    private Instant timestamp;
    private Double cpuUsagePercent;
    private Double cpuTempCelsius;
    private Long cpuFreqAvgMhz;
    private Long ramUsageMb;
    private Double diskUsagePercent;
    private Double networkInKbps;
    private Double networkOutKbps;
    private Long uptimeSeconds;

}
