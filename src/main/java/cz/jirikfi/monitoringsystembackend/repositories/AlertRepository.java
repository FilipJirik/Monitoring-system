package cz.jirikfi.monitoringsystembackend.repositories;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.repositories.projections.AlertSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(UUID id, MetricType metricType);


    @Query(value = """
        SELECT DISTINCT a FROM Alert a
        LEFT JOIN AlertRecipient ar ON a.device.id = ar.device.id
        WHERE (:isAdmin = true OR ar.user.id = :userId)
        AND (:isResolved IS NULL OR a.isResolved = :isResolved)
        AND (:severity IS NULL OR a.severity = :severity)
        ORDER BY a.createdAt DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT a) FROM Alert a
        LEFT JOIN AlertRecipient ar ON a.device.id = ar.device.id
        WHERE (:isAdmin = true OR ar.user.id = :userId)
        AND (:isResolved IS NULL OR a.isResolved = :isResolved)
        AND (:severity IS NULL OR a.severity = :severity)
""")
    Page<AlertSummary> findAllFiltered(
            @Param("userId") UUID userId,
            @Param("isAdmin") boolean isAdmin,
            @Param("isResolved") Boolean isResolved,
            @Param("severity") AlertSeverity severity,
            Pageable pageable);
}
