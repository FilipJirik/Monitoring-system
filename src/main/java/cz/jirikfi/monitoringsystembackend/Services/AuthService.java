package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.RefreshToken;
import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Exceptions.UnauthorizedException;
import cz.jirikfi.monitoringsystembackend.Models.Auth.AuthResponse;
import cz.jirikfi.monitoringsystembackend.Models.Auth.ChangePasswordRequest;
import cz.jirikfi.monitoringsystembackend.Models.Auth.LoginModel;
import cz.jirikfi.monitoringsystembackend.Models.Auth.RefreshTokenRequest;
import cz.jirikfi.monitoringsystembackend.Models.Auth.RegisterModel;
import cz.jirikfi.monitoringsystembackend.Models.Auth.UserInfo;
import cz.jirikfi.monitoringsystembackend.Repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.Security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterModel request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User registered: {} with email: {}", user.getUsername(), user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }
    @Transactional
    public AuthResponse login(LoginModel request) {

        try {
            // Spring Security checks if user password matches - safest
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            User user = (User) authentication.getPrincipal();
            user.setLastLogin(Instant.now());
            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getId(), user.getEmail(), String.valueOf(user.getRole()));
            String refreshToken = refreshTokenService.createRefreshToken(user);

            log.info("User logged in: {}", user.getEmail());

            return AuthResponse.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .token(token)
                    .refreshToken(refreshToken)
                    .build();

        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }
    @Transactional
    public void logout(UUID userId) {
        refreshTokenService.deleteByUserId(userId);
        log.info("User logged out: {}", userId);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    @Transactional(readOnly = true)
    public UserInfo getCurrentUserInfo(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        return UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String incomingToken = request.getRefreshToken();

        if (!jwtUtil.isTokenValid(incomingToken) || !jwtUtil.isRefreshToken(incomingToken)) {
            throw new UnauthorizedException("Invalid refresh token format");
        }

        RefreshToken currentRefreshToken = refreshTokenService.findByToken(incomingToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is not in database!"));

        refreshTokenService.verifyExpiration(currentRefreshToken);

        User user = currentRefreshToken.getUser();

        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
