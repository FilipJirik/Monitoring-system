package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.RefreshToken;
import cz.jirikfi.monitoringsystembackend.Entities.User;
import cz.jirikfi.monitoringsystembackend.Exceptions.UnauthorizedException;
import cz.jirikfi.monitoringsystembackend.Repositories.RefreshTokenRepository;
import cz.jirikfi.monitoringsystembackend.Security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public String createRefreshToken(User user) {
        long expirationSeconds = refreshTokenDurationMs / 1000;
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
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
