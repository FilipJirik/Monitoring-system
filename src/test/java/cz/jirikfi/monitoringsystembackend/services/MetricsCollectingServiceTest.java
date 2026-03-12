package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.exceptions.InternalErrorException;
import cz.jirikfi.monitoringsystembackend.mappers.MetricsMapper;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsCreateRequestDto;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsDetailDto;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsSavedEvent;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsCollectingServiceTest {

    @Mock
    private MetricsMapper metricsMapper;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private MetricsRepository metricRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MetricsCollectingService metricsCollectingService;

    // Captor to inspect the event published by the service (publishEvent accepts Object)
    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID METRIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private Device testDevice;
    private Metrics testMetrics;
    private MetricsCreateRequestDto requestDto;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(UUID.randomUUID()).username("owner").build();
        testDevice = Device.builder().id(DEVICE_ID).name("Server-1").owner(owner).build();

        requestDto = MetricsCreateRequestDto.builder()
                .cpuUsagePercent(75.0)
                .ramUsageMb(4096L)
                .build();

        testMetrics = Metrics.builder()
                .id(METRIC_ID)
                .device(testDevice)
                .cpuUsagePercent(75.0)
                .ramUsageMb(4096L)
                .timestamp(Instant.now())
                .build();
    }

    // =====================================================================
    //  processIncomingMetrics()
    // =====================================================================
    @Nested
    @DisplayName("processIncomingMetrics()")
    class ProcessIncomingMetrics {

        @Test
        @DisplayName("Should save metric, publish event, and broadcast to WebSocket")
        void processIncomingMetrics_ValidRequest_SavesAndPublishesEvent() {
            // Arrange
            when(deviceRepository.getReferenceById(DEVICE_ID)).thenReturn(testDevice);
            when(metricsMapper.toEntity(requestDto, testDevice)).thenReturn(testMetrics);
            when(metricsMapper.toDetailModel(testMetrics)).thenReturn(mock(MetricsDetailDto.class));

            // Act
            metricsCollectingService.processIncomingMetrics(DEVICE_ID, DEVICE_ID, requestDto);

            // Assert
            verify(metricRepository).save(testMetrics);

            // Verify the event was published with the correct metric ID
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            MetricsSavedEvent capturedEvent = (MetricsSavedEvent) eventCaptor.getValue();
            assertThat(capturedEvent.metricId()).isEqualTo(METRIC_ID);

            // Verify WebSocket broadcast was attempted
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/devices/" + DEVICE_ID + "/metrics"), any(MetricsDetailDto.class));
        }

        @Test
        @DisplayName("Should throw InternalErrorException when device IDs do not match")
        void processIncomingMetrics_IdMismatch_ThrowsInternalErrorException() {
            // Arrange
            UUID differentId = UUID.randomUUID();

            // Act & Assert
            assertThatThrownBy(() ->
                    metricsCollectingService.processIncomingMetrics(DEVICE_ID, differentId, requestDto))
                    .isInstanceOf(InternalErrorException.class)
                    .hasMessageContaining("mismatch");
        }
    }

    // =====================================================================
    //  broadcastMetricToFrontend()
    // =====================================================================
    @Nested
    @DisplayName("broadcastMetricToFrontend()")
    class BroadcastMetricToFrontend {

        @Test
        @DisplayName("Should send metric payload to the correct WebSocket destination")
        void broadcastMetricToFrontend_ValidMetric_SendsToCorrectDestination() {
            // Arrange
            MetricsDetailDto payload = mock(MetricsDetailDto.class);
            when(metricsMapper.toDetailModel(testMetrics)).thenReturn(payload);

            // Act
            metricsCollectingService.broadcastMetricToFrontend(testMetrics);

            // Assert — destination includes the device ID
            String expectedDest = "/topic/devices/" + DEVICE_ID + "/metrics";
            verify(messagingTemplate).convertAndSend(expectedDest, payload);
        }

        @Test
        @DisplayName("Should not throw when WebSocket send fails (exception caught)")
        void broadcastMetricToFrontend_SendFails_DoesNotThrow() {
            // Arrange
            when(metricsMapper.toDetailModel(testMetrics)).thenThrow(new RuntimeException("Mapping error"));

            // Act & Assert — no exception propagated
            metricsCollectingService.broadcastMetricToFrontend(testMetrics);
        }
    }
}
