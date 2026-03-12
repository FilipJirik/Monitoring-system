package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.*;
import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.enums.ThresholdOperator;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRepository;
import cz.jirikfi.monitoringsystembackend.repositories.AlertThresholdRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import cz.jirikfi.monitoringsystembackend.services.background.DeviceOfflineMonitorJob;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceOfflineMonitorJobTest {

    @Mock
    private AlertThresholdRepository thresholdRepository;
    @Mock
    private MetricsRepository metricsRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertProcessingService alertProcessingService;
    @Mock
    private SystemSettingsCacheService settingsService;
    @InjectMocks
    private DeviceOfflineMonitorJob offlineMonitorJob;

    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final int OFFLINE_THRESHOLD_SECONDS = 120;

    private Device testDevice;
    private AlertThreshold offlineThreshold;
    private SystemSettings testSettings;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(UUID.randomUUID()).username("owner").build();
        testDevice = Device.builder().id(DEVICE_ID).name("Server-1").owner(owner).build();

        offlineThreshold = AlertThreshold.builder()
                .id(UUID.randomUUID())
                .device(testDevice)
                .metricType(MetricType.DEVICE_OFFLINE)
                .operator(ThresholdOperator.GREATER_THAN)
                .thresholdValue(0.0)
                .severity(AlertSeverity.CRITICAL)
                .build();

        testSettings = SystemSettings.builder()
                .id(SystemSettings.ID)
                .rawDataRetentionDays(7)
                .hourlyDataRetentionDays(90)
                .dailyDataRetentionDays(365)
                .deviceOfflineThresholdSeconds(OFFLINE_THRESHOLD_SECONDS)
                .build();
    }

    // =====================================================================
    // checkDevicesOnlineStatus()
    // =====================================================================
    @Nested
    @DisplayName("checkDevicesOnlineStatus()")
    class CheckDevicesOnlineStatus {

        @Test
        @DisplayName("Should do nothing when no DEVICE_OFFLINE thresholds are configured")
        void noThresholds_DoesNothing() {
            // Arrange
            when(settingsService.getSettings()).thenReturn(testSettings);
            when(thresholdRepository.findAllByMetricType(MetricType.DEVICE_OFFLINE))
                    .thenReturn(Collections.emptyList());

            // Act
            offlineMonitorJob.checkDevicesOnlineStatus();

            // Assert
            verifyNoInteractions(metricsRepository, alertRepository, alertProcessingService);
        }

        @Test
        @DisplayName("Should create alert when device has no metrics at all")
        void noMetrics_CreatesAlert() {
            // Arrange
            when(settingsService.getSettings()).thenReturn(testSettings);
            when(thresholdRepository.findAllByMetricType(MetricType.DEVICE_OFFLINE))
                    .thenReturn(List.of(offlineThreshold));
            when(metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(DEVICE_ID))
                    .thenReturn(Optional.empty());
            when(alertRepository.findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(DEVICE_ID,
                    MetricType.DEVICE_OFFLINE))
                    .thenReturn(Optional.empty());

            // Act
            offlineMonitorJob.checkDevicesOnlineStatus();

            // Assert
            verify(alertProcessingService).createAlertAndNotify(testDevice, offlineThreshold, 0.0);
        }

        @Test
        @DisplayName("Should create alert when device's latest metric is older than threshold")
        void staleMetric_NoActiveAlert_CreatesAlert() {
            // Arrange
            Metrics staleMetric = Metrics.builder()
                    .id(UUID.randomUUID())
                    .device(testDevice)
                    .timestamp(Instant.now().minus(5, ChronoUnit.MINUTES))
                    .build();

            when(settingsService.getSettings()).thenReturn(testSettings);
            when(thresholdRepository.findAllByMetricType(MetricType.DEVICE_OFFLINE))
                    .thenReturn(List.of(offlineThreshold));
            when(metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(DEVICE_ID))
                    .thenReturn(Optional.of(staleMetric));
            when(alertRepository.findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(DEVICE_ID,
                    MetricType.DEVICE_OFFLINE))
                    .thenReturn(Optional.empty());

            // Act
            offlineMonitorJob.checkDevicesOnlineStatus();

            // Assert
            verify(alertProcessingService).createAlertAndNotify(testDevice, offlineThreshold, 0.0);
        }

        @Test
        @DisplayName("Should NOT create duplicate alert when device is offline and active alert already exists")
        void staleMetric_ActiveAlertExists_DoesNothing() {
            // Arrange
            Metrics staleMetric = Metrics.builder()
                    .id(UUID.randomUUID())
                    .device(testDevice)
                    .timestamp(Instant.now().minus(5, ChronoUnit.MINUTES))
                    .build();

            Alert existingAlert = Alert.builder()
                    .id(UUID.randomUUID())
                    .device(testDevice)
                    .metricType(MetricType.DEVICE_OFFLINE)
                    .isResolved(false)
                    .build();

            when(settingsService.getSettings()).thenReturn(testSettings);
            when(thresholdRepository.findAllByMetricType(MetricType.DEVICE_OFFLINE))
                    .thenReturn(List.of(offlineThreshold));
            when(metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(DEVICE_ID))
                    .thenReturn(Optional.of(staleMetric));
            when(alertRepository.findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(DEVICE_ID,
                    MetricType.DEVICE_OFFLINE))
                    .thenReturn(Optional.of(existingAlert));

            // Act
            offlineMonitorJob.checkDevicesOnlineStatus();

            // Assert
            verify(alertProcessingService, never()).createAlertAndNotify(any(Device.class), any(), any());
            verify(alertProcessingService, never()).resolveAlert(any(), any());
        }

        @Test
        @DisplayName("Should do nothing when device is online and no active alert exists")
        void recentMetric_NoActiveAlert_DoesNothing() {
            // Arrange
            Metrics recentMetric = Metrics.builder()
                    .id(UUID.randomUUID())
                    .device(testDevice)
                    .timestamp(Instant.now().minus(30, ChronoUnit.SECONDS))
                    .build();

            when(settingsService.getSettings()).thenReturn(testSettings);
            when(thresholdRepository.findAllByMetricType(MetricType.DEVICE_OFFLINE))
                    .thenReturn(List.of(offlineThreshold));
            when(metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(DEVICE_ID))
                    .thenReturn(Optional.of(recentMetric));
            when(alertRepository.findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(DEVICE_ID,
                    MetricType.DEVICE_OFFLINE))
                    .thenReturn(Optional.empty());

            // Act
            offlineMonitorJob.checkDevicesOnlineStatus();

            // Assert
            verify(alertProcessingService, never()).createAlertAndNotify(any(Device.class), any(), any());
            verify(alertProcessingService, never()).resolveAlert(any(), any());
        }

        @Test
        @DisplayName("Should auto-resolve alert when device comes back online")
        void recentMetric_ActiveAlertExists_ResolvesAlert() {
            // Arrange
            Metrics recentMetric = Metrics.builder()
                    .id(UUID.randomUUID())
                    .device(testDevice)
                    .timestamp(Instant.now().minus(30, ChronoUnit.SECONDS))
                    .build();

            Alert activeAlert = Alert.builder()
                    .id(UUID.randomUUID())
                    .device(testDevice)
                    .metricType(MetricType.DEVICE_OFFLINE)
                    .severity(AlertSeverity.CRITICAL)
                    .isResolved(false)
                    .build();

            when(settingsService.getSettings()).thenReturn(testSettings);
            when(thresholdRepository.findAllByMetricType(MetricType.DEVICE_OFFLINE))
                    .thenReturn(List.of(offlineThreshold));
            when(metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(DEVICE_ID))
                    .thenReturn(Optional.of(recentMetric));
            when(alertRepository.findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(DEVICE_ID,
                    MetricType.DEVICE_OFFLINE))
                    .thenReturn(Optional.of(activeAlert));

            // Act
            offlineMonitorJob.checkDevicesOnlineStatus();

            // Assert
            verify(alertProcessingService).resolveAlert(activeAlert, 0.0);
        }

        @Test
        @DisplayName("Should continue processing other devices when one throws an exception")
        void exceptionInOneDevice_ContinuesWithOthers() {
            // Arrange
            Device device2 = Device.builder()
                    .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                    .name("Server-2")
                    .owner(User.builder().id(UUID.randomUUID()).username("owner").build())
                    .build();

            AlertThreshold threshold2 = AlertThreshold.builder()
                    .id(UUID.randomUUID())
                    .device(device2)
                    .metricType(MetricType.DEVICE_OFFLINE)
                    .operator(ThresholdOperator.GREATER_THAN)
                    .thresholdValue(0.0)
                    .severity(AlertSeverity.WARNING)
                    .build();

            when(settingsService.getSettings()).thenReturn(testSettings);
            when(thresholdRepository.findAllByMetricType(MetricType.DEVICE_OFFLINE))
                    .thenReturn(List.of(offlineThreshold, threshold2));

            // First device throws exception
            when(metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(DEVICE_ID))
                    .thenThrow(new RuntimeException("DB error"));

            // Second device has no metrics (offline)
            when(metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(device2.getId()))
                    .thenReturn(Optional.empty());
            when(alertRepository.findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(device2.getId(),
                    MetricType.DEVICE_OFFLINE))
                    .thenReturn(Optional.empty());

            // Act
            offlineMonitorJob.checkDevicesOnlineStatus();

            // Assert - second device should still be processed
            verify(alertProcessingService).createAlertAndNotify(device2, threshold2, 0.0);
        }
    }
}
