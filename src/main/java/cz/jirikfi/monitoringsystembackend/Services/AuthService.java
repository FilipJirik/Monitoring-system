package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.RefreshToken;
import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Exceptions.UnauthorizedException;
import cz.jirikfi.monitoringsystembackend.Models.Auth.AuthResponse;
import cz.jirikfi.monitoringsystembackend.Models.Auth.LoginModel;
import cz.jirikfi.monitoringsystembackend.Models.Auth.RefreshTokenRequest;
import cz.jirikfi.monitoringsystembackend.Models.Auth.RegisterModel;
import cz.jirikfi.monitoringsystembackend.Models.Auth.UserInfo;
import cz.jirikfi.monitoringsystembackend.Repositories.RefreshTokenRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.Security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

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
        String refreshToken = createRefreshToken(user);

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
        User user = userRepository.findByEmail(request.getEmail());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        user.setLastLogin(Instant.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = createRefreshToken(user);

        log.info("User logged in: {} with email: {}", user.getUsername(), user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .refreshToken(refreshToken)
                .build();
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
    public String createRefreshToken(User user) {
        long expirationSeconds = jwtUtil.getRefreshTokenExpiration() / 1000;
        Instant expiryDate = Instant.now().plusSeconds(expirationSeconds);
        String newTokenString = jwtUtil.generateRefreshToken(user.getId());

        RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId())
                .orElse(RefreshToken.builder()
                        .user(user)
                        .build());

        refreshToken.setToken(newTokenString);
        refreshToken.setExpiryDate(expiryDate);
        refreshToken.setUser(user);

        refreshTokenRepository.save(refreshToken);
        return newTokenString;
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String incomingToken = request.getRefreshToken();

        if (!jwtUtil.isTokenValid(incomingToken) || !jwtUtil.isRefreshToken(incomingToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        RefreshToken currentRefreshToken = refreshTokenRepository.findByToken(incomingToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (currentRefreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(currentRefreshToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = currentRefreshToken.getUser();

        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
