package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.RefreshToken;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.exceptions.ConflictException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.exceptions.UnauthorizedException;
import cz.jirikfi.monitoringsystembackend.mappers.AuthMapper;
import cz.jirikfi.monitoringsystembackend.models.auth.*;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

        @Mock
        private UserRepository userRepository;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private AuthenticationManager authenticationManager;
        @Mock
        private JwtUtil jwtUtil;
        @Mock
        private RefreshTokenService refreshTokenService;
        @Mock
        private AuthMapper authMapper;

        @InjectMocks
        private AuthService authService;

        // ── shared test fixtures ──────────────────────────────────────────

        private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        private static final String USERNAME = "testuser";
        private static final String EMAIL = "test@example.com";
        private static final String RAW_PASSWORD = "securePassword123";
        private static final String ENCODED_PASSWORD = "$2a$10$encodedHash";
        private static final String ACCESS_TOKEN = "jwt.access.token";
        private static final String REFRESH_TOKEN = "jwt.refresh.token";

        private User testUser;
        private AuthResponseDto expectedAuthResponse;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .id(USER_ID)
                                .username(USERNAME)
                                .email(EMAIL)
                                .password(ENCODED_PASSWORD)
                                .role(Role.USER)
                                .build();

                expectedAuthResponse = AuthResponseDto.builder()
                                .userId(USER_ID)
                                .username(USERNAME)
                                .email(EMAIL)
                                .token(ACCESS_TOKEN)
                                .refreshToken(REFRESH_TOKEN)
                                .build();
        }

        // =====================================================================
        // register()
        // =====================================================================
        @Nested
        @DisplayName("register()")
        class Register {

                @Test
                @DisplayName("Should register user when email is unique")
                void register_UniqueEmail_ReturnsAuthResponse() {
                        // Arrange
                        RegisterRequestDto request = RegisterRequestDto.builder()
                                        .username(USERNAME).password(RAW_PASSWORD).email(EMAIL).build();

                        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
                        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
                        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
                        when(jwtUtil.generateToken(any(UUID.class), eq(USERNAME), eq(EMAIL), eq(Role.USER.name())))
                                        .thenReturn(ACCESS_TOKEN);
                        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(REFRESH_TOKEN);
                        when(authMapper.toAuthResponse(any(User.class), eq(ACCESS_TOKEN), eq(REFRESH_TOKEN)))
                                        .thenReturn(expectedAuthResponse);

                        // Act
                        AuthResponseDto result = authService.register(request);

                        // Assert
                        assertThat(result).isNotNull();
                        assertThat(result.getToken()).isEqualTo(ACCESS_TOKEN);
                        assertThat(result.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
                        assertThat(result.getUsername()).isEqualTo(USERNAME);
                        assertThat(result.getEmail()).isEqualTo(EMAIL);

                        verify(passwordEncoder).encode(RAW_PASSWORD);
                        verify(userRepository).save(any(User.class));
                }

                @Test
                @DisplayName("Should throw ConflictException when email already exists")
                void register_DuplicateEmail_ThrowsConflictException() {
                        // Arrange
                        RegisterRequestDto request = RegisterRequestDto.builder()
                                        .username(USERNAME).password(RAW_PASSWORD).email(EMAIL).build();

                        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

                        // Act & Assert
                        assertThatThrownBy(() -> authService.register(request))
                                        .isInstanceOf(ConflictException.class)
                                        .hasMessage("Email already exists");

                        verify(userRepository, never()).save(any());
                }
        }

        // =====================================================================
        // login()
        // =====================================================================
        @Nested
        @DisplayName("login()")
        class Login {

                @Test
                @DisplayName("Should login user and set lastLogin when credentials are valid")
                void login_ValidCredentials_ReturnsAuthResponseAndUpdatesLastLogin() {
                        // Arrange
                        LoginRequestDto request = LoginRequestDto.builder()
                                        .email(EMAIL).password(RAW_PASSWORD).build();

                        UserPrincipal principal = UserPrincipal.builder()
                                        .id(USER_ID).username(USERNAME).email(EMAIL)
                                        .role(Role.USER).password(ENCODED_PASSWORD).build();

                        Authentication authentication = mock(Authentication.class);
                        when(authentication.getPrincipal()).thenReturn(principal);
                        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                        .thenReturn(authentication);
                        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
                        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
                        when(jwtUtil.generateToken(USER_ID, USERNAME, EMAIL, String.valueOf(Role.USER)))
                                        .thenReturn(ACCESS_TOKEN);
                        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(REFRESH_TOKEN);
                        when(authMapper.toAuthResponse(testUser, ACCESS_TOKEN, REFRESH_TOKEN))
                                        .thenReturn(expectedAuthResponse);

                        // Act
                        Instant beforeLogin = Instant.now();
                        AuthResponseDto result = authService.login(request);

                        // Assert
                        assertThat(result).isNotNull();
                        assertThat(result.getToken()).isEqualTo(ACCESS_TOKEN);

                        // Verify lastLogin was updated close to "now"
                        assertThat(testUser.getLastLogin())
                                        .isCloseTo(beforeLogin, within(2, ChronoUnit.SECONDS));

                        verify(userRepository).save(testUser);
                }

                @Test
                @DisplayName("Should throw UnauthorizedException when credentials are invalid")
                void login_BadCredentials_ThrowsUnauthorizedException() {
                        // Arrange
                        LoginRequestDto request = LoginRequestDto.builder()
                                        .email(EMAIL).password("wrongPassword").build();

                        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                        .thenThrow(new BadCredentialsException("Bad credentials"));

                        // Act & Assert
                        assertThatThrownBy(() -> authService.login(request))
                                        .isInstanceOf(UnauthorizedException.class)
                                        .hasMessage("Invalid email or password");

                        verifyNoInteractions(jwtUtil, refreshTokenService, authMapper);
                }

                @Test
                @DisplayName("Should throw UnauthorizedException when user not found after authentication")
                void login_UserDeletedAfterAuth_ThrowsUnauthorizedException() {
                        // Arrange
                        LoginRequestDto request = LoginRequestDto.builder()
                                        .email(EMAIL).password(RAW_PASSWORD).build();

                        UserPrincipal principal = UserPrincipal.builder()
                                        .id(USER_ID).username(USERNAME).email(EMAIL)
                                        .role(Role.USER).password(ENCODED_PASSWORD).build();

                        Authentication authentication = mock(Authentication.class);
                        when(authentication.getPrincipal()).thenReturn(principal);
                        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                        .thenReturn(authentication);
                        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

                        // Act & Assert
                        assertThatThrownBy(() -> authService.login(request))
                                        .isInstanceOf(UnauthorizedException.class)
                                        .hasMessage("User account not found");

                        verify(userRepository, never()).save(any());
                }
        }

        // =====================================================================
        // logout()
        // =====================================================================
        @Nested
        @DisplayName("logout()")
        class Logout {

                @Test
                @DisplayName("Should delegate refresh token deletion to RefreshTokenService")
                void logout_ValidRequest_DeletesRefreshToken() {
                        // Arrange
                        LogoutRequestDto request = new LogoutRequestDto();
                        request.setRefreshToken(REFRESH_TOKEN);

                        // Act
                        authService.logout(request);

                        // Assert - verify the service delegates correctly
                        verify(refreshTokenService).deleteByToken(REFRESH_TOKEN);
                }
        }

        // =====================================================================
        // changePassword()
        // =====================================================================
        @Nested
        @DisplayName("changePassword()")
        class ChangePassword {

                private UserPrincipal principal;

                @BeforeEach
                void setUpPrincipal() {
                        principal = UserPrincipal.builder()
                                        .id(USER_ID).username(USERNAME).email(EMAIL)
                                        .role(Role.USER).password(ENCODED_PASSWORD).build();
                }

                @Test
                @DisplayName("Should change password when current password is correct")
                void changePassword_CorrectCurrentPassword_UpdatesPassword() {
                        // Arrange
                        String newPassword = "newSecurePassword123";
                        String newEncodedPassword = "$2a$10$newEncodedHash";

                        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                                        .currentPassword(RAW_PASSWORD).newPassword(newPassword).build();

                        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
                        when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);

                        // Act
                        authService.changePassword(principal, request);

                        // Assert
                        assertThat(testUser.getPassword()).isEqualTo(newEncodedPassword);
                        verify(userRepository).save(testUser);
                }

                @Test
                @DisplayName("Should throw NotFoundException when user does not exist")
                void changePassword_UserNotFound_ThrowsNotFoundException() {
                        // Arrange
                        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                                        .currentPassword(RAW_PASSWORD).newPassword("newPassword123").build();

                        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

                        // Act & Assert
                        assertThatThrownBy(() -> authService.changePassword(principal, request))
                                        .isInstanceOf(NotFoundException.class)
                                        .hasMessage("User not found");

                        verify(userRepository, never()).save(any());
                }

                @Test
                @DisplayName("Should throw UnauthorizedException when current password is wrong")
                void changePassword_WrongCurrentPassword_ThrowsUnauthorizedException() {
                        // Arrange
                        ChangePasswordRequestDto request = ChangePasswordRequestDto.builder()
                                        .currentPassword("wrongPassword").newPassword("newPassword123").build();

                        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
                        when(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).thenReturn(false);

                        // Act & Assert
                        assertThatThrownBy(() -> authService.changePassword(principal, request))
                                        .isInstanceOf(UnauthorizedException.class)
                                        .hasMessage("Current password is incorrect");

                        verify(userRepository, never()).save(any());
                }
        }

        // =====================================================================
        // getCurrentUserInfo()
        // =====================================================================
        @Nested
        @DisplayName("getCurrentUserInfo()")
        class GetCurrentUserInfo {

                @Test
                @DisplayName("Should return user info when principal is valid")
                void getCurrentUserInfo_ValidPrincipal_ReturnsUserInfoDto() {
                        // Arrange
                        UserPrincipal principal = UserPrincipal.builder()
                                        .id(USER_ID).username(USERNAME).email(EMAIL)
                                        .role(Role.USER).password(ENCODED_PASSWORD).build();

                        // Act
                        UserInfoDto result = authService.getCurrentUserInfo(principal);

                        // Assert
                        assertThat(result.getUserId()).isEqualTo(USER_ID);
                        assertThat(result.getUsername()).isEqualTo(USERNAME);
                        assertThat(result.getEmail()).isEqualTo(EMAIL);
                        assertThat(result.getRole()).isEqualTo(Role.USER);
                }
        }

        // =====================================================================
        // refreshToken()
        // =====================================================================
        @Nested
        @DisplayName("refreshToken()")
        class RefreshTokenTests {

                private static final String NEW_ACCESS_TOKEN = "jwt.new.access.token";
                private static final String NEW_REFRESH_TOKEN = "jwt.new.refresh.token";

                @Test
                @DisplayName("Should refresh tokens when incoming token is valid")
                void refreshToken_ValidToken_ReturnsNewTokens() {
                        // Arrange
                        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                                        .refreshToken(REFRESH_TOKEN).build();

                        RefreshToken storedToken = RefreshToken.builder()
                                        .token(REFRESH_TOKEN).user(testUser)
                                        .expiryDate(Instant.now().plusSeconds(3600)).build();

                        AuthResponseDto newAuthResponse = AuthResponseDto.builder()
                                        .userId(USER_ID).username(USERNAME).email(EMAIL)
                                        .token(NEW_ACCESS_TOKEN).refreshToken(NEW_REFRESH_TOKEN).build();

                        when(jwtUtil.isTokenValid(REFRESH_TOKEN)).thenReturn(true);
                        when(jwtUtil.isRefreshToken(REFRESH_TOKEN)).thenReturn(true);
                        when(refreshTokenService.findByToken(REFRESH_TOKEN)).thenReturn(Optional.of(storedToken));
                        when(refreshTokenService.verifyExpiration(storedToken)).thenReturn(storedToken);
                        when(jwtUtil.generateToken(USER_ID, USERNAME, EMAIL, Role.USER.name()))
                                        .thenReturn(NEW_ACCESS_TOKEN);
                        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(NEW_REFRESH_TOKEN);
                        when(authMapper.toAuthResponse(testUser, NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN))
                                        .thenReturn(newAuthResponse);

                        // Act
                        AuthResponseDto result = authService.refreshToken(request);

                        // Assert
                        assertThat(result.getToken()).isEqualTo(NEW_ACCESS_TOKEN);
                        assertThat(result.getRefreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
                }

                @Test
                @DisplayName("Should throw UnauthorizedException when token format is invalid")
                void refreshToken_InvalidFormat_ThrowsUnauthorizedException() {
                        // Arrange
                        String invalidToken = "not.a.valid.jwt";
                        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                                        .refreshToken(invalidToken).build();

                        when(jwtUtil.isTokenValid(invalidToken)).thenReturn(false);

                        // Act & Assert
                        assertThatThrownBy(() -> authService.refreshToken(request))
                                        .isInstanceOf(UnauthorizedException.class)
                                        .hasMessage("Invalid refresh token format");

                        verifyNoInteractions(refreshTokenService);
                }

                @Test
                @DisplayName("Should throw UnauthorizedException when token is not a refresh type")
                void refreshToken_NotRefreshType_ThrowsUnauthorizedException() {
                        // Arrange
                        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                                        .refreshToken(ACCESS_TOKEN).build();

                        when(jwtUtil.isTokenValid(ACCESS_TOKEN)).thenReturn(true);
                        when(jwtUtil.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);

                        // Act & Assert
                        assertThatThrownBy(() -> authService.refreshToken(request))
                                        .isInstanceOf(UnauthorizedException.class)
                                        .hasMessage("Invalid refresh token format");

                        verifyNoInteractions(refreshTokenService);
                }

                @Test
                @DisplayName("Should throw UnauthorizedException when token is not in database")
                void refreshToken_TokenNotInDatabase_ThrowsUnauthorizedException() {
                        // Arrange
                        String unknownToken = "jwt.unknown.refresh";
                        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                                        .refreshToken(unknownToken).build();

                        when(jwtUtil.isTokenValid(unknownToken)).thenReturn(true);
                        when(jwtUtil.isRefreshToken(unknownToken)).thenReturn(true);
                        when(refreshTokenService.findByToken(unknownToken)).thenReturn(Optional.empty());

                        // Act & Assert
                        assertThatThrownBy(() -> authService.refreshToken(request))
                                        .isInstanceOf(UnauthorizedException.class)
                                        .hasMessage("Refresh token is not in database!");
                }
        }
}
