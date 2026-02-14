package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.exceptions.ConflictException;
import cz.jirikfi.monitoringsystembackend.exceptions.ForbiddenException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.AlertMapper;
import cz.jirikfi.monitoringsystembackend.models.alerts.AlertResponseModel;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRecipientRepository;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.services.background.NotificationJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertRecipientRepository alertRecipientRepository;
    private final UserRepository userRepository;
    private final AlertMapper alertMapper;
    private final JobScheduler jobScheduler;
    private final NotificationJobService notificationJobService;

    @Transactional(readOnly = true)
    public Page<AlertResponseModel> getAlerts(UserPrincipal principal, Boolean isResolved, AlertSeverity severity, Pageable pageable) {
        boolean isAdmin = principal.getRole() == Role.ADMIN;

        Page<Alert> alertPage = alertRepository.findAllFiltered(
                principal.getId(),
                isAdmin,
                isResolved,
                severity,
                pageable
        );
        return alertPage.map(alertMapper::toResponseModel);
    }

    @Transactional
    public AlertResponseModel resolveAlert(UUID alertId, UserPrincipal principal) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert not found with ID: " + alertId));

        boolean isAdmin = principal.getRole() == Role.ADMIN;
        boolean isRecipient = alertRecipientRepository.existsByUserIdAndDeviceId(principal.getId(), alert.getDevice().getId());

        // Permission check
        if (!isAdmin && !isRecipient) {
            log.warn("Unauthorized resolve attempt for alert {} by user {}", alertId, principal.getId());
            throw new ForbiddenException("You don't have permission to resolve this alert");
        }

        if (alert.getIsResolved()) {
            throw new ConflictException("Alert is already resolved");
        }

        alert.setIsResolved(true);
        alert.setResolvedAt(Instant.now());

        User resolver = userRepository.getReferenceById(principal.getId());
        alert.setResolvedBy(resolver);

        Alert savedAlert = alertRepository.save(alert);

        jobScheduler.enqueue(() -> notificationJobService.processAlertNotifications(alertId));

        return alertMapper.toResponseModel(savedAlert);
    }
}