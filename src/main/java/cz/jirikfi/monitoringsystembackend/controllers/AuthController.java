package cz.jirikfi.monitoringsystembackend.controllers;

import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.models.auth.AuthResponseDto;
import cz.jirikfi.monitoringsystembackend.models.auth.ChangePasswordRequestDto;
import cz.jirikfi.monitoringsystembackend.models.auth.LoginRequestDto;
import cz.jirikfi.monitoringsystembackend.models.auth.RefreshTokenRequestDto;
import cz.jirikfi.monitoringsystembackend.models.auth.RegisterRequestDto;
import cz.jirikfi.monitoringsystembackend.models.auth.UserInfoDto;
import cz.jirikfi.monitoringsystembackend.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        AuthResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserInfoDto userInfo = authService.getCurrentUserInfo(principal);
        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        authService.changePassword(principal, request);
        return ResponseEntity.noContent().build();
    }
}
