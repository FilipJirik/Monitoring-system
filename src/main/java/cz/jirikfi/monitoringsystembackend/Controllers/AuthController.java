package cz.jirikfi.monitoringsystembackend.Controllers;

import cz.jirikfi.monitoringsystembackend.Models.Auth.AuthResponse;
import cz.jirikfi.monitoringsystembackend.Models.Auth.LoginModel;
import cz.jirikfi.monitoringsystembackend.Models.Auth.RefreshTokenRequest;
import cz.jirikfi.monitoringsystembackend.Models.Auth.RegisterModel;
import cz.jirikfi.monitoringsystembackend.Models.Auth.UserInfo;
import cz.jirikfi.monitoringsystembackend.Services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterModel request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginModel request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser(@AuthenticationPrincipal UUID userId) {
        UserInfo userInfo = authService.getCurrentUserInfo(userId);
        return ResponseEntity.ok(userInfo);
    }
}
