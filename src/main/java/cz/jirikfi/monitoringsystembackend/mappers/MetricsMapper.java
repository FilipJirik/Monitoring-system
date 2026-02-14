package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.BaseMetric;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.models.metrics.*;
import cz.jirikfi.monitoringsystembackend.utils.MetricUtil;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class MetricsMapper {

    public MetricsStatusModel toStatusModel(Metrics entity, boolean isOnline) {
        if (entity == null) {
            return MetricsStatusModel.builder()
                    .isOnline(false)
                    .build();
        }

        return MetricsStatusModel.builder()
                .isOnline(isOnline)
                .lastSeen(entity.getTimestamp())
                .uptimeSeconds(entity.getUptimeSeconds())
                .currentCpuUsage(entity.getCpuUsagePercent())
                .currentCpuTemp(entity.getCpuTempCelsius())
                .currentCpuFreqMhz(entity.getCpuFreqAvgMhz())
                .currentRamUsage(entity.getRamUsageMb() != null ? entity.getRamUsageMb().doubleValue() : 0.0)
                .currentDiskUsage(entity.getDiskUsagePercent())
                .build();
    }

    public Metrics toEntity(MetricsCreateModel model, Device device) {
        return Metrics.builder()
                .device(device)
                .timestamp(Instant.now())
                .uptimeSeconds(model.getUptimeSeconds())
                .cpuUsagePercent(model.getCpuUsagePercent())
                .cpuFreqAvgMhz(model.getCpuFreqAvgMhz())
                .cpuTempCelsius(model.getCpuTempCelsius())
                .ramUsageMb(model.getRamUsageMb())
                .diskUsagePercent(model.getDiskUsagePercent())
                .build();
    }
    public MetricsDetailModel toDetailModel(Metrics entity) {
        return MetricsDetailModel.builder()
                .timestamp(entity.getTimestamp())
                .uptimeSeconds(entity.getUptimeSeconds())
                .cpuUsagePercent(entity.getCpuUsagePercent())
                .cpuTempCelsius(entity.getCpuTempCelsius())
                .ramUsageMb(entity.getRamUsageMb())
                .diskUsagePercent(entity.getDiskUsagePercent())
                .cpuFreqAvgMhz(entity.getCpuFreqAvgMhz())
                .build();
    }

    public MetricsHistoryModel toHistoryModel(List<? extends BaseMetric> data, MetricType type) {
        List<DataPoint> points = data.stream()
                .map(m -> new DataPoint(m.getTimestamp(), MetricUtil.getValue(m, type)))
                .toList();

        return MetricsHistoryModel.builder()
                .type(type)
                .label(type.getLabel())
                .unit(type.getUnit())
                .data(points)
                .build();
    }
}