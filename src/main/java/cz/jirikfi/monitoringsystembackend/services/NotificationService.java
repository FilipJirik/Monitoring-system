package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.mappers.AlertMapper;
import cz.jirikfi.monitoringsystembackend.models.alerts.AlertDetailDto;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertMapper alertMapper;

    static final String TOPIC_ALERTS = "/topic/alerts";
    static final String TOPIC_ALERTS_RESOLVED = "/topic/alerts/resolved";

    private static final String COLOR_RESOLVED = "#4CAF50";
    private static final String COLOR_TRIGGERED = "#d9534f";
    private static final String SUBJECT_PREFIX_ALERT = "ALERT: ";
    private static final String SUBJECT_PREFIX_RESOLVED = "RESOLVED: ";
    private static final String HEADER_RESOLVED = "System Alert Resolved";
    private static final String HEADER_TRIGGERED = "System Alert Triggered";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmailAlert(String userEmail, Alert alert) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);

            String subject;
            String header;
            String headerColor;
            String statusText;
            String statusColor;

            if (alert.getIsResolved()) {
                subject = SUBJECT_PREFIX_RESOLVED + String.format("%s on device %s - %s",
                        alert.getMetricType().getLabel(), alert.getDevice().getName(), alert.getSeverity());
                header = HEADER_RESOLVED;
                headerColor = COLOR_RESOLVED;
                statusText = STATUS_RESOLVED;
                statusColor = COLOR_RESOLVED;
            } else {
                subject = SUBJECT_PREFIX_ALERT + String.format("%s on device %s - %s",
                        alert.getMetricType().getLabel(), alert.getDevice().getName(), alert.getSeverity());
                header = HEADER_TRIGGERED;
                headerColor = COLOR_TRIGGERED;
                statusText = STATUS_ACTIVE;
                statusColor = COLOR_TRIGGERED;
            }
            helper.setSubject(subject);

            String htmlContent = String.format(
                    """
                            <html>
                            <body style="font-family: Arial, sans-serif; color: #333; line-height: 1.6;">
                                <h2 style="color: %s;">%s</h2>
                                <p>An alert has been %s on device <strong>%s</strong>.</p>

                                <table style="border-collapse: collapse; width: 100%%; max-width: 600px; margin-top: 15px;">
                                    <tr>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Status:</strong></td>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd; color: %s;"><strong>%s</strong></td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Metric:</strong></td>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Severity:</strong></td>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Current Value:</strong></td>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;">%s</td>
                                    </tr>
                                    %s <!-- Threshold Row -->
                                    <tr>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Time:</strong></td>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;">%s</td>
                                    </tr>
                                    %s <!-- Resolved Time Row -->
                                </table>

                                <p style="margin-top: 20px;"><strong>Details:</strong><br>%s</p>

                                <p style="font-size: 12px; color: #777; margin-top: 30px;">
                                    This is an automated message from your Monitoring System. Please do not reply.
                                </p>
                            </body>
                            </html>
                            """,
                    headerColor,
                    header,
                    alert.getIsResolved() ? "resolved" : "triggered",
                    alert.getDevice().getName(),
                    statusColor,
                    statusText,
                    alert.getMetricType().getLabel(),
                    alert.getSeverity(),
                    alert.getMetricValue(),
                    alert.getThresholdValue() != null ? String.format(
                            "<tr><td style=\"padding: 8px; border-bottom: 1px solid #ddd;\"><strong>Threshold:</strong></td><td style=\"padding: 8px; border-bottom: 1px solid #ddd;\">%s</td></tr>",
                            alert.getThresholdValue()) : "",
                    DATE_TIME_FORMATTER.format(alert.getCreatedAt()),
                    alert.getResolvedAt() != null ? String.format(
                            "<tr><td style=\"padding: 8px; border-bottom: 1px solid #ddd;\"><strong>Resolved At:</strong></td><td style=\"padding: 8px; border-bottom: 1px solid #ddd;\">%s</td></tr>",
                            DATE_TIME_FORMATTER.format(alert.getResolvedAt())) : "",
                    alert.getMessage());

            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("HTML Email alert successfully sent to {}", userEmail);

        } catch (Exception e) {
            log.error("Failed to send HTML email alert to {}", userEmail, e);
        }
    }

    public void sendFrontendAlert(Alert alert) {
        try {
            String destination = alert.getIsResolved() ? TOPIC_ALERTS_RESOLVED : TOPIC_ALERTS;
            AlertDetailDto payload = alertMapper.toDetailModel(alert);

            messagingTemplate.convertAndSend(destination, payload);
            log.info("WebSocket alert broadcasted to {}", destination);

        } catch (Exception e) {
            log.error("Failed to send WebSocket alert for device {}", alert.getDevice().getId(), e);
        }
    }
}
