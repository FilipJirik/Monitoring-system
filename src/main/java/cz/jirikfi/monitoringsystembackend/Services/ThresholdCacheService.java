package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.Repositories.AlertThresholdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ThresholdCacheService {

    private final AlertThresholdRepository thresholdRepository;

    private final Map<UUID, List<AlertThreshold>> cache = new ConcurrentHashMap<>();

    public List<AlertThreshold> getThresholds(UUID deviceId) {
        return cache.computeIfAbsent(deviceId, thresholdRepository::findAllByDeviceId);
    }
    public void refreshCache(UUID deviceId) {
        cache.remove(deviceId);
    }
}
