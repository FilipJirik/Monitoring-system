package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.enums.ThresholdOperator;
import cz.jirikfi.monitoringsystembackend.exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.exceptions.ForbiddenException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.AlertThresholdMapper;
import cz.jirikfi.monitoringsystembackend.models.thresholds.CreateThresholdRequestDto;
import cz.jirikfi.monitoringsystembackend.models.thresholds.ThresholdResponseDto;
import cz.jirikfi.monitoringsystembackend.models.thresholds.UpdateThresholdRequestDto;
import cz.jirikfi.monitoringsystembackend.repositories.AlertThresholdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertThresholdServiceTest {

    @Mock
    private AlertThresholdRepository alertThresholdRepository;
    @Mock
    private ThresholdCacheService thresholdCacheService;
    @Mock
    private AlertThresholdMapper alertThresholdMapper;
    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private AlertThresholdService alertThresholdService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID THRESHOLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID WRONG_DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private UserPrincipal userPrincipal;
    private Device testDevice;
    private AlertThreshold testThreshold;
    private ThresholdResponseDto expectedResponse;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(USER_ID).username("testuser").email("test@example.com")
                .role(Role.USER).password("encoded").build();

        User owner = User.builder().id(USER_ID).username("testuser").build();
        testDevice = Device.builder().id(DEVICE_ID).name("Server-1").owner(owner).build();

        testThreshold = AlertThreshold.builder()
                .id(THRESHOLD_ID)
                .device(testDevice)
                .metricType(MetricType.CPU_USAGE)
                .operator(ThresholdOperator.GREATER_THAN)
                .thresholdValue(90.0)
                .severity(AlertSeverity.CRITICAL)
                .build();

        expectedResponse = ThresholdResponseDto.builder()
                .id(THRESHOLD_ID)
                .metricType(MetricType.CPU_USAGE)
                .operator(ThresholdOperator.GREATER_THAN)
                .thresholdValue(90.0)
                .severity(AlertSeverity.CRITICAL)
                .build();
    }

    // =====================================================================
    // getThresholdsByDeviceId()
    // =====================================================================
    @Nested
    @DisplayName("getThresholdsByDeviceId()")
    class GetThresholdsByDeviceId {

        @Test
        @DisplayName("Should return thresholds list after verifying read access")
        void getThresholdsByDeviceId_HasReadAccess_ReturnsMappedList() {
            // Arrange
            when(alertThresholdRepository.findByDevice_IdOrderByMetricType(DEVICE_ID))
                    .thenReturn(List.of(testThreshold));
            when(alertThresholdMapper.toResponse(testThreshold)).thenReturn(expectedResponse);

            // Act
            List<ThresholdResponseDto> result = alertThresholdService.getThresholdsByDeviceId(
                    userPrincipal, DEVICE_ID);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getMetricType()).isEqualTo(MetricType.CPU_USAGE);
            verify(authorizationService).verifyReadAccess(DEVICE_ID, userPrincipal);
        }

        @Test
        @DisplayName("Should propagate ForbiddenException when user has no access")
        void getThresholdsByDeviceId_NoAccess_ThrowsForbiddenException() {
            // Arrange
            doThrow(new ForbiddenException("You don't have permission to access this device"))
                    .when(authorizationService).verifyReadAccess(DEVICE_ID, userPrincipal);

            // Act & Assert
            assertThatThrownBy(() -> alertThresholdService.getThresholdsByDeviceId(userPrincipal, DEVICE_ID))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // =====================================================================
    // createThreshold()
    // =====================================================================
    @Nested
    @DisplayName("createThreshold()")
    class CreateThreshold {

        @Test
        @DisplayName("Should create threshold and invalidate cache")
        void createThreshold_HasEditAccess_SavesAndInvalidatesCache() {
            // Arrange
            CreateThresholdRequestDto request = CreateThresholdRequestDto.builder()
                    .metricType(MetricType.CPU_USAGE)
                    .operator(ThresholdOperator.GREATER_THAN)
                    .thresholdValue(90.0)
                    .severity(AlertSeverity.CRITICAL)
                    .build();

            when(authorizationService.getDeviceWithEditAccess(DEVICE_ID, userPrincipal)).thenReturn(testDevice);
            when(alertThresholdMapper.createToEntity(request, testDevice)).thenReturn(testThreshold);
            when(alertThresholdRepository.save(testThreshold)).thenReturn(testThreshold);
            when(alertThresholdMapper.toResponse(testThreshold)).thenReturn(expectedResponse);

            // Act
            ThresholdResponseDto result = alertThresholdService.createThreshold(
                    userPrincipal, DEVICE_ID, request);

            // Assert
            assertThat(result.getMetricType()).isEqualTo(MetricType.CPU_USAGE);
            assertThat(result.getThresholdValue()).isEqualTo(90.0);
            verify(thresholdCacheService).invalidateDeviceCache(DEVICE_ID);
        }
    }

    // =====================================================================
    // updateThreshold()
    // =====================================================================
    @Nested
    @DisplayName("updateThreshold()")
    class UpdateThreshold {

        @Test
        @DisplayName("Should update threshold and invalidate cache")
        void updateThreshold_ValidThreshold_UpdatesAndInvalidatesCache() {
            // Arrange
            UpdateThresholdRequestDto request = UpdateThresholdRequestDto.builder()
                    .metricType(MetricType.RAM_USAGE)
                    .operator(ThresholdOperator.GREATER_THAN)
                    .thresholdValue(8000.0)
                    .severity(AlertSeverity.WARNING)
                    .build();

            when(alertThresholdRepository.findByIdWithDevice(THRESHOLD_ID))
                    .thenReturn(Optional.of(testThreshold));
            when(alertThresholdRepository.save(testThreshold)).thenReturn(testThreshold);
            when(alertThresholdMapper.toResponse(testThreshold)).thenReturn(expectedResponse);

            // Act
            ThresholdResponseDto result = alertThresholdService.updateThreshold(
                    userPrincipal, DEVICE_ID, THRESHOLD_ID, request);

            // Assert
            assertThat(result).isNotNull();
            verify(alertThresholdMapper).updateEntity(testThreshold, request);
            verify(authorizationService).verifyEditPermission(userPrincipal, testDevice);
            verify(thresholdCacheService).invalidateDeviceCache(DEVICE_ID);
        }

        @Test
        @DisplayName("Should throw NotFoundException when threshold does not exist")
        void updateThreshold_ThresholdNotFound_ThrowsNotFoundException() {
            // Arrange
            UpdateThresholdRequestDto request = UpdateThresholdRequestDto.builder().build();

            when(alertThresholdRepository.findByIdWithDevice(THRESHOLD_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> alertThresholdService.updateThreshold(
                    userPrincipal, DEVICE_ID, THRESHOLD_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(THRESHOLD_ID.toString());
        }

        @Test
        @DisplayName("Should throw BadRequestException when threshold belongs to a different device")
        void updateThreshold_WrongDevice_ThrowsBadRequestException() {
            // Arrange
            UpdateThresholdRequestDto request = UpdateThresholdRequestDto.builder().build();

            when(alertThresholdRepository.findByIdWithDevice(THRESHOLD_ID))
                    .thenReturn(Optional.of(testThreshold));

            // Act & Assert - threshold.device.id is DEVICE_ID but we pass WRONG_DEVICE_ID
            assertThatThrownBy(() -> alertThresholdService.updateThreshold(
                    userPrincipal, WRONG_DEVICE_ID, THRESHOLD_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong to device");
        }
    }

    // =====================================================================
    // deleteThreshold()
    // =====================================================================
    @Nested
    @DisplayName("deleteThreshold()")
    class DeleteThreshold {

        @Test
        @DisplayName("Should delete threshold and invalidate cache")
        void deleteThreshold_ValidThreshold_DeletesAndInvalidatesCache() {
            // Arrange
            when(alertThresholdRepository.findByIdWithDevice(THRESHOLD_ID))
                    .thenReturn(Optional.of(testThreshold));

            // Act
            alertThresholdService.deleteThreshold(userPrincipal, DEVICE_ID, THRESHOLD_ID);

            // Assert
            verify(authorizationService).verifyEditPermission(userPrincipal, testDevice);
            verify(alertThresholdRepository).delete(testThreshold);
            verify(thresholdCacheService).invalidateDeviceCache(DEVICE_ID);
        }

        @Test
        @DisplayName("Should throw NotFoundException when threshold does not exist")
        void deleteThreshold_ThresholdNotFound_ThrowsNotFoundException() {
            // Arrange
            when(alertThresholdRepository.findByIdWithDevice(THRESHOLD_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> alertThresholdService.deleteThreshold(
                    userPrincipal, DEVICE_ID, THRESHOLD_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(THRESHOLD_ID.toString());
        }

        @Test
        @DisplayName("Should throw BadRequestException when threshold belongs to a different device")
        void deleteThreshold_WrongDevice_ThrowsBadRequestException() {
            // Arrange
            when(alertThresholdRepository.findByIdWithDevice(THRESHOLD_ID))
                    .thenReturn(Optional.of(testThreshold));

            // Act & Assert
            assertThatThrownBy(() -> alertThresholdService.deleteThreshold(
                    userPrincipal, WRONG_DEVICE_ID, THRESHOLD_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong to device");
        }
    }
}
