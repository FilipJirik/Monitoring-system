package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.AlertRecipient;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.entities.UserPrincipal;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.mappers.AlertRecipientMapper;
import cz.jirikfi.monitoringsystembackend.models.recipients.CreateRecipientRequestDto;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientResponseDto;
import cz.jirikfi.monitoringsystembackend.models.recipients.RecipientStatusDto;
import cz.jirikfi.monitoringsystembackend.models.recipients.UpdateRecipientRequestDto;
import cz.jirikfi.monitoringsystembackend.repositories.AlertRecipientRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.repositories.projections.RecipientStatus;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertRecipientServiceTest {

    @Mock
    private AlertRecipientRepository alertRecipientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private AlertRecipientMapper alertRecipientMapper;

    @InjectMocks
    private AlertRecipientService alertRecipientService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID RECIPIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private UserPrincipal userPrincipal;
    private Device testDevice;
    private User owner;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(USER_ID).username("testuser").email("test@example.com")
                .role(Role.USER).password("encoded").build();

        owner = User.builder().id(USER_ID).username("testuser").email("test@example.com").build();
        testDevice = Device.builder().id(DEVICE_ID).name("Server-1").owner(owner).build();
    }

    // =====================================================================
    // getRecipientsWithStatus()
    // =====================================================================
    @Nested
    @DisplayName("getRecipientsWithStatus()")
    class GetRecipientsWithStatus {

        @Test
        @DisplayName("Should return potential recipients when includePotential is true")
        void getRecipientsWithStatus_IncludePotential_ReturnsFromUserRepository() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            RecipientStatus mockProjection = mock(RecipientStatus.class);
            Page<RecipientStatus> projectionPage = new PageImpl<>(List.of(mockProjection));
            RecipientStatusDto expectedDto = new RecipientStatusDto(
                    USER_ID, "testuser", "test@example.com", true, false, true, true, true);

            when(userRepository.findPotentialRecipients(DEVICE_ID, pageable)).thenReturn(projectionPage);
            when(alertRecipientMapper.mapProjectionToModel(mockProjection)).thenReturn(expectedDto);

            // Act
            Page<RecipientStatusDto> result = alertRecipientService.getRecipientsWithStatus(
                    userPrincipal, DEVICE_ID, true, pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            // includePotential=true requires edit access
            verify(authorizationService).verifyEditAccess(DEVICE_ID, userPrincipal);
        }

        @Test
        @DisplayName("Should return active recipients when includePotential is false")
        void getRecipientsWithStatus_ActiveOnly_ReturnsFromRecipientRepository() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<RecipientStatus> emptyPage = Page.empty();

            when(alertRecipientRepository.findActiveRecipientsProjected(DEVICE_ID, pageable))
                    .thenReturn(emptyPage);

            // Act
            alertRecipientService.getRecipientsWithStatus(userPrincipal, DEVICE_ID, false, pageable);

            // Assert - includePotential=false requires only read access
            verify(authorizationService).verifyReadAccess(DEVICE_ID, userPrincipal);
            verify(userRepository, never()).findPotentialRecipients(any(), any());
        }
    }

    // =====================================================================
    // addRecipient()
    // =====================================================================
    @Nested
    @DisplayName("addRecipient()")
    class AddRecipient {

        @Test
        @DisplayName("Should add self as recipient when userId is null (self-subscribe)")
        void addRecipient_SelfSubscribe_RequiresReadAccess() {
            // Arrange
            CreateRecipientRequestDto request = CreateRecipientRequestDto.builder()
                    .userId(null) // null means self
                    .notifyEmail(true).notifyFrontend(true).build();

            AlertRecipient savedRecipient = AlertRecipient.builder()
                    .id(RECIPIENT_ID).device(testDevice).user(owner)
                    .notifyEmail(true).notifyFrontend(true).build();

            RecipientResponseDto expectedResponse = RecipientResponseDto.builder()
                    .id(RECIPIENT_ID).userId(USER_ID).username("testuser")
                    .email("test@example.com").notifyEmail(true).notifyFrontend(true).build();

            when(alertRecipientRepository.existsByDeviceIdAndUserId(DEVICE_ID, USER_ID)).thenReturn(false);
            when(authorizationService.getDeviceWithReadAccess(DEVICE_ID, userPrincipal)).thenReturn(testDevice);
            when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
            when(alertRecipientRepository.save(any(AlertRecipient.class))).thenReturn(savedRecipient);
            when(alertRecipientMapper.toResponse(savedRecipient)).thenReturn(expectedResponse);

            // Act
            RecipientResponseDto result = alertRecipientService.addRecipient(userPrincipal, DEVICE_ID, request);

            // Assert
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            // Self-subscribe requires only read access
            verify(authorizationService).verifyReadAccess(DEVICE_ID, userPrincipal);
        }

        @Test
        @DisplayName("Should add another user as recipient requiring edit access")
        void addRecipient_OtherUser_RequiresEditAccess() {
            // Arrange
            CreateRecipientRequestDto request = CreateRecipientRequestDto.builder()
                    .userId(OTHER_USER_ID) // adding someone else
                    .notifyEmail(true).notifyFrontend(false).build();

            User otherUser = User.builder().id(OTHER_USER_ID).username("other").build();
            AlertRecipient savedRecipient = AlertRecipient.builder()
                    .id(RECIPIENT_ID).device(testDevice).user(otherUser)
                    .notifyEmail(true).notifyFrontend(false).build();

            RecipientResponseDto expectedResponse = RecipientResponseDto.builder()
                    .id(RECIPIENT_ID).userId(OTHER_USER_ID).build();

            when(alertRecipientRepository.existsByDeviceIdAndUserId(DEVICE_ID, OTHER_USER_ID)).thenReturn(false);
            when(userRepository.existsById(OTHER_USER_ID)).thenReturn(true);
            when(authorizationService.getDeviceWithReadAccess(DEVICE_ID, userPrincipal)).thenReturn(testDevice);
            when(userRepository.getReferenceById(OTHER_USER_ID)).thenReturn(otherUser);
            when(alertRecipientRepository.save(any(AlertRecipient.class))).thenReturn(savedRecipient);
            when(alertRecipientMapper.toResponse(savedRecipient)).thenReturn(expectedResponse);

            // Act
            RecipientResponseDto result = alertRecipientService.addRecipient(userPrincipal, DEVICE_ID, request);

            // Assert
            assertThat(result.getUserId()).isEqualTo(OTHER_USER_ID);
            // Adding another user requires edit access
            verify(authorizationService).verifyEditAccess(DEVICE_ID, userPrincipal);
        }

        @Test
        @DisplayName("Should throw BadRequestException when user is already a recipient")
        void addRecipient_AlreadyRecipient_ThrowsBadRequestException() {
            // Arrange
            CreateRecipientRequestDto request = CreateRecipientRequestDto.builder()
                    .userId(null).build();

            when(alertRecipientRepository.existsByDeviceIdAndUserId(DEVICE_ID, USER_ID)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> alertRecipientService.addRecipient(userPrincipal, DEVICE_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already a recipient");
        }

        @Test
        @DisplayName("Should throw NotFoundException when target user does not exist")
        void addRecipient_TargetUserNotFound_ThrowsNotFoundException() {
            // Arrange
            CreateRecipientRequestDto request = CreateRecipientRequestDto.builder()
                    .userId(OTHER_USER_ID).build();

            when(alertRecipientRepository.existsByDeviceIdAndUserId(DEVICE_ID, OTHER_USER_ID)).thenReturn(false);
            when(userRepository.existsById(OTHER_USER_ID)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> alertRecipientService.addRecipient(userPrincipal, DEVICE_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(OTHER_USER_ID.toString());
        }
    }

    // =====================================================================
    // updateRecipient()
    // =====================================================================
    @Nested
    @DisplayName("updateRecipient()")
    class UpdateRecipient {

        @Test
        @DisplayName("Should update own recipient settings with read access")
        void updateRecipient_SelfUpdate_RequiresReadAccess() {
            // Arrange
            UpdateRecipientRequestDto request = UpdateRecipientRequestDto.builder()
                    .notifyEmail(false).notifyFrontend(true).build();

            AlertRecipient existingRecipient = AlertRecipient.builder()
                    .id(RECIPIENT_ID).device(testDevice).user(owner)
                    .notifyEmail(true).notifyFrontend(true).build();

            RecipientResponseDto expectedResponse = RecipientResponseDto.builder()
                    .id(RECIPIENT_ID).userId(USER_ID).notifyEmail(false).notifyFrontend(true).build();

            when(alertRecipientRepository.findByDeviceIdAndUserId(DEVICE_ID, USER_ID))
                    .thenReturn(Optional.of(existingRecipient));
            when(alertRecipientRepository.save(existingRecipient)).thenReturn(existingRecipient);
            when(alertRecipientMapper.toResponse(existingRecipient)).thenReturn(expectedResponse);

            // Act
            RecipientResponseDto result = alertRecipientService.updateRecipient(
                    userPrincipal, DEVICE_ID, USER_ID, request);

            // Assert
            assertThat(result.getNotifyEmail()).isFalse();
            assertThat(existingRecipient.getNotifyEmail()).isFalse();
            verify(authorizationService).verifyReadAccess(DEVICE_ID, userPrincipal);
        }

        @Test
        @DisplayName("Should throw NotFoundException when recipient not found")
        void updateRecipient_RecipientNotFound_ThrowsNotFoundException() {
            // Arrange
            UpdateRecipientRequestDto request = UpdateRecipientRequestDto.builder().build();

            when(alertRecipientRepository.findByDeviceIdAndUserId(DEVICE_ID, USER_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> alertRecipientService.updateRecipient(
                    userPrincipal, DEVICE_ID, USER_ID, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Recipient not found");
        }
    }

    // =====================================================================
    // deleteRecipient()
    // =====================================================================
    @Nested
    @DisplayName("deleteRecipient()")
    class DeleteRecipient {

        @Test
        @DisplayName("Should delete own subscription with read access")
        void deleteRecipient_SelfUnsubscribe_RequiresReadAccess() {
            // Arrange
            AlertRecipient existingRecipient = AlertRecipient.builder()
                    .id(RECIPIENT_ID).device(testDevice).user(owner).build();

            when(alertRecipientRepository.findByDeviceIdAndUserId(DEVICE_ID, USER_ID))
                    .thenReturn(Optional.of(existingRecipient));

            // Act
            alertRecipientService.deleteRecipient(userPrincipal, DEVICE_ID, USER_ID);

            // Assert
            verify(authorizationService).verifyReadAccess(DEVICE_ID, userPrincipal);
            verify(alertRecipientRepository).delete(existingRecipient);
        }

        @Test
        @DisplayName("Should require edit access when deleting another user's subscription")
        void deleteRecipient_OtherUser_RequiresEditAccess() {
            // Arrange
            User otherUser = User.builder().id(OTHER_USER_ID).username("other").build();
            AlertRecipient existingRecipient = AlertRecipient.builder()
                    .id(RECIPIENT_ID).device(testDevice).user(otherUser).build();

            when(alertRecipientRepository.findByDeviceIdAndUserId(DEVICE_ID, OTHER_USER_ID))
                    .thenReturn(Optional.of(existingRecipient));

            // Act
            alertRecipientService.deleteRecipient(userPrincipal, DEVICE_ID, OTHER_USER_ID);

            // Assert
            verify(authorizationService).verifyEditAccess(DEVICE_ID, userPrincipal);
            verify(alertRecipientRepository).delete(existingRecipient);
        }

        @Test
        @DisplayName("Should throw NotFoundException when recipient not found")
        void deleteRecipient_RecipientNotFound_ThrowsNotFoundException() {
            // Arrange
            when(alertRecipientRepository.findByDeviceIdAndUserId(DEVICE_ID, USER_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> alertRecipientService.deleteRecipient(userPrincipal, DEVICE_ID, USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Recipient not found");
        }
    }
}
