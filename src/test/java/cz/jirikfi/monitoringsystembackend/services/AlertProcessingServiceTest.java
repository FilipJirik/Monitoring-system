package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.Metrics;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.enums.ThresholdOperator;
import cz.jirikfi.monitoringsystembackend.models.metrics.MetricsSavedEvent;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import cz.jirikfi.monitoringsystembackend.services.background.NotificationJobService;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertProcessingServiceTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private ThresholdCacheService thresholdCacheService;
    @Mock
    private MetricsRepository metricsRepository;
    @Mock
    private JobScheduler jobScheduler;

    @InjectMocks
    private AlertProcessingService alertProcessingService;

    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID METRIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID ALERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    private Device testDevice;
    private Metrics testMetric;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(UUID.randomUUID()).username("owner").build();
        testDevice = Device.builder().id(DEVICE_ID).name("Server-1").owner(owner).build();

        testMetric = Metrics.builder()
                .id(METRIC_ID)
                .device(testDevice)
                .cpuUsagePercent(95.0)
                .ramUsageMb(8000L)
                .diskUsagePercent(50.0)
                .build();
    }

    // =====================================================================
    // onMetricsSaved()
    // =====================================================================
    @Nested
    @DisplayName("onMetricsSaved()")
    class OnMetricsSaved {

        @Test
        @DisplayName("Should fetch metric by ID and delegate to checkThresholds")
        void onMetricsSaved_ValidEvent_FetchesMetricAndProcesses() {
            // Arrange
            MetricsSavedEvent event = new MetricsSavedEvent(METRIC_ID);

            when(metricsRepository.findById(METRIC_ID)).thenReturn(Optional.of(testMetric));
            // No thresholds configured - checkThresholds returns early
            when(thresholdCacheService.getThresholdsByDevice(DEVICE_ID)).thenReturn(Collections.emptyList());

            // Act
            alertProcessingService.onMetricsSaved(event);

            // Assert
            verify(metricsRepository).findById(METRIC_ID);
            verify(thresholdCacheService).getThresholdsByDevice(DEVICE_ID);
        }
    }

    // =====================================================================
    // checkThresholds()
    // =====================================================================
    @Nested
    @DisplayName("checkThresholds()")
    class CheckThresholds {

        @Test
        @DisplayName("Should skip processing when no thresholds are configured")
        void checkThresholds_NoThresholds_DoesNothing() {
            // Arrange
            when(thresholdCacheService.getThresholdsByDevice(DEVICE_ID)).thenReturn(Collections.emptyList());

            // Act
            alertProcessingService.checkThresholds(testMetric);

            // Assert - no alerts created or resolved
            verifyNoInteractions(alertRepository, jobScheduler);
        }

        @Test
        @DisplayName("Should create alert when threshold is breached and no active alert exists")
        void checkThresholds_ThresholdBreached_NoActiveAlert_CreatesNewAlert() {
            // Arrange
            AlertThreshold threshold = AlertThreshold.builder()
                    .metricType(MetricType.CPU_USAGE)
                    .operator(ThresholdOperator.GREATER_THAN)
                    .thresholdValue(90.0)
                    .severity(AlertSeverity.CRITICAL)
                    .device(testDevice)
                    .build();

            when(thresholdCacheService.getThresholdsByDevice(DEVICE_ID)).thenReturn(List.of(threshold));
            // No active alert exists for this metric type
            when(alertRepository.findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(DEVICE_ID, MetricType.CPU_USAGE))
                    .thenReturn(Optional.empty());

            // Act
            alertProcessingService.checkThresholds(testMetric);

            // Assert - a new alert should be created and saved
            verify(alertRepository).save(any(Alert.class));
            verify(jobScheduler).enqueue(any(org.jobrunr.jobs.lambdas.JobLambda.class));
        }

        @Test
        @DisplayName("Should do nothing when threshold is breached but active alert already exists")
        void checkThresholds_ThresholdBreached_ActiveAlertExists_DoesNothing() {
            // Arrange
            AlertThreshold threshold = AlertThreshold.builder()
                    .metricType(MetricType.CPU_USAGE)
                    .operator(ThresholdOperator.GREATER_THAN)
                    .thresholdValue(90.0)
                    .severity(AlertSeverity.CRITICAL)
                    .device(testDevice)
                    .build();

            Alert existingAlert = Alert.builder()
                    .id(ALERT_ID).device(testDevice)
                    .metricType(MetricType.CPU_USAGE).isResolved(false).build();

            when(thresholdCacheService.getThresholdsByDevice(DEVICE_ID)).thenReturn(List.of(threshold));
            // An active alert already exists - should not create a duplicate
            when(alertRepository.findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(DEVICE_ID, MetricType.CPU_USAGE))
                    .thenReturn(Optional.of(existingAlert));

            // Act
            alertProcessingService.checkThresholds(testMetric);

            // Assert - no save (no new alert, no resolve)
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should auto-resolve alert when metric returns to normal")
        void checkThresholds_MetricBackToNormal_ResolvesActiveAlert() {
            // Arrange - CPU is at 50%, threshold is > 90%, so it's NOT breached
            testMetric.setCpuUsagePercent(50.0);

            AlertThreshold threshold = AlertThreshold.builder()
                    .metricType(MetricType.CPU_USAGE)
                    .operator(ThresholdOperator.GREATER_THAN)
                    .thresholdValue(90.0)
                    .severity(AlertSeverity.CRITICAL)
                    .device(testDevice)
                    .build();

            Alert activeAlert = Alert.builder()
                    .id(ALERT_ID).device(testDevice)
                    .metricType(MetricType.CPU_USAGE)
                    .severity(AlertSeverity.CRITICAL)
                    .isResolved(false).build();

            when(thresholdCacheService.getThresholdsByDevice(DEVICE_ID)).thenReturn(List.of(threshold));
            when(alertRepository.findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(DEVICE_ID, MetricType.CPU_USAGE))
                    .thenReturn(Optional.of(activeAlert));

            // Act
            alertProcessingService.checkThresholds(testMetric);

            // Assert - alert should be auto-resolved
            assertThat(activeAlert.getIsResolved()).isTrue();
            verify(alertRepository).save(activeAlert);
        }

        @Test
        @DisplayName("Should do nothing when metric is normal and no active alert exists")
        void checkThresholds_MetricNormal_NoActiveAlert_DoesNothing() {
            // Arrange - CPU at 50%, threshold > 90%, not breached, no existing alert
            testMetric.setCpuUsagePercent(50.0);

            AlertThreshold threshold = AlertThreshold.builder()
                    .metricType(MetricType.CPU_USAGE)
                    .operator(ThresholdOperator.GREATER_THAN)
                    .thresholdValue(90.0)
                    .severity(AlertSeverity.CRITICAL)
                    .device(testDevice)
                    .build();

            when(thresholdCacheService.getThresholdsByDevice(DEVICE_ID)).thenReturn(List.of(threshold));
            when(alertRepository.findFirstByDeviceIdAndMetricTypeAndIsResolvedFalse(DEVICE_ID, MetricType.CPU_USAGE))
                    .thenReturn(Optional.empty());

            // Act
            alertProcessingService.checkThresholds(testMetric);

            // Assert - no alert created, nothing saved
            verify(alertRepository, never()).save(any());
        }
    }

    // =====================================================================
    // createAlertAndNotify()
    // =====================================================================
    @Nested
    @DisplayName("createAlertAndNotify()")
    class CreateAlertAndNotify {

        @Test
        @DisplayName("Should create alert with correct fields and enqueue notification")
        void createAlertAndNotify_ValidInput_SavesAlertAndEnqueuesJob() {
            // Arrange
            AlertThreshold threshold = AlertThreshold.builder()
                    .metricType(MetricType.CPU_USAGE)
                    .operator(ThresholdOperator.GREATER_THAN)
                    .thresholdValue(90.0)
                    .severity(AlertSeverity.CRITICAL)
                    .device(testDevice)
                    .build();
            Double currentValue = 95.0;

            // Act
            Instant beforeCreate = Instant.now();
            alertProcessingService.createAlertAndNotify(testMetric, threshold, currentValue);

            // Assert - capture the saved alert to verify its fields
            // Using @Captor here because we need to inspect the Alert object that was
            // constructed inside the method (not accessible via a test fixture reference)
            verify(alertRepository).save(argThat(alert -> {
                assertThat(alert.getDevice()).isEqualTo(testDevice);
                assertThat(alert.getMetricType()).isEqualTo(MetricType.CPU_USAGE);
                assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
                assertThat(alert.getMetricValue()).isEqualTo(95.0);
                assertThat(alert.getThresholdValue()).isEqualTo(90.0);
                assertThat(alert.getIsResolved()).isFalse();
                assertThat(alert.getCreatedAt())
                        .isCloseTo(beforeCreate, within(2, ChronoUnit.SECONDS));
                assertThat(alert.getMessage()).contains("CPU Usage");
                return true;
            }));

            verify(jobScheduler).enqueue(any(org.jobrunr.jobs.lambdas.JobLambda.class));
        }

        @Test
        @DisplayName("Should create alert with offline-specific message for DEVICE_OFFLINE")
        void createAlertAndNotify_DeviceOffline_UsesOfflineMessage() {
            // Arrange
            AlertThreshold threshold = AlertThreshold.builder()
                    .metricType(MetricType.DEVICE_OFFLINE)
                    .operator(ThresholdOperator.GREATER_THAN)
                    .thresholdValue(0.0)
                    .severity(AlertSeverity.CRITICAL)
                    .device(testDevice)
                    .build();

            // Act
            alertProcessingService.createAlertAndNotify(testMetric, threshold, 0.0);

            // Assert
            verify(alertRepository).save(argThat(alert -> {
                assertThat(alert.getMessage())
                        .isEqualTo("Device has gone offline and stopped sending metrics.");
                assertThat(alert.getMessage()).doesNotContain("0.0");
                return true;
            }));
        }
    }

    // =====================================================================
    // resolveAlert()
    // =====================================================================
    @Nested
    @DisplayName("resolveAlert() – auto-resolve")
    class ResolveAlertAuto {

        @Test
        @DisplayName("Should mark alert as resolved with current value and enqueue notification")
        void resolveAlert_ActiveAlert_MarksResolvedAndNotifies() {
            // Arrange
            Alert activeAlert = Alert.builder()
                    .id(ALERT_ID).device(testDevice)
                    .metricType(MetricType.CPU_USAGE)
                    .severity(AlertSeverity.CRITICAL)
                    .isResolved(false)
                    .message("CPU Usage exceeded threshold")
                    .build();
            Double recoveredValue = 45.0;

            // Act
            Instant beforeResolve = Instant.now();
            alertProcessingService.resolveAlert(activeAlert, recoveredValue);

            // Assert
            assertThat(activeAlert.getIsResolved()).isTrue();
            assertThat(activeAlert.getResolvedAt())
                    .isCloseTo(beforeResolve, within(2, ChronoUnit.SECONDS));
            assertThat(activeAlert.getMetricValue()).isEqualTo(45.0);
            assertThat(activeAlert.getMessage()).contains("back to normal");

            verify(alertRepository).save(activeAlert);
            verify(jobScheduler).enqueue(any(org.jobrunr.jobs.lambdas.JobLambda.class));
        }

        @Test
        @DisplayName("Should use online-specific message when resolving DEVICE_OFFLINE alert")
        void resolveAlert_DeviceOffline_UsesOnlineMessage() {
            // Arrange
            Alert activeAlert = Alert.builder()
                    .id(ALERT_ID).device(testDevice)
                    .metricType(MetricType.DEVICE_OFFLINE)
                    .severity(AlertSeverity.CRITICAL)
                    .isResolved(false)
                    .message("Device has gone offline and stopped sending metrics.")
                    .build();

            // Act
            alertProcessingService.resolveAlert(activeAlert, 0.0);

            // Assert
            assertThat(activeAlert.getMessage())
                    .isEqualTo("Device is back online and sending metrics.");
            assertThat(activeAlert.getMessage()).doesNotContain("0.0");
            verify(alertRepository).save(activeAlert);
        }
    }
}
