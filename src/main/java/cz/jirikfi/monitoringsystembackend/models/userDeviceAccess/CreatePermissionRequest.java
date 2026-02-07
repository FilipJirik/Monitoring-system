package cz.jirikfi.monitoringsystembackend.models.userDeviceAccess;

import cz.jirikfi.monitoringsystembackend.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreatePermissionRequest {
    @NotNull
    private PermissionLevel permissionLevel = PermissionLevel.READ;
}
