package cz.jirikfi.monitoringsystembackend.services;

import cz.jirikfi.monitoringsystembackend.configurations.CacheConfig;
import cz.jirikfi.monitoringsystembackend.entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.repositories.AlertThresholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdCacheService {

    private final AlertThresholdRepository thresholdRepository;

    @Cacheable(value = CacheConfig.DEVICE_THRESHOLDS, key = "#deviceId")
    @Transactional(readOnly = true)
    public List<AlertThreshold> getThresholdsByDevice(UUID deviceId) {
        log.debug("DB HIT: Loading thresholds for device: {}", deviceId);
        return thresholdRepository.findAllByDeviceId(deviceId);
    }

    @CacheEvict(value = CacheConfig.DEVICE_THRESHOLDS, key = "#deviceId")
    public void invalidateDeviceCache(UUID deviceId) {
        log.debug("CACHE EVICT: Invalidating thresholds for device: {}", deviceId);
    }
}
