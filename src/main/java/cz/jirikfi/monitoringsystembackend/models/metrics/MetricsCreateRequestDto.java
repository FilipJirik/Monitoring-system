package cz.jirikfi.monitoringsystembackend.models.metrics;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricsCreateRequestDto {
    @Nullable
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Min(value = 0, message = "CPU usage must be greater than or equal to 0")
    @Max(value = 100, message = "CPU usage must be less than or equal to 100")
    @Nullable
    private Double cpuUsagePercent;

    @Positive(message = "CPU frequency must be positive")
    @Nullable
    private Long cpuFreqAvgMhz;

    @Min(value = -50, message = "CPU temperature must be greater than or equal to -50")
    @Max(value = 150, message = "CPU temperature must be less than or equal to 150")
    @Nullable
    private Double cpuTempCelsius;

    @PositiveOrZero(message = "RAM usage must be greater than or equal to 0")
    @Nullable
    private Long ramUsageMb;

    @Min(value = 0, message = "Disk usage must be greater than or equal to 0")
    @Max(value = 100, message = "Disk usage must be less than or equal to 100")
    @Nullable
    private Double diskUsagePercent;

    @PositiveOrZero(message = "Network input must be greater than or equal to 0")
    @Nullable
    private Double networkInKbps;

    @PositiveOrZero(message = "Network output must be greater than or equal to 0")
    @Nullable
    private Double networkOutKbps;

    @PositiveOrZero(message = "Uptime must be greater than or equal to 0")
    @Nullable
    private Long uptimeSeconds;

    @PositiveOrZero(message = "Process count must be greater than or equal to 0")
    @Nullable
    private Integer processCount;

    @PositiveOrZero(message = "TCP connections count must be greater than or equal to 0")
    @Nullable
    private Integer tcpConnectionsCount;

    @PositiveOrZero(message = "Listening ports count must be greater than or equal to 0")
    @Nullable
    private Integer listeningPortsCount;
}
