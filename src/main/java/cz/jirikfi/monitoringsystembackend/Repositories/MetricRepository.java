package cz.jirikfi.monitoringsystembackend.Repositories;

import cz.jirikfi.monitoringsystembackend.Entities.Metrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MetricRepository extends JpaRepository<Metrics, UUID> {

    @Query("SELECT m FROM Metrics m WHERE m.device.id = :deviceId " +
            "ORDER BY m.timestamp DESC")
    List<Metrics> findByDeviceIdOrderByTimestampDesc(UUID deviceId);
}
