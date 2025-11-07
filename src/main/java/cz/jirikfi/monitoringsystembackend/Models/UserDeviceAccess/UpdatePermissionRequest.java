package cz.jirikfi.monitoringsystembackend.Models.UserDeviceAccess;

import cz.jirikfi.monitoringsystembackend.Entities.Enums.PermissionLevel;
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
