package cz.jirikfi.monitoringsystembackend.services.background;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.entities.AlertRecipient;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRecipientRepository;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRepository;
import cz.jirikfi.monitoringsystembackend.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationJobService {

    private final AlertRepository alertRepository;
    private final AlertRecipientRepository alertRecipientRepository;
    private final NotificationService notificationService;

    @Job(name = "Send notifications for Alert %0")
    @Transactional(readOnly = true)
    public void processAlertNotifications(UUID alertId) {
        Alert alert = alertRepository.findById(alertId).orElse(null);
        if (alert == null)
            return;

        List<AlertRecipient> recipients = alertRecipientRepository.findByDeviceIdWithUser(alert.getDevice().getId());

        if (recipients.isEmpty())
            return;

        boolean sendToFrontend = recipients.stream().anyMatch(AlertRecipient::getNotifyFrontend);

        if (sendToFrontend) {
            notificationService.sendFrontendAlert(alert);
        }

        for (AlertRecipient recipient : recipients) {
            if (recipient.getNotifyEmail()) {
                notificationService.sendEmailAlert(recipient.getUser().getEmail(), alert);
            }
        }
    }
}
