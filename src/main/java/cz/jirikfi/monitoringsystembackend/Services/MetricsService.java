package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Device;
import cz.jirikfi.monitoringsystembackend.Entities.Metrics;
import cz.jirikfi.monitoringsystembackend.Exceptions.NotFoundException;
import cz.jirikfi.monitoringsystembackend.Mappers.MetricsMapper;
import cz.jirikfi.monitoringsystembackend.Models.Metrics.MetricsModel;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.MetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MetricsService {
    private final DeviceRepository deviceRepository;
    private final MetricRepository metricsRepository;
    private final MetricsMapper metricsMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Metrics saveMetrics(UUID deviceId, String rawApiKey, MetricsModel model) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));

        if (!passwordEncoder.matches(rawApiKey, device.getApiKey())) {
            throw new BadCredentialsException("Invalid API Key for device " + deviceId);
        }

        device.setLastSeen(Instant.now());
        deviceRepository.save(device);

        Metrics metrics = metricsMapper.toEntity(model, device);
        metricsRepository.save(metrics);
        return metrics;
    }

    @Transactional(readOnly = true)
    public Slice<MetricsModel> getMetricsHistory(UUID deviceId, Pageable pageable) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new NotFoundException("Device not found");
        }

        return metricsRepository.findByDeviceIdOrderByTimestampDesc(deviceId, pageable)
                .map(metricsMapper::toModel);
    }

    @Transactional(readOnly = true)
    public MetricsModel getLatestMetrics(UUID deviceId) {
        return metricsRepository.findFirstByDeviceIdOrderByTimestampDesc(deviceId)
                .map(metricsMapper::toModel)
                .orElse(null);
    }



    // TODO:
    // USER can choose from modes like (AVERAGE OF DAY, WEEK, OF MONTH) to make average of old records

    // REMEMBER LAST: week, day, month (always have unaveraged records of)
    // AVERAGE DAYS: week

//    public void makeAverageOfOldRecords(UUID id, int daysToAverage, int lastDaysToKeep) {
//        List<Metric> metrics = deviceDatabase.findAllById(id).stream().map(Device::getMetrics);
//
//        List<Timestamp> timestamps = metrics.stream().sorted(Comparator.comparing(Metric::getTimestamp).reversed()).map(Metric::getTimestamp).toList();
//        // NOTE:  lastTimestamp = timestamps.get(0);
//
//        Timestamp lastTimestamp = timestamps.get(0);
//        lastTimestamp.getDay();
//
//        int left = 0;
//        int right = timestamps.size();

//        for (int i = 0; i <= timestamps.size(); i++) {
//            if (timestamps.get(left).getDay() - timestamps.get(right / 2).getDay() < lastDaysToKeep) {
//
//            }
//        }
//    }




}
