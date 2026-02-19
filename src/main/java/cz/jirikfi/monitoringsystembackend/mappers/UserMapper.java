package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.UserAccessResponseDto;
import cz.jirikfi.monitoringsystembackend.models.users.UserResponseDto;
import cz.jirikfi.monitoringsystembackend.repositories.projections.UserAccess;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDto toResponse(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    public UserAccessResponseDto toUserAccessResponse(UserAccess proj) {
        if (proj == null) {
            return null;
        }
        String permission = Boolean.TRUE.equals(proj.getIsOwner())
                ? "OWNER"
                : (proj.getPermissionLevel() != null ? proj.getPermissionLevel().name() : "READ");

        return UserAccessResponseDto.builder()
                .userId(proj.getUserId())
                .username(proj.getUsername())
                .email(proj.getEmail())
                .permissionLevel(permission)
                .build();
    }
}
