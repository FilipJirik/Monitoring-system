package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.Enums.PermissionLevel;
import cz.jirikfi.monitoringsystembackend.Exceptions.ForbiddenException;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.UserDeviceAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthorizationService {
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private UserDeviceAccessRepository userDeviceAccessRepository;

    // has any permissions?
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

    public void checkDeviceOwnership(UUID userId, UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found: " + deviceId));

        if (!device.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the device owner can perform this action");
        }
    }

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
