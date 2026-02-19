package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.AlertRecipient;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.AlertRecipientMapper;
import cz.jirikfi.monitoringsystembackend.models.recipients.CreateRecipientRequestDto;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientResponseDto;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientStatusDto;
import cz.jirikfi.monitoringsystembackend.models.recipients.UpdateRecipientRequestDto;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRecipientRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertRecipientService {

    private final AlertRecipientRepository alertRecipientRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final AlertRecipientMapper alertRecipientMapper;

    private final org.slf4j.Logger _logger = org.slf4j.LoggerFactory.getLogger(AlertRecipientService.class);

    @Transactional(readOnly = true)
    public Page<RecipientStatusDto> getRecipientsWithStatus(UserPrincipal principal, UUID deviceId, boolean includePotential, Pageable pageable) {
        if (includePotential) {
            authorizationService.verifyEditAccess(deviceId, principal);

            return userRepository.findPotentialRecipients(deviceId, pageable)
                    .map(alertRecipientMapper::mapProjectionToModel);
        } else {
            authorizationService.verifyReadAccess(deviceId, principal);

            return alertRecipientRepository.findActiveRecipientsProjected(deviceId, pageable)
                    .map(alertRecipientMapper::mapProjectionToModel);
        }
    }

    @Transactional
    public RecipientResponseDto addRecipient(UserPrincipal principal, UUID deviceId, CreateRecipientRequestDto model) {
        UUID targetUserId = (model.getUserId() != null) ? model.getUserId() : principal.getId();

        _logger.info("Adding recipient for device {} to user {}", deviceId, targetUserId);

        // Permission check based on target user
        boolean isSelf = targetUserId.equals(principal.getId());

        _logger.info("Target user is self: {}", isSelf);

        if (isSelf) {
            authorizationService.verifyReadAccess(deviceId, principal);
        } else {
            authorizationService.verifyEditAccess(deviceId, principal);
        }

        if (alertRecipientRepository.existsByDeviceIdAndUserId(deviceId, targetUserId)) {
            throw new BadRequestException("User is already a recipient for this device");
        }

        // Check if target user exists (if not self)
        if (!isSelf && !userRepository.existsById(targetUserId)) {
            throw new NotFoundException("User not found: " + targetUserId);
        }

        // We need the Device entity to set the relationship, but only for creating the recipient
        Device device = authorizationService.getDeviceWithReadAccess(deviceId, principal);

        AlertRecipient recipient = AlertRecipient.builder()
                .device(device)
                .user(userRepository.getReferenceById(targetUserId))
                .notifyEmail(model.getNotifyEmail())
                .notifyFrontend(model.getNotifyFrontend())
                .build();

        return alertRecipientMapper.toResponse(alertRecipientRepository.save(recipient));
    }

    @Transactional
    public RecipientResponseDto updateRecipient(UserPrincipal principal, UUID deviceId, UUID recipientUserId, UpdateRecipientRequestDto model) {
        // Lightweight check
        boolean isSelf = recipientUserId.equals(principal.getId());
        if (isSelf) {
            authorizationService.verifyReadAccess(deviceId, principal);
        } else {
            authorizationService.verifyEditAccess(deviceId, principal);
        }

        AlertRecipient recipient = alertRecipientRepository.findByDeviceIdAndUserId(deviceId, recipientUserId)
                .orElseThrow(() -> new NotFoundException("Recipient not found"));

        recipient.setNotifyEmail(model.getNotifyEmail());
        recipient.setNotifyFrontend(model.getNotifyFrontend());

        return alertRecipientMapper.toResponse(alertRecipientRepository.save(recipient));
    }

    @Transactional
    public void deleteRecipient(UserPrincipal principal, UUID deviceId, UUID recipientUserId) {
        // Lightweight check
        boolean isSelf = recipientUserId.equals(principal.getId());
        if (isSelf) {
            authorizationService.verifyReadAccess(deviceId, principal);
        } else {
            authorizationService.verifyEditAccess(deviceId, principal);
        }

        AlertRecipient recipient = alertRecipientRepository.findByDeviceIdAndUserId(deviceId, recipientUserId)
                .orElseThrow(() -> new NotFoundException("Recipient not found"));

        alertRecipientRepository.delete(recipient);
    }
}
