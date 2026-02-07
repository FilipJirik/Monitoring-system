package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.MetricsMapper;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsCreateModel;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import lombok.RequiredArgsConstructor;
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
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Metrics saveMetrics(UUID deviceId, String rawApiKey, MetricsCreateModel model) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        if (!passwordEncoder.matches(rawApiKey, device.getApiKey())) {
            throw new BadCredentialsException("Invalid API Key for device " + deviceId);
        }

        device.setLastSeen(Instant.now());
        deviceRepository.save(device);

        Metrics metrics = metricsMapper.toEntity(model, device);
        metricRepository.save(metrics);
        return metrics;
    }
}
