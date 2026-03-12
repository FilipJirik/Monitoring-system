package cz.jirikfi.monitoringsystembackend.repositories;

import cz.jirikfi.monitoringsystembackend.entities.BaseMetric;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.repositories.projections.AggregatedMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricsRepository extends JpaRepository<Metrics, UUID> {

    Optional<Metrics> findFirstByDeviceIdOrderByTimestampDesc(UUID deviceId);

    List<? extends BaseMetric> findAllByDeviceIdAndTimestampBetweenOrderByTimestampAsc(UUID deviceId, Instant from, Instant to);

    @Query("""
        SELECT
            AVG(m.cpuUsagePercent) as avgCpuUsage,
            AVG(m.cpuTempCelsius) as avgCpuTemp,
            AVG(m.cpuFreqAvgMhz) as avgCpuFreq,
            AVG(m.ramUsageMb) as avgRamUsage,
            AVG(m.diskUsagePercent) as avgDiskUsage,
            AVG(m.networkInKbps) as avgNetworkIn,
            AVG(m.networkOutKbps) as avgNetworkOut,
            MAX(m.uptimeSeconds) as maxUptime,
            AVG(m.processCount) as avgProcessCount,
            AVG(m.tcpConnectionsCount) as avgTcpConnectionsCount,
            AVG(m.listeningPortsCount) as avgListeningPortsCount
        FROM Metrics m
        WHERE m.device.id = :deviceId
          AND m.timestamp >= :from
          AND m.timestamp < :to
    """)
    AggregatedMetric findAggregatedValues(@Param("deviceId") UUID deviceId,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to);
    void deleteByTimestampBefore(Instant timestamp);
}
