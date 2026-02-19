package cz.jirikfi.monitoringsystembackend.repositories.projections;

import cz.jirikfi.monitoringsystembackend.enums.PermissionLevel;

import java.util.UUID;

public interface DeviceAccess {
    UUID getDeviceId();
    String getDeviceName();
    Boolean getIsOwner();
    PermissionLevel getPermissionLevel();
}
