package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsCreateModel;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MetricsMapper {

    public Metrics toEntity(MetricsCreateModel model, Device device) {
        return Metrics.builder()
                .device(device) // Nastavíme vazbu
                .timestamp(model.getTimestamp() != null ? model.getTimestamp() : Instant.now())
                .cpuUsagePercent(model.getCpuUsagePercent())
                .cpuFreqAvgMhz(model.getCpuFreqAvgMhz())
                .cpuTempCelsius(model.getCpuTempCelsius())
                .ramUsageMb(model.getRamUsageMb())
                .diskUsagePercent(model.getDiskUsagePercent())
                .networkInKbps(model.getNetworkInKbps())
                .networkOutKbps(model.getNetworkOutKbps())
                .uptimeSeconds(model.getUptimeSeconds())
                .build();
    }

    public MetricsCreateModel toModel(Metrics metrics) {
        return MetricsCreateModel.builder()
                .timestamp(metrics.getTimestamp())
                .cpuUsagePercent(metrics.getCpuUsagePercent())
                .cpuFreqAvgMhz(metrics.getCpuFreqAvgMhz())
                .cpuTempCelsius(metrics.getCpuTempCelsius())
                .ramUsageMb(metrics.getRamUsageMb())
                .diskUsagePercent(metrics.getDiskUsagePercent())
                .networkInKbps(metrics.getNetworkInKbps())
                .networkOutKbps(metrics.getNetworkOutKbps())
                .uptimeSeconds(metrics.getUptimeSeconds())
                .build();
    }
}