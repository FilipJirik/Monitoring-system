package cz.jirikfi.monitoringsystembackend.Models.Auth;

import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AuthResponse {
    UUID userId;
    String username;
    String email;
    String token;
}
