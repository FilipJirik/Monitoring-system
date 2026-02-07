package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.enums.PermissionLevel;
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

    // has any permissions?
    @Transactional(readOnly = true)
    public void checkDeviceAccess(UUID userId, UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found: " + deviceId));

        if (device.getOwner().getId().equals(userId)) {
            return;
        }
        boolean hasAccess = userDeviceAccessRepository.existsByUserIdAndDeviceId(userId, deviceId);
        if (hasAccess) {
            return;
        }

        throw new ForbiddenException("You don't have permission to access this device");
    }
    @Transactional(readOnly = true)
    public void checkDeviceWritePermission(UUID userId, UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found: " + deviceId));

        if (device.getOwner().getId().equals(userId)) { // Is owner - has full access
            return;
        }
        PermissionLevel permission = userDeviceAccessRepository
                .findPermissionLevel(userId, deviceId);

        if (permission != PermissionLevel.WRITE && permission != PermissionLevel.ADMIN) {
            throw new ForbiddenException("You don't have permission to edit this device");
        }
    }

    @Transactional(readOnly = true)
    public void checkDeviceOwnership(UUID userId, UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found: " + deviceId));

        if (!device.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the device owner can perform this action");
        }
    }
    @Transactional(readOnly = true)
    public PermissionLevel getPermissionLevel(UUID userId, UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found: " + deviceId));

        if (device.getOwner().getId().equals(userId)) {
            // Owner has the highest permission level (equivalent to ADMIN)
            return PermissionLevel.ADMIN;
        }
        PermissionLevel permission = userDeviceAccessRepository
                .findPermissionLevel(userId, deviceId);
        
        // Return null if no access, so callers can check for null
        return permission;
    }
    
    @Transactional(readOnly = true)
    public boolean hasReadAccess(UUID userId, UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found: " + deviceId));

        if (device.getOwner().getId().equals(userId)) {
            return true; // Owner has all permissions
        }
        return userDeviceAccessRepository.existsByUserIdAndDeviceId(userId, deviceId);
    }
    
    @Transactional(readOnly = true)
    public boolean hasWriteAccess(UUID userId, UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found: " + deviceId));

        if (device.getOwner().getId().equals(userId)) {
            return true; // Owner has all permissions
        }
        PermissionLevel permission = userDeviceAccessRepository
                .findPermissionLevel(userId, deviceId);
        
        return permission == PermissionLevel.WRITE || permission == PermissionLevel.ADMIN;
    }



}
