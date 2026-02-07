package cz.jirikfi.monitoringsystembackend.repositories;

import cz.jirikfi.monitoringsystembackend.entities.BaseMetric;
import cz.jirikfi.monitoringsystembackend.entities.MetricsDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MetricsDailyRepository extends JpaRepository<MetricsDaily, UUID> {
    List<? extends BaseMetric> findAllByDeviceIdAndTimestampBetweenOrderByTimestampAsc(UUID deviceId, Instant from, Instant to);
    void deleteByTimestampBefore(Instant timestamp);
}
