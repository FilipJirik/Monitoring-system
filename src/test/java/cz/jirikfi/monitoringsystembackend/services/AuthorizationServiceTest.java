package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.PermissionLevel;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.exceptions.ForbiddenException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserDeviceAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private UserDeviceAccessRepository userDeviceAccessRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private UserPrincipal regularUser;
    private UserPrincipal adminUser;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        regularUser = UserPrincipal.builder()
                .id(USER_ID).username("user").email("user@test.com")
                .role(Role.USER).password("encoded").build();

        adminUser = UserPrincipal.builder()
                .id(USER_ID).username("admin").email("admin@test.com")
                .role(Role.ADMIN).password("encoded").build();

        User owner = User.builder().id(USER_ID).username("user").build();
        testDevice = Device.builder().id(DEVICE_ID).name("Server-1").owner(owner).build();
    }

    // =====================================================================
    // verifyEditPermission()
    // =====================================================================
    @Nested
    @DisplayName("verifyEditPermission()")
    class VerifyEditPermission {

        @Test
        @DisplayName("Should allow when user is admin")
        void verifyEditPermission_Admin_Passes() {
            // Arrange — admin user, any device

            // Act & Assert — no exception means success
            authorizationService.verifyEditPermission(adminUser, testDevice);
        }

        @Test
        @DisplayName("Should allow when user is the device owner")
        void verifyEditPermission_Owner_Passes() {
            // Arrange — regularUser.id matches device.owner.id (both USER_ID)

            // Act & Assert
            authorizationService.verifyEditPermission(regularUser, testDevice);
        }

        @Test
        @DisplayName("Should allow when user has EDIT permission")
        void verifyEditPermission_HasEditAccess_Passes() {
            // Arrange — different owner so ownership check fails
            User differentOwner = User.builder().id(UUID.randomUUID()).build();
            Device otherDevice = Device.builder().id(DEVICE_ID).owner(differentOwner).build();

            when(userDeviceAccessRepository.findPermissionLevel(USER_ID, DEVICE_ID))
                    .thenReturn(PermissionLevel.EDIT);

            // Act & Assert
            authorizationService.verifyEditPermission(regularUser, otherDevice);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when user has only READ permission")
        void verifyEditPermission_ReadOnly_ThrowsForbiddenException() {
            // Arrange
            User differentOwner = User.builder().id(UUID.randomUUID()).build();
            Device otherDevice = Device.builder().id(DEVICE_ID).owner(differentOwner).build();

            when(userDeviceAccessRepository.findPermissionLevel(USER_ID, DEVICE_ID))
                    .thenReturn(PermissionLevel.READ);

            // Act & Assert
            assertThatThrownBy(() -> authorizationService.verifyEditPermission(regularUser, otherDevice))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when user has no permission at all")
        void verifyEditPermission_NoPermission_ThrowsForbiddenException() {
            // Arrange
            User differentOwner = User.builder().id(UUID.randomUUID()).build();
            Device otherDevice = Device.builder().id(DEVICE_ID).owner(differentOwner).build();

            when(userDeviceAccessRepository.findPermissionLevel(USER_ID, DEVICE_ID))
                    .thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> authorizationService.verifyEditPermission(regularUser, otherDevice))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // =====================================================================
    // verifyReadAccess()
    // =====================================================================
    @Nested
    @DisplayName("verifyReadAccess()")
    class VerifyReadAccess {

        @Test
        @DisplayName("Should allow admin when device exists")
        void verifyReadAccess_AdminDeviceExists_Passes() {
            // Arrange
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);

            // Act & Assert
            authorizationService.verifyReadAccess(DEVICE_ID, adminUser);
        }

        @Test
        @DisplayName("Should throw NotFoundException for admin when device does not exist")
        void verifyReadAccess_AdminDeviceNotFound_ThrowsNotFoundException() {
            // Arrange
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authorizationService.verifyReadAccess(DEVICE_ID, adminUser))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Should allow regular user with read access")
        void verifyReadAccess_UserHasAccess_Passes() {
            // Arrange
            when(deviceRepository.hasReadAccess(DEVICE_ID, USER_ID)).thenReturn(true);

            // Act & Assert
            authorizationService.verifyReadAccess(DEVICE_ID, regularUser);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when regular user has no access")
        void verifyReadAccess_UserNoAccess_ThrowsForbiddenException() {
            // Arrange
            when(deviceRepository.hasReadAccess(DEVICE_ID, USER_ID)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authorizationService.verifyReadAccess(DEVICE_ID, regularUser))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // =====================================================================
    // verifyEditAccess()
    // =====================================================================
    @Nested
    @DisplayName("verifyEditAccess()")
    class VerifyEditAccess {

        @Test
        @DisplayName("Should allow admin when device exists")
        void verifyEditAccess_AdminDeviceExists_Passes() {
            // Arrange
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);

            // Act & Assert
            authorizationService.verifyEditAccess(DEVICE_ID, adminUser);
        }

        @Test
        @DisplayName("Should throw NotFoundException for admin when device does not exist")
        void verifyEditAccess_AdminDeviceNotFound_ThrowsNotFoundException() {
            // Arrange
            when(deviceRepository.existsById(DEVICE_ID)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authorizationService.verifyEditAccess(DEVICE_ID, adminUser))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Should allow regular user with edit access")
        void verifyEditAccess_UserHasEditAccess_Passes() {
            // Arrange
            when(deviceRepository.hasEditAccess(DEVICE_ID, USER_ID)).thenReturn(true);

            // Act & Assert
            authorizationService.verifyEditAccess(DEVICE_ID, regularUser);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when regular user has no edit access")
        void verifyEditAccess_UserNoEditAccess_ThrowsForbiddenException() {
            // Arrange
            when(deviceRepository.hasEditAccess(DEVICE_ID, USER_ID)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authorizationService.verifyEditAccess(DEVICE_ID, regularUser))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // =====================================================================
    // getDeviceWithReadAccess()
    // =====================================================================
    @Nested
    @DisplayName("getDeviceWithReadAccess()")
    class GetDeviceWithReadAccess {

        @Test
        @DisplayName("Should return device for admin")
        void getDeviceWithReadAccess_Admin_ReturnsDevice() {
            // Arrange
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(testDevice));

            // Act
            Device result = authorizationService.getDeviceWithReadAccess(DEVICE_ID, adminUser);

            // Assert
            assertThat(result).isEqualTo(testDevice);
        }

        @Test
        @DisplayName("Should throw NotFoundException for admin when device missing")
        void getDeviceWithReadAccess_AdminDeviceNotFound_ThrowsNotFoundException() {
            // Arrange
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authorizationService.getDeviceWithReadAccess(DEVICE_ID, adminUser))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Should return device for regular user with access")
        void getDeviceWithReadAccess_UserWithAccess_ReturnsDevice() {
            // Arrange
            when(deviceRepository.findByIdAndUserAccess(DEVICE_ID, USER_ID))
                    .thenReturn(Optional.of(testDevice));

            // Act
            Device result = authorizationService.getDeviceWithReadAccess(DEVICE_ID, regularUser);

            // Assert
            assertThat(result).isEqualTo(testDevice);
        }

        @Test
        @DisplayName("Should throw ForbiddenException for regular user without access")
        void getDeviceWithReadAccess_UserNoAccess_ThrowsForbiddenException() {
            // Arrange
            when(deviceRepository.findByIdAndUserAccess(DEVICE_ID, USER_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authorizationService.getDeviceWithReadAccess(DEVICE_ID, regularUser))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // =====================================================================
    // getDeviceWithEditAccess()
    // =====================================================================
    @Nested
    @DisplayName("getDeviceWithEditAccess()")
    class GetDeviceWithEditAccess {

        @Test
        @DisplayName("Should return device for admin")
        void getDeviceWithEditAccess_Admin_ReturnsDevice() {
            // Arrange
            when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(testDevice));

            // Act
            Device result = authorizationService.getDeviceWithEditAccess(DEVICE_ID, adminUser);

            // Assert
            assertThat(result).isEqualTo(testDevice);
        }

        @Test
        @DisplayName("Should return device for regular user with edit access")
        void getDeviceWithEditAccess_UserWithEditAccess_ReturnsDevice() {
            // Arrange
            when(deviceRepository.findByIdAndUserEditAccess(DEVICE_ID, USER_ID))
                    .thenReturn(Optional.of(testDevice));

            // Act
            Device result = authorizationService.getDeviceWithEditAccess(DEVICE_ID, regularUser);

            // Assert
            assertThat(result).isEqualTo(testDevice);
        }

        @Test
        @DisplayName("Should throw ForbiddenException for regular user without edit access")
        void getDeviceWithEditAccess_UserNoEditAccess_ThrowsForbiddenException() {
            // Arrange
            when(deviceRepository.findByIdAndUserEditAccess(DEVICE_ID, USER_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authorizationService.getDeviceWithEditAccess(DEVICE_ID, regularUser))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
