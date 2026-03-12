package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.entities.AlertRecipient;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRecipientRepository;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRepository;
import cz.jirikfi.monitoringsystembackend.services.background.NotificationJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationJobServiceTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertRecipientRepository alertRecipientRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationJobService notificationJobService;

    private static final UUID ALERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private Device testDevice;
    private Alert testAlert;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(UUID.randomUUID()).username("owner").build();
        testDevice = Device.builder().id(DEVICE_ID).name("Server-1").owner(owner).build();

        testAlert = Alert.builder()
                .id(ALERT_ID)
                .device(testDevice)
                .metricType(MetricType.CPU_USAGE)
                .severity(AlertSeverity.CRITICAL)
                .isResolved(false)
                .build();
    }

    // =====================================================================
    //  processAlertNotifications()
    // =====================================================================
    @Nested
    @DisplayName("processAlertNotifications()")
    class ProcessAlertNotifications {

        @Test
        @DisplayName("Should do nothing when alert is not found in DB")
        void processAlertNotifications_AlertNotFound_DoesNothing() {
            // Arrange
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

            // Act
            notificationJobService.processAlertNotifications(ALERT_ID);

            // Assert
            verifyNoInteractions(alertRecipientRepository, notificationService);
        }

        @Test
        @DisplayName("Should do nothing when there are no recipients")
        void processAlertNotifications_NoRecipients_DoesNothing() {
            // Arrange
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(testAlert));
            when(alertRecipientRepository.findByDeviceIdWithUser(DEVICE_ID))
                    .thenReturn(Collections.emptyList());

            // Act
            notificationJobService.processAlertNotifications(ALERT_ID);

            // Assert
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Should send frontend alert when at least one recipient has notifyFrontend=true")
        void processAlertNotifications_FrontendEnabled_SendsFrontendAlert() {
            // Arrange
            User recipient = User.builder().id(UUID.randomUUID())
                    .email("user@example.com").build();

            AlertRecipient frontendRecipient = AlertRecipient.builder()
                    .user(recipient).notifyFrontend(true).notifyEmail(false).build();

            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(testAlert));
            when(alertRecipientRepository.findByDeviceIdWithUser(DEVICE_ID))
                    .thenReturn(Collections.singletonList(frontendRecipient));

            // Act
            notificationJobService.processAlertNotifications(ALERT_ID);

            // Assert
            verify(notificationService).sendFrontendAlert(testAlert);
            verify(notificationService, never()).sendEmailAlert(anyString(), any());
        }

        @Test
        @DisplayName("Should send email to recipients with notifyEmail=true")
        void processAlertNotifications_EmailEnabled_SendsEmailAlert() {
            // Arrange
            User recipient = User.builder().id(UUID.randomUUID())
                    .email("user@example.com").build();

            AlertRecipient emailRecipient = AlertRecipient.builder()
                    .user(recipient).notifyFrontend(false).notifyEmail(true).build();

            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(testAlert));
            when(alertRecipientRepository.findByDeviceIdWithUser(DEVICE_ID))
                    .thenReturn(Collections.singletonList(emailRecipient));

            // Act
            notificationJobService.processAlertNotifications(ALERT_ID);

            // Assert
            verify(notificationService).sendEmailAlert("user@example.com", testAlert);
            verify(notificationService, never()).sendFrontendAlert(any());
        }

        @Test
        @DisplayName("Should send both frontend and email when both are enabled")
        void processAlertNotifications_BothEnabled_SendsBoth() {
            // Arrange
            User recipient = User.builder().id(UUID.randomUUID())
                    .email("user@example.com").build();

            AlertRecipient bothRecipient = AlertRecipient.builder()
                    .user(recipient).notifyFrontend(true).notifyEmail(true).build();

            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(testAlert));
            when(alertRecipientRepository.findByDeviceIdWithUser(DEVICE_ID))
                    .thenReturn(Collections.singletonList(bothRecipient));

            // Act
            notificationJobService.processAlertNotifications(ALERT_ID);

            // Assert
            verify(notificationService).sendFrontendAlert(testAlert);
            verify(notificationService).sendEmailAlert("user@example.com", testAlert);
        }
    }
}
