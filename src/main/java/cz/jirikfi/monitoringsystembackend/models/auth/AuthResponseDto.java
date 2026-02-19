package cz.jirikfi.monitoringsystembackend.models.auth;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AuthResponseDto {
    UUID userId;
    String username;
    String email;
    String token;
    String refreshToken;
}
