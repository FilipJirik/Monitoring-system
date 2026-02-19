package cz.jirikfi.monitoringsystembackend.models.auth;

import cz.jirikfi.monitoringsystembackend.enums.Role;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserInfoDto {
    UUID userId;
    String username;
    String email;
    Role role;
}
