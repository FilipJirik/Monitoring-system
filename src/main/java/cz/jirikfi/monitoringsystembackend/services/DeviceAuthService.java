package cz.jirikfi.monitoringsystembackend.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.Hashing;
import cz.jirikfi.monitoringsystembackend.configurations.CacheConfig;
import cz.jirikfi.monitoringsystembackend.entities.Device;
import cz.jirikfi.monitoringsystembackend.entities.DevicePrincipal;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceAuthService {

    private final DeviceRepository deviceRepository;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    public String generateRawApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    public String hashApiKey(String rawKey) {
        return Hashing.sha256()
                .hashString(rawKey, StandardCharsets.UTF_8)
                .toString();
    }

    @Cacheable(value = CacheConfig.DEVICE_API_KEYS, key = "#rawApiKey", unless = "#result == null")
    @Transactional(readOnly = true)
    public DevicePrincipal resolveDeviceByApiKey(String rawApiKey) {
        String incomingHash = hashApiKey(rawApiKey);

        return deviceRepository.findAuthDataByApiKey(incomingHash)
                .map(device -> new DevicePrincipal(device.getId(), device.getName()))
                .orElse(null);
    }

    @CacheEvict(value = CacheConfig.DEVICE_API_KEYS, key = "#oldRawApiKey")
    public void invalidateKey(String oldRawApiKey) {
        log.info("Invalidating API Key cache");
    }


    // Used for updating lastSeen time for devices - throttle it to one minute to reduce db calls

    private final Cache<UUID, Boolean> activityCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    @Async
    @Transactional
    public void updateLastSeen(UUID deviceId) {
        if (activityCache.getIfPresent(deviceId) == null) {

            deviceRepository.findById(deviceId).ifPresent(device -> {
                device.setLastSeen(Instant.now());
                deviceRepository.save(device);

                activityCache.put(deviceId, true);
            });
        }
    }

}
