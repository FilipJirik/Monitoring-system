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
public class MetricsCreateModel {
    @Nullable
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Min(0)
    @Max(100)
    @Nullable // for errors
    private Double cpuUsagePercent;

    @Positive
    @Nullable
    private Long cpuFreqAvgMhz;

    @Min(-50)
    @Max(150)
    @Nullable
    private Double cpuTempCelsius;

    @PositiveOrZero
    @Nullable
    private Long ramUsageMb;

    @Min(0)
    @Max(100)
    @Nullable
    private Double diskUsagePercent;

    @PositiveOrZero
    @Nullable
    private Double networkInKbps;

    @PositiveOrZero
    @Nullable
    private Double networkOutKbps;

    @PositiveOrZero
    @Nullable
    private Long uptimeSeconds;
}
