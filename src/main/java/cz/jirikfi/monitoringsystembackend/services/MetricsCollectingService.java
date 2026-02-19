package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.exceptions.InternalErrorException;
import cz.jirikfi.monitoringsystembackend.mappers.MetricsMapper;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsCreateRequestDto;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsDetailDto;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsSavedEvent;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;



@Service
@RequiredArgsConstructor
public class MetricsCollectingService {

    private final MetricsMapper metricsMapper;
    private final DeviceRepository deviceRepository;
    private final MetricsRepository metricRepository;
    private final AlertProcessingService alertProcessingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private final org.slf4j.Logger _logger = org.slf4j.LoggerFactory.getLogger(MetricsCollectingService.class);

    private static final String WEBSOCKET_METRICS_PATH = "/topic/devices/%s/metrics";

    @Async
    @Transactional
    public void processIncomingMetrics(UUID deviceId, UUID principalDeviceId, MetricsCreateRequestDto model) {
        if (!deviceId.equals(principalDeviceId)) {
            throw new InternalErrorException("Device ID mismatch");
        }

        Device device = deviceRepository.getReferenceById(deviceId);

        Metrics metrics = metricsMapper.toEntity(model, device);
        metricRepository.save(metrics);

        eventPublisher.publishEvent(new MetricsSavedEvent(metrics.getId()));

        broadcastMetricToFrontend(metrics);
    }

    @Async
    public void broadcastMetricToFrontend(Metrics metric) {
        try {
            UUID deviceId = metric.getDevice().getId();
            String destination = String.format(WEBSOCKET_METRICS_PATH, deviceId);

            MetricsDetailDto payload = metricsMapper.toDetailModel(metric);
            messagingTemplate.convertAndSend(destination, payload);

        } catch (Exception e) {
            _logger.error("Error while sending WebSocket metrics", e);
        }
    }
}
