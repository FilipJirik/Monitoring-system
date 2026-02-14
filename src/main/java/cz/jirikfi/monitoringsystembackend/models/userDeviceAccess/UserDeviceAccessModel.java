package cz.jirikfi.monitoringsystembackend.models.userDeviceAccess;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class UserDeviceAccessModel {
    private UUID userId;
    private UUID deviceId;
    private String permissionLevel;
}
