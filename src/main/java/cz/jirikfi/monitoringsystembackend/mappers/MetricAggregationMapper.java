package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.BaseMetric;
import cz.jirikfi.monitoringsystembackend.repositories.projections.AggregatedMetric;
import org.springframework.stereotype.Component;

@Component
public class MetricAggregationMapper {

    public <T extends BaseMetric> void mapAggregatedStats(AggregatedMetric source, T target) {
        if (source == null) {
            return;
        }
        target.setCpuUsagePercent(source.getAvgCpuUsage());
        target.setCpuTempCelsius(source.getAvgCpuTemp());
        target.setDiskUsagePercent(source.getAvgDiskUsage());
        target.setNetworkInKbps(source.getAvgNetworkIn());
        target.setNetworkOutKbps(source.getAvgNetworkOut());
        target.setUptimeSeconds(source.getMaxUptime());

        target.setCpuFreqAvgMhz(toLong(source.getAvgCpuFreq()));
        target.setRamUsageMb(toLong(source.getAvgRamUsage()));

        target.setProcessCount(toInteger(source.getAvgProcessCount()));
        target.setTcpConnectionsCount(toInteger(source.getAvgTcpConnectionsCount()));
        target.setListeningPortsCount(toInteger(source.getAvgListeningPortsCount()));
    }

    private Long toLong(Double value) {
        return value != null ? Math.round(value) : null;
    }

    private Integer toInteger(Double value) {
        return value != null ? (int) Math.round(value) : null;
    }
}
