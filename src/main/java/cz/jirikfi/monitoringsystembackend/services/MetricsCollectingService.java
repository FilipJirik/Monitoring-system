package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.exceptions.InternalErrorException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.exceptions.UnauthorizedException;
import cz.jirikfi.monitoringsystembackend.mappers.MetricsMapper;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsCreateModel;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;



@Service
@RequiredArgsConstructor
public class MetricsCollectingService {

    private final MetricsMapper metricsMapper;
    private final DeviceRepository deviceRepository;
    private final MetricsRepository metricRepository;
    private final AlertService alertService;

    @Async
    @Transactional
    public void processIncomingMetrics(UUID deviceId, UUID principalDeviceId, MetricsCreateModel model) {
        if (!deviceId.equals(principalDeviceId)) {
            throw new InternalErrorException("Device ID mismatch");
        }

        Device device = deviceRepository.getReferenceById(deviceId);

        Metrics metrics = metricsMapper.toEntity(model, device);
        metricRepository.save(metrics);

        alertService.checkThresholdsAsync(metrics);
    }
}
