package cz.jirikfi.monitoringsystembackend.components;

import cz.jirikfi.monitoringsystembackend.entities.SystemSettings;
import cz.jirikfi.monitoringsystembackend.entities.User;
import cz.jirikfi.monitoringsystembackend.enums.Role;
import cz.jirikfi.monitoringsystembackend.repositories.SystemSettingsRepository;
import cz.jirikfi.monitoringsystembackend.repositories.UserRepository;
import cz.jirikfi.monitoringsystembackend.utils.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final SystemSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Load default admin credentials from application.yml
    @Value("${app.security.admin-email}")
    private String adminEmail;

    @Value("${app.security.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedSettings();
        seedAdmin();
    }

    private void seedSettings() {
        if (!settingsRepository.existsById(SystemSettings.ID)) {
            log.info("Seeding default System Settings...");

            SystemSettings defaults = SystemSettings.builder()
                    .id(SystemSettings.ID)
                    .rawDataRetentionDays(SystemSettings.DEFAULT_RAW_RETENTION)
                    .hourlyDataRetentionDays(SystemSettings.DEFAULT_HOURLY_RETENTION)
                    .dailyDataRetentionDays(SystemSettings.DEFAULT_DAILY_RETENTION)
                    .deviceOfflineThresholdSeconds(SystemSettings.DEFAULT_OFFLINE_THRESHOLD)
                    .build();

            settingsRepository.save(defaults);
        }
    }

    private void seedAdmin() {

        User user = userRepository.findByEmail(adminEmail);

        if (user == null) {
            log.info("Seeding default Admin User ({})", adminEmail);

            User admin = User.builder()
                    .id(UuidGenerator.v7())
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .username("Administrator")
                    .role(Role.ADMIN)
                    .createdAt(Instant.now())
                    .build();

            userRepository.save(admin);
        }
    }
}
