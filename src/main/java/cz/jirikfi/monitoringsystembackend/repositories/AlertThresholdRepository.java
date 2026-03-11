package cz.jirikfi.monitoringsystembackend.repositories;

import cz.jirikfi.monitoringsystembackend.entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertThresholdRepository extends JpaRepository<AlertThreshold, UUID> {

    List<AlertThreshold> findByDevice_IdOrderByMetricType(UUID deviceId);

    List<AlertThreshold> findAllByDeviceId(UUID deviceId);

    @Query("SELECT t FROM AlertThreshold t JOIN FETCH t.device WHERE t.metricType = :metricType")
    List<AlertThreshold> findAllByMetricType(@Param("metricType") MetricType metricType);

    @Query("SELECT t FROM AlertThreshold t JOIN FETCH t.device WHERE t.id = :id")
    Optional<AlertThreshold> findByIdWithDevice(@Param("id") UUID id);
}
