package cz.jirikfi.monitoringsystembackend.Models.UserDeviceAccess;

import cz.jirikfi.monitoringsystembackend.Entities.Enums.PermissionLevel;
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
