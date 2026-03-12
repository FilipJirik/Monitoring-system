package cz.jirikfi.monitoringsystembackend.models.userDeviceAccess;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class UserAccessResponseDto {
    private UUID userId;
    private String username;
    private String email;
    private String permissionLevel; // "OWNER", "READ", "EDIT" easier for frontend
}