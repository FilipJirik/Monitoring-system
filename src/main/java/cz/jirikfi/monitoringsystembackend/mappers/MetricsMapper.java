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

    public MetricsStatusDto toStatusModel(Metrics entity, boolean isOnline) {
        if (entity == null) {
            return MetricsStatusDto.builder()
                    .isOnline(false)
                    .build();
        }

        return MetricsStatusDto.builder()
                .isOnline(isOnline)
                .lastSeen(entity.getTimestamp())
                .uptimeSeconds(entity.getUptimeSeconds())
                .currentCpuUsage(entity.getCpuUsagePercent())
                .currentCpuTemp(entity.getCpuTempCelsius())
                .currentCpuFreqMhz(entity.getCpuFreqAvgMhz())
                .currentRamUsage(entity.getRamUsageMb() != null ? entity.getRamUsageMb().doubleValue() : 0.0)
                .currentDiskUsage(entity.getDiskUsagePercent())
                .currentNetworkInKbps(entity.getNetworkInKbps())
                .currentNetworkOutKbps(entity.getNetworkOutKbps())
                .processCount(entity.getProcessCount())
                .tcpConnectionsCount(entity.getTcpConnectionsCount())
                .listeningPortsCount(entity.getListeningPortsCount())
                .build();
    }

    public Metrics toEntity(MetricsCreateRequestDto model, Device device) {
        return Metrics.builder()
                .device(device)
                .timestamp(Instant.now())
                .uptimeSeconds(model.getUptimeSeconds())
                .cpuUsagePercent(model.getCpuUsagePercent())
                .cpuFreqAvgMhz(model.getCpuFreqAvgMhz())
                .cpuTempCelsius(model.getCpuTempCelsius())
                .ramUsageMb(model.getRamUsageMb())
                .diskUsagePercent(model.getDiskUsagePercent())
                .networkInKbps(model.getNetworkInKbps())
                .networkOutKbps(model.getNetworkOutKbps())
                .processCount(model.getProcessCount())
                .tcpConnectionsCount(model.getTcpConnectionsCount())
                .listeningPortsCount(model.getListeningPortsCount())
                .build();
    }
    public MetricsDetailDto toDetailModel(Metrics entity) {
        return MetricsDetailDto.builder()
                .timestamp(entity.getTimestamp())
                .uptimeSeconds(entity.getUptimeSeconds())
                .cpuUsagePercent(entity.getCpuUsagePercent())
                .cpuTempCelsius(entity.getCpuTempCelsius())
                .ramUsageMb(entity.getRamUsageMb())
                .diskUsagePercent(entity.getDiskUsagePercent())
                .networkInKbps(entity.getNetworkInKbps())
                .networkOutKbps(entity.getNetworkOutKbps())
                .cpuFreqAvgMhz(entity.getCpuFreqAvgMhz())
                .processCount(entity.getProcessCount())
                .tcpConnectionsCount(entity.getTcpConnectionsCount())
                .listeningPortsCount(entity.getListeningPortsCount())
                .build();
    }

    public MetricsHistoryDto toHistoryModel(List<? extends BaseMetric> data, MetricType type) {
        List<DataPointDto> points = data.stream()
                .map(m -> new DataPointDto(m.getTimestamp(), MetricUtil.getValue(m, type)))
                .toList();

        return MetricsHistoryDto.builder()
                .type(type)
                .label(type.getLabel())
                .unit(type.getUnit())
                .data(points)
                .build();
    }
}