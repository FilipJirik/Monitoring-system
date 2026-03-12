package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.Alert;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.AlertSeverity;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.exceptions.ConflictException;
import cz.jirikfi.monitoringsystembackend.exceptions.ForbiddenException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.AlertMapper;
import cz.jirikfi.monitoringsystembackend.models.alerts.AlertResponseDto;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRecipientRepository;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.repositories.projections.AlertSummary;
import cz.jirikfi.monitoringsystembackend.services.background.NotificationJobService;
import org.jobrunr.scheduling.JobScheduler;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertRecipientRepository alertRecipientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AlertMapper alertMapper;
    @Mock
    private JobScheduler jobScheduler;
    @InjectMocks
    private AlertService alertService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ALERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private UserPrincipal userPrincipal;
    private UserPrincipal adminPrincipal;
    private Device testDevice;
    private Alert testAlert;
    private AlertResponseDto expectedResponse;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(USER_ID).username("testuser").email("test@example.com")
                .role(Role.USER).password("encoded").build();

        adminPrincipal = UserPrincipal.builder()
                .id(USER_ID).username("admin").email("admin@example.com")
                .role(Role.ADMIN).password("encoded").build();

        User owner = User.builder().id(USER_ID).username("testuser").build();
        testDevice = Device.builder().id(DEVICE_ID).name("Server-1").owner(owner).build();

        testAlert = Alert.builder()
                .id(ALERT_ID)
                .device(testDevice)
                .metricType(MetricType.CPU_USAGE)
                .severity(AlertSeverity.CRITICAL)
                .message("CPU Usage exceeded threshold")
                .metricValue(95.0)
                .thresholdValue(90.0)
                .isResolved(false)
                .build();

        expectedResponse = AlertResponseDto.builder()
                .id(ALERT_ID)
                .deviceName("Server-1")
                .metricType(MetricType.CPU_USAGE)
                .severity(AlertSeverity.CRITICAL)
                .isResolved(false)
                .createdAt(Instant.now())
                .build();
    }

    // =====================================================================
    // getAlerts()
    // =====================================================================
    @Nested
    @DisplayName("getAlerts()")
    class GetAlerts {

        @Test
        @DisplayName("Should return filtered alerts page for regular user")
        void getAlerts_RegularUser_ReturnsMappedAlertPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            AlertSummary mockProjection = mock(AlertSummary.class);
            Page<AlertSummary> alertPage = new PageImpl<>(List.of(mockProjection));

            when(alertRepository.findAllFiltered(USER_ID, false, null, null, pageable))
                    .thenReturn(alertPage);
            when(alertMapper.toResponseModel(mockProjection)).thenReturn(expectedResponse);

            // Act
            Page<AlertResponseDto> result = alertService.getAlerts(userPrincipal, null, null, pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getDeviceName()).isEqualTo("Server-1");
        }

        @Test
        @DisplayName("Should pass isAdmin=true for admin users")
        void getAlerts_AdminUser_PassesIsAdminTrue() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<AlertSummary> emptyPage = Page.empty();

            when(alertRepository.findAllFiltered(USER_ID, true, true, AlertSeverity.CRITICAL, pageable))
                    .thenReturn(emptyPage);

            // Act
            alertService.getAlerts(adminPrincipal, true, AlertSeverity.CRITICAL, pageable);

            // Assert - verify admin flag and filters are passed through
            verify(alertRepository).findAllFiltered(USER_ID, true, true, AlertSeverity.CRITICAL, pageable);
        }
    }

    // =====================================================================
    // resolveAlert()
    // =====================================================================
    @Nested
    @DisplayName("resolveAlert()")
    class ResolveAlert {

        @Test
        @DisplayName("Should resolve alert when user is admin")
        void resolveAlert_AdminUser_ResolvesAlertAndEnqueuesNotification() {
            // Arrange
            User resolverRef = User.builder().id(USER_ID).build();

            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(testAlert));
            when(userRepository.getReferenceById(USER_ID)).thenReturn(resolverRef);
            when(alertRepository.save(testAlert)).thenReturn(testAlert);
            when(alertMapper.toResponseModel(testAlert)).thenReturn(expectedResponse);

            // Act
            Instant beforeResolve = Instant.now();
            AlertResponseDto result = alertService.resolveAlert(ALERT_ID, adminPrincipal);

            // Assert
            assertThat(result).isNotNull();
            assertThat(testAlert.getIsResolved()).isTrue();
            assertThat(testAlert.getResolvedBy()).isEqualTo(resolverRef);
            assertThat(testAlert.getResolvedAt())
                    .isCloseTo(beforeResolve, within(2, ChronoUnit.SECONDS));

            verify(alertRepository).save(testAlert);
            // Verify that a background notification job is enqueued
            verify(jobScheduler).enqueue(any(org.jobrunr.jobs.lambdas.JobLambda.class));
        }

        @Test
        @DisplayName("Should resolve alert when user is a recipient of the device")
        void resolveAlert_UserIsRecipient_ResolvesAlert() {
            // Arrange
            User resolverRef = User.builder().id(USER_ID).build();

            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(testAlert));
            // User is not admin, but is a recipient for this device
            when(alertRecipientRepository.existsByUserIdAndDeviceId(USER_ID, DEVICE_ID)).thenReturn(true);
            when(userRepository.getReferenceById(USER_ID)).thenReturn(resolverRef);
            when(alertRepository.save(testAlert)).thenReturn(testAlert);
            when(alertMapper.toResponseModel(testAlert)).thenReturn(expectedResponse);

            // Act
            AlertResponseDto result = alertService.resolveAlert(ALERT_ID, userPrincipal);

            // Assert
            assertThat(result).isNotNull();
            assertThat(testAlert.getIsResolved()).isTrue();
        }

        @Test
        @DisplayName("Should throw NotFoundException when alert does not exist")
        void resolveAlert_AlertNotFound_ThrowsNotFoundException() {
            // Arrange
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> alertService.resolveAlert(ALERT_ID, adminPrincipal))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(ALERT_ID.toString());
        }

        @Test
        @DisplayName("Should throw ForbiddenException when user is not admin and not recipient")
        void resolveAlert_NotAdminNotRecipient_ThrowsForbiddenException() {
            // Arrange
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(testAlert));
            when(alertRecipientRepository.existsByUserIdAndDeviceId(USER_ID, DEVICE_ID)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> alertService.resolveAlert(ALERT_ID, userPrincipal))
                    .isInstanceOf(ForbiddenException.class);

            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ConflictException when alert is already resolved")
        void resolveAlert_AlreadyResolved_ThrowsConflictException() {
            // Arrange
            testAlert.setIsResolved(true);
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(testAlert));

            // Act & Assert
            assertThatThrownBy(() -> alertService.resolveAlert(ALERT_ID, adminPrincipal))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Alert is already resolved");

            verify(alertRepository, never()).save(any());
        }
    }

    // =====================================================================
    // deleteAlert()
    // =====================================================================
    @Nested
    @DisplayName("deleteAlert()")
    class DeleteAlert {

        @Test
        @DisplayName("Should delete alert when user is admin")
        void deleteAlert_AdminUser_DeletesAlert() {
            // Arrange
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(testAlert));

            // Act
            alertService.deleteAlert(ALERT_ID, adminPrincipal);

            // Assert
            verify(alertRepository).delete(testAlert);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when user is not admin")
        void deleteAlert_NonAdminUser_ThrowsForbiddenException() {
            // Arrange
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(testAlert));

            // Act & Assert
            assertThatThrownBy(() -> alertService.deleteAlert(ALERT_ID, userPrincipal))
                    .isInstanceOf(ForbiddenException.class);

            verify(alertRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw NotFoundException when alert does not exist")
        void deleteAlert_AlertNotFound_ThrowsNotFoundException() {
            // Arrange
            when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> alertService.deleteAlert(ALERT_ID, adminPrincipal))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(ALERT_ID.toString());
        }
    }
}
