package cz.jirikfi.monitoringsystembackend.models.userDeviceAccess;

import cz.jirikfi.monitoringsystembackend.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdatePermissionRequest {
    @NotNull
    private PermissionLevel permissionLevel;
}
