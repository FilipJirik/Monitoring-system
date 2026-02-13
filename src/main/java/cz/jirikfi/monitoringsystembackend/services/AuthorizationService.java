package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.PermissionLevel;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.exceptions.ForbiddenException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserDeviceAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationService {
    private final DeviceRepository deviceRepository;
    private final UserDeviceAccessRepository userDeviceAccessRepository;

    public void verifyEditPermission(UserPrincipal principal, Device device) {
        if (principal.getRole() == Role.ADMIN || device.getOwner().getId().equals(principal.getId())) {
            return;
        }

        PermissionLevel permission = userDeviceAccessRepository
                .findPermissionLevel(principal.getId(), device.getId());

        if (permission == null || permission == PermissionLevel.READ) {
            throw new ForbiddenException("You don't have permission to edit this device");
        }
    }

    @Transactional(readOnly = true)
    public void verifyReadAccess(UUID deviceId, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            if (!deviceRepository.existsById(deviceId)) {
                throw new NotFoundException("Device not found");
            }
            return;
        }

        if (!deviceRepository.hasReadAccess(deviceId, principal.getId())) {
            throw new ForbiddenException("You don't have permission to access this device");
        }
    }

    @Transactional(readOnly = true)
    public void verifyEditAccess(UUID deviceId, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            if (!deviceRepository.existsById(deviceId)) {
                throw new NotFoundException("Device not found");
            }
            return;
        }

        if (!deviceRepository.hasEditAccess(deviceId, principal.getId())) {
            throw new ForbiddenException("You don't have permission to edit this device");
        }
    }

    @Transactional(readOnly = true)
    public Device getDeviceWithReadAccess(UUID deviceId, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            return deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new NotFoundException("Device not found"));
        }

        return deviceRepository.findByIdAndUserAccess(deviceId, principal.getId())
                .orElseThrow(() -> new ForbiddenException("Device not found or access denied"));
    }

    @Transactional(readOnly = true)
    public Device getDeviceWithEditAccess(UUID deviceId, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN) {
            return deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new NotFoundException("Device not found"));
        }

        return deviceRepository.findByIdAndUserEditAccess(deviceId, principal.getId())
                .orElseThrow(() -> new ForbiddenException("You don't have permission to edit this device"));
    }


}
