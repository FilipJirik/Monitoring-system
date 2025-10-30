package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Metrics;
import cz.jirikfi.monitoringsystembackend.Repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.Repositories.MetricRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MetricsService {
    @Autowired
    private DeviceRepository deviceDatabase;
    @Autowired
    private MetricRepository metricDatabase;

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

    public List<Metrics> getMetrics(UUID deviceId) {
        return metricDatabase.findByDeviceIdOrderByTimestampDesc(deviceId);
    }



}
