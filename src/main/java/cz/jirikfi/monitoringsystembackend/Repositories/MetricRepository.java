package cz.jirikfi.monitoringsystembackend.Repositories;

import cz.jirikfi.monitoringsystembackend.Entities.Metrics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricRepository extends JpaRepository<Metrics, UUID> {

    Optional<Metrics> findFirstByDeviceIdOrderByTimestampDesc(UUID deviceId);

    Slice<Metrics> findByDeviceIdOrderByTimestampDesc(UUID deviceId, Pageable pageable);

}
