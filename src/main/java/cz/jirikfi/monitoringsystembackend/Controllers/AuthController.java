package cz.jirikfi.monitoringsystembackend.Controllers;

import cz.jirikfi.monitoringsystembackend.Models.Auth.AuthResponse;
import cz.jirikfi.monitoringsystembackend.Models.Auth.LoginModel;
import cz.jirikfi.monitoringsystembackend.Models.Auth.RegisterModel;
import cz.jirikfi.monitoringsystembackend.Services.AuthService;
import cz.jirikfi.monitoringsystembackend.Services.DeviceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterModel request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response); // Status code 204 ?
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginModel request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
