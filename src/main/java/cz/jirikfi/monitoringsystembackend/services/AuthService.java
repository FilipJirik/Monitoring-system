package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.RefreshToken;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.exceptions.ConflictException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.exceptions.UnauthorizedException;
import cz.jirikfi.monitoringsystembackend.mappers.AuthMapper;
import cz.jirikfi.monitoringsystembackend.models.auth.AuthResponse;
import cz.jirikfi.monitoringsystembackend.models.auth.ChangePasswordRequest;
import cz.jirikfi.monitoringsystembackend.models.auth.LoginModel;
import cz.jirikfi.monitoringsystembackend.models.auth.RefreshTokenRequest;
import cz.jirikfi.monitoringsystembackend.models.auth.RegisterModel;
import cz.jirikfi.monitoringsystembackend.models.auth.UserInfo;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.utils.JwtUtil;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;

    @Transactional
    public AuthResponse register(RegisterModel request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User registered: {} with email: {}", user.getUsername(), user.getEmail());

        return authMapper.toAuthResponse(user, token, refreshToken);
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
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new UnauthorizedException("User account not found"));

            user.setLastLogin(Instant.now());
            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail(), String.valueOf(user.getRole()));
            String refreshToken = refreshTokenService.createRefreshToken(user);

            log.info("User logged in: {}", user.getEmail());

            return authMapper.toAuthResponse(user, token, refreshToken);

        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }
    @Transactional
    public void logout(UserPrincipal principal) {
        refreshTokenService.deleteByUserId(principal.getId());
        log.info("User logged out: {}", principal.getId());
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", principal.getUsername());
    }

    @Transactional(readOnly = true)
    public UserInfo getCurrentUserInfo(UserPrincipal principal) {
        return new UserInfo(principal.getId(), principal.getUsername(), principal.getEmail(), principal.getRole());
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

        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        String newRefreshToken = refreshTokenService.createRefreshToken(user);

        return authMapper.toAuthResponse(user, newAccessToken, newRefreshToken);
    }
}
