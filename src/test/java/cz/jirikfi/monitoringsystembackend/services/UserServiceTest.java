package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserDeviceAccess;
import cz.jirikfi.monitoringsystembackend.enums.PermissionLevel;
import cz.jirikfi.monitoringsystembackend.exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.exceptions.ConflictException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.DeviceMapper;
import cz.jirikfi.monitoringsystembackend.mappers.UserMapper;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.CreatePermissionRequestDto;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.UpdatePermissionRequestDto;
import cz.jirikfi.monitoringsystembackend.models.users.CreateUserRequestDto;
import cz.jirikfi.monitoringsystembackend.models.users.UpdateUserRequestDto;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserDeviceAccessRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDeviceAccessRepository userDeviceAccessRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private User testUser;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID).username("testuser").email("test@example.com")
                .password("encoded").build();

        testDevice = Device.builder()
                .id(DEVICE_ID).name("Server-1").owner(testUser).build();
    }

    // =====================================================================
    //  createUser()
    // =====================================================================
    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        @DisplayName("Should create user with encoded password when email is unique")
        void createUser_UniqueEmail_CreatesAndReturnsUser() {
            // Arrange
            CreateUserRequestDto request = CreateUserRequestDto.builder()
                    .username("newuser").email("new@example.com").password("plaintext").build();

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("plaintext")).thenReturn("encoded-hash");

            // Act
            User result = userService.createUser(request);

            // Assert
            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getPassword()).isEqualTo("encoded-hash");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when email already exists")
        void createUser_DuplicateEmail_ThrowsConflictException() {
            // Arrange
            CreateUserRequestDto request = CreateUserRequestDto.builder()
                    .email("taken@example.com").build();

            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // =====================================================================
    //  updateUser()
    // =====================================================================
    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("Should update username and email when both are provided")
        void updateUser_ValidFields_UpdatesUser() {
            // Arrange
            UpdateUserRequestDto request = new UpdateUserRequestDto();
            request.setUsername("updated");
            request.setEmail("updated@example.com");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailAndIdNot("updated@example.com", USER_ID)).thenReturn(false);

            // Act
            User result = userService.updateUser(USER_ID, request);

            // Assert
            assertThat(result.getUsername()).isEqualTo("updated");
            assertThat(result.getEmail()).isEqualTo("updated@example.com");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw ConflictException when new email is taken by another user")
        void updateUser_EmailTaken_ThrowsConflictException() {
            // Arrange
            UpdateUserRequestDto request = new UpdateUserRequestDto();
            request.setEmail("taken@example.com");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmailAndIdNot("taken@example.com", USER_ID)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateUser(USER_ID, request))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // =====================================================================
    //  deleteUser()
    // =====================================================================
    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("Should delete user when found")
        void deleteUser_UserExists_DeletesUser() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            // Act
            userService.deleteUser(USER_ID);

            // Assert
            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user does not exist")
        void deleteUser_UserNotFound_ThrowsNotFoundException() {
            // Arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // =====================================================================
    //  grantAccess()
    // =====================================================================
    @Nested
    @DisplayName("grantAccess()")
    class GrantAccess {

        @Test
        @DisplayName("Should save new access when none exists")
        void grantAccess_NoExisting_SavesAccess() {
            // Arrange
            CreatePermissionRequestDto request = new CreatePermissionRequestDto();
            request.setPermissionLevel(PermissionLevel.READ);

            when(userDeviceAccessRepository.findByUserIdAndDeviceId(USER_ID, DEVICE_ID))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(testDevice));

            // Act
            userService.grantAccess(USER_ID, DEVICE_ID, request);

            // Assert
            verify(userDeviceAccessRepository).save(any(UserDeviceAccess.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when user already has access")
        void grantAccess_AlreadyHasAccess_ThrowsConflictException() {
            // Arrange
            UserDeviceAccess existing = UserDeviceAccess.builder()
                    .user(testUser).device(testDevice).permissionLevel(PermissionLevel.READ).build();

            when(userDeviceAccessRepository.findByUserIdAndDeviceId(USER_ID, DEVICE_ID))
                    .thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> userService.grantAccess(USER_ID, DEVICE_ID, new CreatePermissionRequestDto()))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // =====================================================================
    //  changePermission()
    // =====================================================================
    @Nested
    @DisplayName("changePermission()")
    class ChangePermission {

        @Test
        @DisplayName("Should update permission level when access exists")
        void changePermission_AccessExists_UpdatesPermission() {
            // Arrange
            UserDeviceAccess access = UserDeviceAccess.builder()
                    .user(testUser).device(testDevice).permissionLevel(PermissionLevel.READ).build();

            UpdatePermissionRequestDto request = new UpdatePermissionRequestDto();
            request.setPermissionLevel(PermissionLevel.EDIT);

            when(userDeviceAccessRepository.findByUserIdAndDeviceId(USER_ID, DEVICE_ID))
                    .thenReturn(Optional.of(access));

            // Act
            userService.changePermission(USER_ID, DEVICE_ID, request);

            // Assert
            assertThat(access.getPermissionLevel()).isEqualTo(PermissionLevel.EDIT);
            verify(userDeviceAccessRepository).save(access);
        }

        @Test
        @DisplayName("Should throw BadRequestException when access does not exist")
        void changePermission_NoAccess_ThrowsBadRequestException() {
            // Arrange
            when(userDeviceAccessRepository.findByUserIdAndDeviceId(USER_ID, DEVICE_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.changePermission(USER_ID, DEVICE_ID,
                    new UpdatePermissionRequestDto()))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // =====================================================================
    //  revokeAccess()
    // =====================================================================
    @Nested
    @DisplayName("revokeAccess()")
    class RevokeAccess {

        @Test
        @DisplayName("Should delete access when it exists")
        void revokeAccess_AccessExists_DeletesAccess() {
            // Arrange
            UserDeviceAccess access = UserDeviceAccess.builder()
                    .user(testUser).device(testDevice).build();

            when(userDeviceAccessRepository.findByUserIdAndDeviceId(USER_ID, DEVICE_ID))
                    .thenReturn(Optional.of(access));

            // Act
            userService.revokeAccess(USER_ID, DEVICE_ID);

            // Assert
            verify(userDeviceAccessRepository).delete(access);
        }

        @Test
        @DisplayName("Should throw BadRequestException when no access to revoke")
        void revokeAccess_NoAccess_ThrowsBadRequestException() {
            // Arrange
            when(userDeviceAccessRepository.findByUserIdAndDeviceId(USER_ID, DEVICE_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.revokeAccess(USER_ID, DEVICE_ID))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
