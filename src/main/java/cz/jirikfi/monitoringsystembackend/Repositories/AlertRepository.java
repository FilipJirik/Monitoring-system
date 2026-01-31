package cz.jirikfi.monitoringsystembackend.Repositories;

import cz.jirikfi.monitoringsystembackend.Entities.Alert;
import cz.jirikfi.monitoringsystembackend.Entities.Enums.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(UUID id, MetricType metricType);
}
