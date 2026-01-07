package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.Enums.PermissionLevel;
import cz.jirikfi.monitoringsystembackend.Exceptions.ForbiddenException;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.UserDeviceAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

        if (device.getOwner().getId().equals(userId)) { // Is owner
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
            return PermissionLevel.WRITE;
        }
        return userDeviceAccessRepository
                .findPermissionLevel(userId, deviceId);
    }



}
