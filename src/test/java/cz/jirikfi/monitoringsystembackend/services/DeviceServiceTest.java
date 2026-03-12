package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.exceptions.ConflictException;
import cz.jirikfi.monitoringsystembackend.exceptions.ForbiddenException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.DeviceMapper;
import cz.jirikfi.monitoringsystembackend.mappers.UserMapper;
import cz.jirikfi.monitoringsystembackend.models.devices.CreateDeviceRequestDto;
import cz.jirikfi.monitoringsystembackend.models.devices.DeviceResponseDto;
import cz.jirikfi.monitoringsystembackend.models.devices.DeviceWithApiKeyDto;
import cz.jirikfi.monitoringsystembackend.models.devices.UpdateDeviceRequestDto;
import cz.jirikfi.monitoringsystembackend.models.userDeviceAccess.UserAccessResponseDto;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.repositories.projections.UserAccess;
import cz.jirikfi.monitoringsystembackend.utils.ServerUrlUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

        @Mock
        private DeviceRepository deviceRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private ImageService imageService;
        @Mock
        private DeviceMapper deviceMapper;
        @Mock
        private ServerUrlUtil serverUrlUtil;
        @Mock
        private DeviceAuthService deviceAuthService;
        @Mock
        private AuthorizationService authorizationService;
        @Mock
        private UserMapper userMapper;

        @InjectMocks
        private DeviceService deviceService;

        private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
        private static final String DEVICE_NAME = "Production Server";
        private static final String RAW_API_KEY = "rawApiKey123";
        private static final String HASHED_API_KEY = "hashedApiKey456";
        private static final String SETUP_COMMAND = "./monitoring-agent setup --server-url=http://localhost --device-id=id --api-key=key";
        private static final String DEFAULT_IMAGE = "default.png";

        private UserPrincipal userPrincipal;
        private User owner;
        private Device testDevice;
        private DeviceResponseDto expectedDeviceResponse;

        @BeforeEach
        void setUp() {
                userPrincipal = UserPrincipal.builder()
                                .id(USER_ID).username("testuser").email("test@example.com")
                                .role(Role.USER).password("encoded").build();

                owner = User.builder()
                                .id(USER_ID).username("testuser").email("test@example.com")
                                .role(Role.USER).build();

                testDevice = Device.builder()
                                .id(DEVICE_ID).name(DEVICE_NAME)
                                .operatingSystem("Linux").ipAddress("192.168.1.1")
                                .owner(owner).imageFilename(DEFAULT_IMAGE)
                                .apiKey(HASHED_API_KEY).build();

                expectedDeviceResponse = DeviceResponseDto.builder()
                                .id(DEVICE_ID).name(DEVICE_NAME)
                                .operatingSystem("Linux").ipAddress("192.168.1.1")
                                .ownerId(USER_ID).ownerUsername("testuser")
                                .imageFilename(DEFAULT_IMAGE).build();

                ReflectionTestUtils.setField(deviceService, "defaultPictureFilename", DEFAULT_IMAGE);
        }

        // =====================================================================
        // createDevice()
        // =====================================================================
        @Nested
        @DisplayName("createDevice()")
        class CreateDevice {

                @Test
                @DisplayName("Should create device when name is unique")
                void createDevice_UniqueName_ReturnsDeviceWithApiKey() {
                        // Arrange
                        CreateDeviceRequestDto request = CreateDeviceRequestDto.builder()
                                        .name(DEVICE_NAME).operatingSystem("Linux")
                                        .ipAddress("192.168.1.1").build();

                        Device mappedDevice = Device.builder().id(DEVICE_ID).name(DEVICE_NAME).build();

                        when(deviceRepository.existsByName(DEVICE_NAME)).thenReturn(false);
                        when(deviceMapper.createToEntity(request)).thenReturn(mappedDevice);
                        when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
                        when(deviceAuthService.generateRawApiKey()).thenReturn(RAW_API_KEY);
                        when(deviceAuthService.hashApiKey(RAW_API_KEY, DEVICE_ID)).thenReturn(HASHED_API_KEY);
                        when(serverUrlUtil.getSetupCommand(DEVICE_ID.toString(), RAW_API_KEY))
                                        .thenReturn(SETUP_COMMAND);

                        // Act
                        DeviceWithApiKeyDto result = deviceService.createDevice(userPrincipal, request);

                        // Assert
                        assertThat(result).isNotNull();
                        assertThat(result.getId()).isEqualTo(DEVICE_ID);
                        assertThat(result.getApiKey()).isEqualTo(RAW_API_KEY);
                        assertThat(result.getSetupCommand()).isEqualTo(SETUP_COMMAND);

                        // Verify the hashed key was set on the device, not the raw key
                        assertThat(mappedDevice.getApiKey()).isEqualTo(HASHED_API_KEY);
                        verify(deviceRepository).save(mappedDevice);
                }

                @Test
                @DisplayName("Should throw ConflictException when device name already exists")
                void createDevice_DuplicateName_ThrowsConflictException() {
                        // Arrange
                        CreateDeviceRequestDto request = CreateDeviceRequestDto.builder()
                                        .name(DEVICE_NAME).build();

                        when(deviceRepository.existsByName(DEVICE_NAME)).thenReturn(true);

                        // Act & Assert
                        assertThatThrownBy(() -> deviceService.createDevice(userPrincipal, request))
                                        .isInstanceOf(ConflictException.class)
                                        .hasMessageContaining(DEVICE_NAME);

                        verify(deviceRepository, never()).save(any());
                }
        }

        // =====================================================================
        // getDevice()
        // =====================================================================
        @Nested
        @DisplayName("getDevice()")
        class GetDevice {

        @Test
        @DisplayName("Should return device when user has read access")
        void getDevice_HasReadAccess_ReturnsDeviceResponseDto() {
            // Arrange
            when(authorizationService.getDeviceWithReadAccess(DEVICE_ID, userPrincipal))
                    .thenReturn(testDevice);
            when(deviceMapper.toResponse(testDevice)).thenReturn(expectedDeviceResponse);

            // Act
            DeviceResponseDto result = deviceService.getDevice(userPrincipal, DEVICE_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(DEVICE_ID);
            assertThat(result.getName()).isEqualTo(DEVICE_NAME);
        }

        @Test
        @DisplayName("Should propagate ForbiddenException when user has no access")
        void getDevice_NoAccess_ThrowsForbiddenException() {
            // Arrange - AuthorizationService throws when access is denied
            when(authorizationService.getDeviceWithReadAccess(DEVICE_ID, userPrincipal))
                    .thenThrow(new ForbiddenException("Device not found or access denied"));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.getDevice(userPrincipal, DEVICE_ID))
                    .isInstanceOf(ForbiddenException.class);
        }
        }

        // =====================================================================
        // updateDevice()
        // =====================================================================
        @Nested
        @DisplayName("updateDevice()")
        class UpdateDevice {

                @Test
                @DisplayName("Should update device when user has edit access")
                void updateDevice_HasEditAccess_ReturnsUpdatedDevice() {
                        // Arrange
                        UpdateDeviceRequestDto request = new UpdateDeviceRequestDto();
                        request.setName("Updated Server");

                        when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                                        .thenReturn(testDevice);
                        when(deviceMapper.toResponse(testDevice)).thenReturn(expectedDeviceResponse);

                        // Act
                        DeviceResponseDto result = deviceService.updateDevice(userPrincipal, DEVICE_ID, request);

                        // Assert
                        assertThat(result).isNotNull();
                        verify(deviceMapper).updateEntity(testDevice, request);
                        verify(deviceRepository).save(testDevice);
                }

                @Test
                @DisplayName("Should propagate ForbiddenException when user has no edit access")
                void updateDevice_NoEditAccess_ThrowsForbiddenException() {
                        // Arrange
                        UpdateDeviceRequestDto request = new UpdateDeviceRequestDto();

                        when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                                        .thenThrow(new ForbiddenException(
                                                        "You don't have permission to edit this device"));

                        // Act & Assert
                        assertThatThrownBy(() -> deviceService.updateDevice(userPrincipal, DEVICE_ID, request))
                                        .isInstanceOf(ForbiddenException.class);

                        verify(deviceRepository, never()).save(any());
                }
        }

        // =====================================================================
        // deleteDevice()
        // =====================================================================
        @Nested
        @DisplayName("deleteDevice()")
        class DeleteDevice {

                @Test
                @DisplayName("Should delete device and its custom image when user has edit access")
                void deleteDevice_HasEditAccessWithCustomImage_DeletesDeviceAndImage() {
                        // Arrange
                        String customImage = "custom-image.png";
                        testDevice.setImageFilename(customImage);
                        when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                                        .thenReturn(testDevice);

                        // Act
                        deviceService.deleteDevice(userPrincipal, DEVICE_ID);

                        // Assert
                        verify(deviceRepository).delete(testDevice);
                        verify(imageService).deleteImage(customImage);
                }

                @Test
                @DisplayName("Should delete device but NOT the default image when user has edit access")
                void deleteDevice_HasEditAccessWithDefaultImage_DeletesOnlyDevice() {
                        // Arrange
                        testDevice.setImageFilename(DEFAULT_IMAGE);
                        when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                                        .thenReturn(testDevice);

                        // Act
                        deviceService.deleteDevice(userPrincipal, DEVICE_ID);

                        // Assert
                        verify(deviceRepository).delete(testDevice);
                        verify(imageService, never()).deleteImage(DEFAULT_IMAGE);
                }

        @Test
        @DisplayName("Should propagate ForbiddenException when user has no edit access")
        void deleteDevice_NoEditAccess_ThrowsForbiddenException() {
            // Arrange
            when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                    .thenThrow(new ForbiddenException("You don't have permission to edit this device"));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.deleteDevice(userPrincipal, DEVICE_ID))
                    .isInstanceOf(ForbiddenException.class);

            verify(deviceRepository, never()).delete(any());
        }
        }

        // =====================================================================
        // changeDevicePicture()
        // =====================================================================
        @Nested
        @DisplayName("changeDevicePicture()")
        class ChangeDevicePicture {

                @Test
                @DisplayName("Should save new image and update device")
                void changeDevicePicture_ValidFile_UpdatesImageFilename() {
                        // Arrange
                        MultipartFile mockFile = mock(MultipartFile.class);
                        String newFilename = "new-image-uuid.png";

                        when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                                        .thenReturn(testDevice);
                        when(imageService.saveImage(mockFile)).thenReturn(newFilename);

                        // Act
                        Instant beforeCall = Instant.now();
                        deviceService.changeDevicePicture(userPrincipal, DEVICE_ID, mockFile);

                        // Assert
                        assertThat(testDevice.getImageFilename()).isEqualTo(newFilename);
                        assertThat(testDevice.getUpdatedAt())
                                        .isCloseTo(beforeCall, within(2, ChronoUnit.SECONDS));
                        verify(deviceRepository).save(testDevice);
                }

                @Test
                @DisplayName("Should propagate ForbiddenException when user has no edit access")
                void changeDevicePicture_NoEditAccess_ThrowsForbiddenException() {
                        // Arrange
                        MultipartFile mockFile = mock(MultipartFile.class);
                        when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                                        .thenThrow(new ForbiddenException(
                                                        "You don't have permission to edit this device"));

                        // Act & Assert
                        assertThatThrownBy(() -> deviceService.changeDevicePicture(userPrincipal, DEVICE_ID, mockFile))
                                        .isInstanceOf(ForbiddenException.class);

                        verifyNoInteractions(imageService);
                }
        }

        // =====================================================================
        // resetDevicePicture()
        // =====================================================================
        @Nested
        @DisplayName("resetDevicePicture()")
        class ResetDevicePicture {

                @Test
                @DisplayName("Should reset to default and delete old image when image is custom")
                void resetDevicePicture_CustomImage_ResetsToDefaultAndDeletesOld() {
                        // Arrange
                        String customImage = "custom-image-uuid.png";
                        testDevice.setImageFilename(customImage);

                        when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                                        .thenReturn(testDevice);
                        when(imageService.getDefaultFilename()).thenReturn(DEFAULT_IMAGE);

                        // Act
                        Instant beforeCall = Instant.now();
                        deviceService.resetDevicePicture(userPrincipal, DEVICE_ID);

                        // Assert
                        assertThat(testDevice.getImageFilename()).isEqualTo(DEFAULT_IMAGE);
                        assertThat(testDevice.getUpdatedAt())
                                        .isCloseTo(beforeCall, within(2, ChronoUnit.SECONDS));
                        verify(deviceRepository).save(testDevice);
                        verify(imageService).deleteImage(customImage);
                }

                @Test
                @DisplayName("Should do nothing when image is already the default")
                void resetDevicePicture_AlreadyDefault_DoesNothing() {
                        // Arrange
                        testDevice.setImageFilename(DEFAULT_IMAGE);

                        when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                                        .thenReturn(testDevice);
                        when(imageService.getDefaultFilename()).thenReturn(DEFAULT_IMAGE);

                        // Act
                        deviceService.resetDevicePicture(userPrincipal, DEVICE_ID);

                        // Assert - no save, no delete, early return
                        verify(deviceRepository, never()).save(any());
                        verify(imageService, never()).deleteImage(any());
                }
        }

        // =====================================================================
        // regenerateApiKey()
        // =====================================================================
        @Nested
        @DisplayName("regenerateApiKey()")
        class RegenerateApiKey {

                @Test
                @DisplayName("Should generate a new API key and return it raw")
                void regenerateApiKey_HasEditAccess_ReturnsNewRawApiKey() {
                        // Arrange
                        String newRawKey = "newRawApiKey789";
                        String newHashedKey = "newHashedKey000";

                        when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                                        .thenReturn(testDevice);
                        when(deviceAuthService.generateRawApiKey()).thenReturn(newRawKey);
                        when(deviceAuthService.hashApiKey(newRawKey, DEVICE_ID)).thenReturn(newHashedKey);
                        when(serverUrlUtil.getSetupCommand(DEVICE_ID.toString(), newRawKey)).thenReturn(SETUP_COMMAND);

                        // Act
                        Instant beforeCall = Instant.now();
                        DeviceWithApiKeyDto result = deviceService.regenerateApiKey(userPrincipal, DEVICE_ID);

                        // Assert
                        assertThat(result.getId()).isEqualTo(DEVICE_ID);
                        assertThat(result.getApiKey()).isEqualTo(newRawKey);
                        assertThat(result.getSetupCommand()).isEqualTo(SETUP_COMMAND);

                        // Verify the hashed key (not raw) is persisted
                        assertThat(testDevice.getApiKey()).isEqualTo(newHashedKey);
                        assertThat(testDevice.getUpdatedAt())
                                        .isCloseTo(beforeCall, within(2, ChronoUnit.SECONDS));
                        verify(deviceRepository).save(testDevice);
                }

        @Test
        @DisplayName("Should propagate ForbiddenException when user has no edit access")
        void regenerateApiKey_NoEditAccess_ThrowsForbiddenException() {
            // Arrange
            when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal))
                    .thenThrow(new ForbiddenException("You don't have permission to edit this device"));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.regenerateApiKey(userPrincipal, DEVICE_ID))
                    .isInstanceOf(ForbiddenException.class);

            verify(deviceRepository, never()).save(any());
        }
        }

        // =====================================================================
        // getAllAccessibleDevices()
        // =====================================================================
        @Nested
        @DisplayName("getAllAccessibleDevices()")
        class GetAllAccessibleDevices {

                @Test
                @DisplayName("Should return page of devices for regular user with keyword")
                void getAllAccessibleDevices_UserWithKeyword_ReturnsMappedPage() {
                        // Arrange
                        Pageable pageable = PageRequest.of(0, 10);
                        String keyword = " server ";
                        Page<Device> devicePage = new PageImpl<>(List.of(testDevice));

                        when(deviceRepository.searchDevices(USER_ID, false, "server", pageable))
                                        .thenReturn(devicePage);
                        when(deviceMapper.toResponse(testDevice)).thenReturn(expectedDeviceResponse);

                        // Act
                        Page<DeviceResponseDto> result = deviceService.getAllAccessibleDevices(
                                        userPrincipal, keyword, pageable);

                        // Assert
                        assertThat(result.getContent()).hasSize(1);
                        assertThat(result.getContent().getFirst().getName()).isEqualTo(DEVICE_NAME);
                }

                @Test
                @DisplayName("Should pass isAdmin=true when user is admin")
                void getAllAccessibleDevices_AdminUser_PassesIsAdminTrue() {
                        // Arrange
                        UserPrincipal adminPrincipal = UserPrincipal.builder()
                                        .id(USER_ID).username("admin").email("admin@example.com")
                                        .role(Role.ADMIN).password("encoded").build();

                        Pageable pageable = PageRequest.of(0, 10);
                        Page<Device> emptyPage = Page.empty();

                        when(deviceRepository.searchDevices(USER_ID, true, null, pageable))
                                        .thenReturn(emptyPage);

                        // Act
                        Page<DeviceResponseDto> result = deviceService.getAllAccessibleDevices(
                                        adminPrincipal, null, pageable);

                        // Assert
                        assertThat(result.getContent()).isEmpty();

                        // verify isAdmin=true was passed to the repository
                        verify(deviceRepository).searchDevices(USER_ID, true, null, pageable);
                }

                @Test
                @DisplayName("Should pass null keyword when keyword is blank")
                void getAllAccessibleDevices_BlankKeyword_PassesNullKeyword() {
                        // Arrange
                        Pageable pageable = PageRequest.of(0, 10);
                        Page<Device> emptyPage = Page.empty();

                        when(deviceRepository.searchDevices(USER_ID, false, null, pageable))
                                        .thenReturn(emptyPage);

                        // Act
                        deviceService.getAllAccessibleDevices(userPrincipal, "   ", pageable);

                        // Assert - blank keyword is normalized to null
                        verify(deviceRepository).searchDevices(USER_ID, false, null, pageable);
                }
        }

        // =====================================================================
        // getDeviceUsers()
        // =====================================================================
        @Nested
        @DisplayName("getDeviceUsers()")
        class GetDeviceUsers {

                @Test
                @DisplayName("Should return page of users with access to device")
                void getDeviceUsers_DeviceExists_ReturnsUserAccessPage() {
                        // Arrange
                        Pageable pageable = PageRequest.of(0, 10);
                        UserAccess mockProjection = mock(UserAccess.class);
                        Page<UserAccess> projectionPage = new PageImpl<>(List.of(mockProjection));

                        UserAccessResponseDto expectedDto = UserAccessResponseDto.builder()
                                        .userId(USER_ID).username("testuser").email("test@example.com")
                                        .permissionLevel("OWNER").build();

                        when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);
                        when(userRepository.findUsersWithAccessToDevice(DEVICE_ID, pageable))
                                        .thenReturn(projectionPage);
                        when(userMapper.toUserAccessResponse(mockProjection)).thenReturn(expectedDto);

                        // Act
                        Page<UserAccessResponseDto> result = deviceService.getDeviceUsers(DEVICE_ID, pageable);

                        // Assert
                        assertThat(result.getContent()).hasSize(1);
                        assertThat(result.getContent().getFirst().getPermissionLevel()).isEqualTo("OWNER");
                }

                @Test
                @DisplayName("Should throw NotFoundException when device does not exist")
                void getDeviceUsers_DeviceNotFound_ThrowsNotFoundException() {
                        // Arrange
                        Pageable pageable = PageRequest.of(0, 10);
                        when(deviceRepository.existsById(DEVICE_ID)).thenReturn(false);

                        // Act & Assert
                        assertThatThrownBy(() -> deviceService.getDeviceUsers(DEVICE_ID, pageable))
                                        .isInstanceOf(NotFoundException.class)
                                        .hasMessageContaining(DEVICE_ID.toString());
                }
        }
}
