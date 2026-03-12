package cz.jirikfi.monitoringsystembackend.models.userDeviceAccess;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class DeviceAccessResponseDto{
    private UUID deviceId;
    private String deviceName;
    private String permissionLevel; // "OWNER", "READ", "EDIT" easier for frontend
}
