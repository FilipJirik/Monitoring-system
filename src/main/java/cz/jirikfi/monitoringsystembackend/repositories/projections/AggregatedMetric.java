package cz.jirikfi.monitoringsystembackend.repositories.projections;

public interface AggregatedMetric {
    Double getAvgCpuUsage();
    Double getAvgCpuTemp();
    Double getAvgCpuFreq();
    Double getAvgRamUsage();
    Double getAvgDiskUsage();
    Double getAvgNetworkIn();
    Double getAvgNetworkOut();
    Long getMaxUptime();
    Double getAvgProcessCount();
    Double getAvgTcpConnectionsCount();
    Double getAvgListeningPortsCount();
}
