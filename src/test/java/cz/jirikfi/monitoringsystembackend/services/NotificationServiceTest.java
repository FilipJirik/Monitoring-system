package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.mappers.AlertMapper;
import cz.jirikfi.monitoringsystembackend.models.alerts.AlertDetailDto;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private AlertMapper alertMapper;

    @InjectMocks
    private NotificationService notificationService;

    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ALERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private Device testDevice;
    private Alert activeAlert;
    private Alert resolvedAlert;

    @BeforeEach
    void setUp() {
        // @Value fields are not injected in unit tests — set manually
        ReflectionTestUtils.setField(notificationService, "fromEmail", "noreply@monitoring.test");

        User owner = User.builder().id(UUID.randomUUID()).username("owner").build();
        testDevice = Device.builder().id(DEVICE_ID).name("Server-1").owner(owner).build();

        activeAlert = Alert.builder()
                .id(ALERT_ID)
                .device(testDevice)
                .metricType(MetricType.CPU_USAGE)
                .severity(AlertSeverity.CRITICAL)
                .message("CPU Usage exceeded threshold")
                .metricValue(95.0)
                .thresholdValue(90.0)
                .isResolved(false)
                .createdAt(Instant.now())
                .build();

        resolvedAlert = Alert.builder()
                .id(ALERT_ID)
                .device(testDevice)
                .metricType(MetricType.CPU_USAGE)
                .severity(AlertSeverity.CRITICAL)
                .message("CPU Usage back to normal")
                .metricValue(45.0)
                .thresholdValue(90.0)
                .isResolved(true)
                .createdAt(Instant.now().minusSeconds(300))
                .resolvedAt(Instant.now())
                .build();
    }

    // =====================================================================
    // sendFrontendAlert()
    // =====================================================================
    @Nested
    @DisplayName("sendFrontendAlert()")
    class SendFrontendAlert {

        @Test
        @DisplayName("Should broadcast active alert to /topic/alerts")
        void sendFrontendAlert_ActiveAlert_BroadcastsToAlertsTopic() {
            // Arrange
            AlertDetailDto payload = AlertDetailDto.builder()
                    .id(ALERT_ID).deviceId(DEVICE_ID).deviceName("Server-1")
                    .metricType(MetricType.CPU_USAGE).severity(AlertSeverity.CRITICAL)
                    .isResolved(false).build();

            when(alertMapper.toDetailModel(activeAlert)).thenReturn(payload);

            // Act
            notificationService.sendFrontendAlert(activeAlert);

            // Assert - uses the constant TOPIC_ALERTS
            verify(messagingTemplate).convertAndSend(
                    eq(NotificationService.TOPIC_ALERTS), eq(payload));
        }

        @Test
        @DisplayName("Should broadcast resolved alert to /topic/alerts/resolved")
        void sendFrontendAlert_ResolvedAlert_BroadcastsToResolvedTopic() {
            // Arrange
            AlertDetailDto payload = AlertDetailDto.builder()
                    .id(ALERT_ID).deviceId(DEVICE_ID).deviceName("Server-1")
                    .metricType(MetricType.CPU_USAGE).severity(AlertSeverity.CRITICAL)
                    .isResolved(true).build();

            when(alertMapper.toDetailModel(resolvedAlert)).thenReturn(payload);

            // Act
            notificationService.sendFrontendAlert(resolvedAlert);

            // Assert - uses the constant TOPIC_ALERTS_RESOLVED
            verify(messagingTemplate).convertAndSend(
                    eq(NotificationService.TOPIC_ALERTS_RESOLVED), eq(payload));
        }

        @Test
        @DisplayName("Should not throw when messagingTemplate throws (exception swallowed)")
        void sendFrontendAlert_TemplateFails_DoesNotThrow() {
            // Arrange
            AlertDetailDto payload = AlertDetailDto.builder().id(ALERT_ID).build();
            when(alertMapper.toDetailModel(activeAlert)).thenReturn(payload);
            doThrow(new RuntimeException("WebSocket failure"))
                    .when(messagingTemplate).convertAndSend(anyString(), any(AlertDetailDto.class));

            // Act & Assert - no exception propagated
            notificationService.sendFrontendAlert(activeAlert);
        }
    }

    // =====================================================================
    // sendEmailAlert()
    // =====================================================================
    @Nested
    @DisplayName("sendEmailAlert()")
    class SendEmailAlert {

        @Test
        @DisplayName("Should send email via JavaMailSender for an active alert")
        void sendEmailAlert_ActiveAlert_SendsEmail() {
            // Arrange - use a real MimeMessage so MimeMessageHelper can set headers
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            notificationService.sendEmailAlert("user@example.com", activeAlert);

            // Assert
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send email for a resolved alert")
        void sendEmailAlert_ResolvedAlert_SendsEmail() {
            // Arrange - use a real MimeMessage so MimeMessageHelper can set headers
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act
            notificationService.sendEmailAlert("user@example.com", resolvedAlert);

            // Assert
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should not throw when mailSender throws (exception swallowed)")
        void sendEmailAlert_MailSenderFails_DoesNotThrow() {
            // Arrange
            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP failure"));

            // Act & Assert - no exception propagated
            notificationService.sendEmailAlert("user@example.com", activeAlert);
        }
    }
}
