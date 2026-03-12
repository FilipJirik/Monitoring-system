package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.entities.SystemSettings;
import cz.jirikfi.monitoringsystembackend.exceptions.BadRequestException;
import cz.jirikfi.monitoringsystembackend.exceptions.InternalErrorException;
import cz.jirikfi.monitoringsystembackend.models.settings.SettingsUpdateRequestDto;
import cz.jirikfi.monitoringsystembackend.repositories.SystemSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemSettingsServiceTest {

    @Mock
    private SystemSettingsRepository settingsRepository;

    @InjectMocks
    private SystemSettingsCacheService systemSettingsService;

    private SystemSettings defaultSettings;

    @BeforeEach
    void setUp() {
        defaultSettings = SystemSettings.builder()
                .id(SystemSettings.ID)
                .rawDataRetentionDays(7)
                .hourlyDataRetentionDays(90)
                .dailyDataRetentionDays(365)
                .deviceOfflineThresholdSeconds(120)
                .build();
    }

    // =====================================================================
    // getSettings()
    // =====================================================================
    @Nested
    @DisplayName("getSettings()")
    class GetSettings {

        @Test
        @DisplayName("Should return system settings when they exist")
        void getSettings_SettingsExist_ReturnsSettings() {
            // Arrange
            when(settingsRepository.findById(SystemSettings.ID))
                    .thenReturn(Optional.of(defaultSettings));

            // Act
            SystemSettings result = systemSettingsService.getSettings();

            // Assert
            assertThat(result.getRawDataRetentionDays()).isEqualTo(7);
            assertThat(result.getHourlyDataRetentionDays()).isEqualTo(90);
            assertThat(result.getDailyDataRetentionDays()).isEqualTo(365);
            assertThat(result.getDeviceOfflineThresholdSeconds()).isEqualTo(120);
        }

        @Test
        @DisplayName("Should throw InternalErrorException when settings are not initialized")
        void getSettings_NotInitialized_ThrowsInternalErrorException() {
            // Arrange
            when(settingsRepository.findById(SystemSettings.ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> systemSettingsService.getSettings())
                    .isInstanceOf(InternalErrorException.class)
                    .hasMessageContaining("DataSeeder");
        }
    }

    // =====================================================================
    // updateSettings()
    // =====================================================================
    @Nested
    @DisplayName("updateSettings()")
    class UpdateSettings {

        @Test
        @DisplayName("Should save valid settings")
        void updateSettings_ValidRequest_SavesSettings() {
            // Arrange — valid: raw(7) <= hourly(90) <= daily(365)
            SettingsUpdateRequestDto request = new SettingsUpdateRequestDto(7, 90, 365, 120);

            // Act
            systemSettingsService.updateSettings(request);

            // Assert
            verify(settingsRepository).save(argThat(saved -> {
                assertThat(saved.getId()).isEqualTo(SystemSettings.ID);
                assertThat(saved.getRawDataRetentionDays()).isEqualTo(7);
                assertThat(saved.getHourlyDataRetentionDays()).isEqualTo(90);
                assertThat(saved.getDailyDataRetentionDays()).isEqualTo(365);
                assertThat(saved.getDeviceOfflineThresholdSeconds()).isEqualTo(120);
                return true;
            }));
        }

        @Test
        @DisplayName("Should throw BadRequestException when hourly < raw retention")
        void updateSettings_HourlyLessThanRaw_ThrowsBadRequestException() {
            // Arrange — hourly(5) < raw(7) → invalid
            SettingsUpdateRequestDto request = new SettingsUpdateRequestDto(7, 5, 365, 120);

            // Act & Assert
            assertThatThrownBy(() -> systemSettingsService.updateSettings(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Hourly retention");

            verify(settingsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when daily < hourly retention")
        void updateSettings_DailyLessThanHourly_ThrowsBadRequestException() {
            // Arrange — daily(30) < hourly(90) → invalid
            SettingsUpdateRequestDto request = new SettingsUpdateRequestDto(7, 90, 30, 120);

            // Act & Assert
            assertThatThrownBy(() -> systemSettingsService.updateSettings(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Daily retention");

            verify(settingsRepository, never()).save(any());
        }
    }
}
