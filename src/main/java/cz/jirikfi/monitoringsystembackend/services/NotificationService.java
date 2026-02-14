package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.mappers.AlertMapper;
import cz.jirikfi.monitoringsystembackend.models.alerts.AlertDetailModel;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertMapper alertMapper;

    private static final String WEBSOCKET_ALERT_PATH = "/topic/devices/%s/alerts";

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmailAlert(String userEmail, Alert alert) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject(String.format("ALERT: %s on device %s",
                    alert.getSeverity(), alert.getDevice().getName()));

            String htmlContent = String.format("""
                            <html>
                            <body style="font-family: Arial, sans-serif; color: #333; line-height: 1.6;">
                                <h2 style="color: #d9534f;">System Alert Triggered</h2>
                                <p>An anomaly has been detected on device <strong>%s</strong>.</p>
                            
                                <table style="border-collapse: collapse; width: 100%%; max-width: 600px; margin-top: 15px;">
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
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd; color: #d9534f;"><strong>%s</strong></td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Threshold:</strong></td>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Time:</strong></td>
                                        <td style="padding: 8px; border-bottom: 1px solid #ddd;">%s</td>
                                    </tr>
                                </table>
                            
                                <p style="margin-top: 20px;"><strong>Details:</strong><br>%s</p>
                            
                                <p style="font-size: 12px; color: #777; margin-top: 30px;">
                                    This is an automated message from your Monitoring System. Please do not reply.
                                </p>
                            </body>
                            </html>
                            """,
                    alert.getDevice().getName(),
                    alert.getMetricType(),
                    alert.getSeverity(),
                    alert.getMetricValue(),
                    alert.getThresholdValue(),
                    alert.getCreatedAt().toString(),
                    alert.getMessage()
            );

            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("HTML Email alert successfully sent to {}", userEmail);

        } catch (Exception e) {
            log.error("Failed to send HTML email alert to {}: {}", userEmail, e.getMessage());
        }
    }

    public void sendFrontendAlert(Alert alert) {
        try {
            String destination = String.format(WEBSOCKET_ALERT_PATH, alert.getDevice().getId());

            AlertDetailModel payload = alertMapper.toDetailModel(alert);

            messagingTemplate.convertAndSend(destination, payload);
            log.info("WebSocket alert broadcasted to {}", destination);

        } catch (Exception e) {
            log.error("Failed to send WebSocket alert for device {}: {}", alert.getDevice().getId(), e.getMessage());
        }
    }
}
