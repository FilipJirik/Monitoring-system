package cz.jirikfi.monitoringsystembackend.components;

import cz.jirikfi.monitoringsystembackend.entities.*;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsDailyRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsHourlyRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import cz.jirikfi.monitoringsystembackend.repositories.projections.AggregatedMetric;
import cz.jirikfi.monitoringsystembackend.services.SystemSettingsService;
import cz.jirikfi.monitoringsystembackend.utils.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsProcessingJob {

    private final MetricsRepository metricsRepository;
    private final MetricsHourlyRepository metricsHourlyRepository;
    private final MetricsDailyRepository metricsDailyRepository;
    private final DeviceRepository deviceRepository;
    private final SystemSettingsService settingsService;

    // --- 1. Hourly Aggregation (Runs every hour) ---
    @Job(name = "Aggregate Raw Metrics to Hourly")
    @Transactional
    public void aggregateRawToHourly() {
        Instant now = Instant.now();
        Instant hourStart = now.truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS);
        Instant hourEnd = hourStart.plus(1, ChronoUnit.HOURS);

        log.info("Processing HOURLY aggregation: {} - {}", hourStart, hourEnd);

        List<Device> devices = deviceRepository.findAll();
        for (Device device : devices) {
            AggregatedMetric result = metricsRepository.findAggregatedValues(device.getId(), hourStart, hourEnd);

            // Have to check only one column if aggregation was successful
            if (result.getAvgCpuUsage() == null) {
                continue;
            }

            MetricsHourly hourly = MetricsHourly.builder()
                    .id(UuidGenerator.v7())
                    .device(device)
                    .timestamp(hourStart)
                    .cpuUsagePercent(result.getAvgCpuUsage())
                    .cpuTempCelsius(result.getAvgCpuTemp())
                    .cpuFreqAvgMhz(toLong(result.getAvgCpuFreq()))
                    .ramUsageMb(toLong(result.getAvgRamUsage()))
                    .diskUsagePercent(result.getAvgDiskUsage())
                    .networkInKbps(result.getAvgNetworkIn())
                    .networkOutKbps(result.getAvgNetworkOut())
                    .uptimeSeconds(result.getMaxUptime())
                    .build();

            metricsHourlyRepository.save(hourly);
        }
    }

    // --- 2. Daily Aggregation ---
    @Job(name = "Aggregate Hourly Metrics to Daily")
    @Transactional
    public void aggregateHourlyToDaily() {
        Instant now = Instant.now();
        Instant dayStart = now.truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.DAYS);
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);

        log.info("Processing DAILY aggregation: {} - {}", dayStart, dayEnd);

        List<Device> devices = deviceRepository.findAll();

        for (Device device : devices) {
            AggregatedMetric result = metricsHourlyRepository.findAggregatedValues(device.getId(), dayStart, dayEnd);

            if (result.getAvgCpuUsage() == null) {
                continue;
            }

            MetricsDaily daily = MetricsDaily.builder()
                    .id(UuidGenerator.v7())
                    .device(device)
                    .timestamp(dayStart)
                    .cpuUsagePercent(result.getAvgCpuUsage())
                    .cpuTempCelsius(result.getAvgCpuTemp())
                    .cpuFreqAvgMhz(toLong(result.getAvgCpuFreq()))
                    .ramUsageMb(toLong(result.getAvgRamUsage()))
                    .diskUsagePercent(result.getAvgDiskUsage())
                    .networkInKbps(result.getAvgNetworkIn())
                    .networkOutKbps(result.getAvgNetworkOut())
                    .uptimeSeconds(result.getMaxUptime())
                    .build();

            metricsDailyRepository.save(daily);
        }
    }

    @Job(name = "Cleanup Old Metrics")
    @Transactional
    public void cleanupOldData() {
        SystemSettings settings = settingsService.getSettings();

        Instant deleteRawBefore = Instant.now().minus(settings.getRawDataRetentionDays(), ChronoUnit.DAYS);
        Instant deleteHourlyBefore = Instant.now().minus(settings.getHourlyDataRetentionDays(), ChronoUnit.DAYS);
        Instant deleteDailyBefore = Instant.now().minus(settings.getDailyDataRetentionDays(), ChronoUnit.DAYS);

        log.info("Running cleanup. Deleting RAW before {}", deleteRawBefore);

        metricsRepository.deleteByTimestampBefore(deleteRawBefore);
        metricsHourlyRepository.deleteByTimestampBefore(deleteHourlyBefore);
        metricsDailyRepository.deleteByTimestampBefore(deleteDailyBefore);
    }

    private Long toLong(Double value) {
        if (value == null) return null;
        return Math.round(value);
    }
}