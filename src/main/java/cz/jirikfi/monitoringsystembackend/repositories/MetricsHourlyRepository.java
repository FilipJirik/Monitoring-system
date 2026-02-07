package cz.jirikfi.monitoringsystembackend.repositories;

import cz.jirikfi.monitoringsystembackend.entities.BaseMetric;
import cz.jirikfi.monitoringsystembackend.entities.MetricsHourly;
import cz.jirikfi.monitoringsystembackend.repositories.projections.AggregatedMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MetricsHourlyRepository extends JpaRepository<MetricsHourly, UUID> {
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
            MAX(m.uptimeSeconds) as maxUptime
        FROM Metrics m
        WHERE m.device.id = :deviceId 
          AND m.timestamp >= :from 
          AND m.timestamp < :to
    """)
    AggregatedMetric findAggregatedValues(UUID deviceId, Instant from, Instant to);
    void deleteByTimestampBefore(Instant timestamp);
}
