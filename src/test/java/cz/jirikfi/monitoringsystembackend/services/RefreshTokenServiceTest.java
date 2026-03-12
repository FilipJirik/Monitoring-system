package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.RefreshToken;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.exceptions.UnauthorizedException;
import cz.jirikfi.monitoringsystembackend.repositories.RefreshTokenRepository;
import cz.jirikfi.monitoringsystembackend.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private User testUser;

    @BeforeEach
    void setUp() {
        // @Value("${jwt.refresh-expiration}") is not injected in unit tests
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 604800000L); // 7 days

        testUser = User.builder().id(USER_ID).username("testuser").email("test@example.com").build();
    }

    // =====================================================================
    // findByToken()
    // =====================================================================
    @Nested
    @DisplayName("findByToken()")
    class FindByToken {

        @Test
        @DisplayName("Should return token when it exists in the database")
        void findByToken_TokenExists_ReturnsOptionalWithToken() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                    .token("some-refresh-token").user(testUser).build();

            when(refreshTokenRepository.findByToken("some-refresh-token"))
                    .thenReturn(Optional.of(token));

            // Act
            Optional<RefreshToken> result = refreshTokenService.findByToken("some-refresh-token");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getToken()).isEqualTo("some-refresh-token");
        }

        @Test
        @DisplayName("Should return empty when token does not exist")
        void findByToken_TokenNotFound_ReturnsEmptyOptional() {
            // Arrange
            when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

            // Act
            Optional<RefreshToken> result = refreshTokenService.findByToken("nonexistent");

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // =====================================================================
    // createRefreshToken()
    // =====================================================================
    @Nested
    @DisplayName("createRefreshToken()")
    class CreateRefreshToken {

        @Test
        @DisplayName("Should generate token, save to DB, and return the token string")
        void createRefreshToken_ValidUser_SavesAndReturnsTokenString() {
            // Arrange
            String generatedToken = "jwt-refresh-token-string";
            when(jwtUtil.generateRefreshToken(USER_ID)).thenReturn(generatedToken);

            // Act
            Instant beforeCreate = Instant.now();
            String result = refreshTokenService.createRefreshToken(testUser);

            // Assert
            assertThat(result).isEqualTo(generatedToken);

            // Verify the token was saved with correct fields
            verify(refreshTokenRepository).save(argThat(savedToken -> {
                assertThat(savedToken.getUser()).isEqualTo(testUser);
                assertThat(savedToken.getToken()).isEqualTo(generatedToken);
                // Expiry should be ~7 days from now
                assertThat(savedToken.getExpiryDate())
                        .isCloseTo(beforeCreate.plusSeconds(604800), within(2, ChronoUnit.SECONDS));
                return true;
            }));
        }
    }

    // =====================================================================
    // verifyExpiration()
    // =====================================================================
    @Nested
    @DisplayName("verifyExpiration()")
    class VerifyExpiration {

        @Test
        @DisplayName("Should return the token when it is not expired")
        void verifyExpiration_TokenValid_ReturnsToken() {
            // Arrange — token expires in the future
            RefreshToken validToken = RefreshToken.builder()
                    .token("valid-token").user(testUser)
                    .expiryDate(Instant.now().plusSeconds(3600)) // expires in 1 hour
                    .build();

            // Act
            RefreshToken result = refreshTokenService.verifyExpiration(validToken);

            // Assert
            assertThat(result).isEqualTo(validToken);
            verify(refreshTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should delete token and throw UnauthorizedException when expired")
        void verifyExpiration_TokenExpired_DeletesAndThrows() {
            // Arrange — token already expired
            RefreshToken expiredToken = RefreshToken.builder()
                    .token("expired-token").user(testUser)
                    .expiryDate(Instant.now().minusSeconds(3600)) // expired 1 hour ago
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.verifyExpiration(expiredToken))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("expired");

            verify(refreshTokenRepository).delete(expiredToken);
        }
    }

    // =====================================================================
    // deleteByToken()
    // =====================================================================
    @Nested
    @DisplayName("deleteByToken()")
    class DeleteByToken {

        @Test
        @DisplayName("Should delegate deletion to the repository")
        void deleteByToken_ValidToken_DelegatesToRepository() {
            // Arrange & Act
            refreshTokenService.deleteByToken("token-to-delete");

            // Assert
            verify(refreshTokenRepository).deleteByToken("token-to-delete");
        }
    }
}
