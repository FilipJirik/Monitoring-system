package cz.jirikfi.monitoringsystembackend.mappers;

import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.models.auth.AuthResponseDto;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public AuthResponseDto toAuthResponse(User user, String token, String refreshToken) {
        return AuthResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }
}
