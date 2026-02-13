package cz.jirikfi.monitoringsystembackend.components;

import cz.jirikfi.monitoringsystembackend.entities.*;
import cz.jirikfi.monitoringsystembackend.mappers.MetricAggregationMapper;
import cz.jirikfi.monitoringsystembackend.repositories.DeviceRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsDailyRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsHourlyRepository;
import cz.jirikfi.monitoringsystembackend.repositories.MetricsRepository;
import cz.jirikfi.monitoringsystembackend.repositories.projections.AggregatedMetric;
import cz.jirikfi.monitoringsystembackend.services.SystemSettingsService;
import cz.jirikfi.monitoringsystembackend.utils.UuidGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsProcessingJob {

    private final MetricsRepository metricsRepository;
    private final MetricsHourlyRepository metricsHourlyRepository;
    private final MetricsDailyRepository metricsDailyRepository;
    private final DeviceRepository deviceRepository;

    private final SystemSettingsService settingsService;
    private final JobScheduler jobScheduler;
    private final MetricAggregationMapper aggregationMapper;


    @PostConstruct
    public void scheduleRecurrentJobs() {
        // Hourly aggregation: Run every hour at minute 05
        jobScheduler.scheduleRecurrently("aggregate-hourly", Cron.hourly(5), this::aggregateRawToHourly);

        // Daily aggregation: Run every day at 00:30
        jobScheduler.scheduleRecurrently("aggregate-daily", Cron.daily(0, 30), this::aggregateHourlyToDaily);

        // Retention cleanup: Run every day at 03:00
        jobScheduler.scheduleRecurrently("retention-cleanup", Cron.daily(3, 0), this::cleanupOldData);

        log.info("JobRunr recurring jobs were scheduled successfully.");
    }

    @Job(name = "Aggregate Raw Metrics to Hourly")
    public void aggregateRawToHourly() {
        Instant now = Instant.now();
        Instant hourStart = now.truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS);
        Instant hourEnd = hourStart.plus(1, ChronoUnit.HOURS);

        log.info("Processing HOURLY aggregation: {} - {}", hourStart, hourEnd);

        List<Device> devices = deviceRepository.findAll();
        for (Device device : devices) {
            try {
                AggregatedMetric result = metricsRepository.findAggregatedValues(device.getId(), hourStart, hourEnd);

                // Have to check only one column if aggregation was successful
                if (result == null || result.getAvgCpuUsage() == null)
                    continue;

                MetricsHourly hourly = new MetricsHourly();
                hourly.setId(UuidGenerator.v7());
                hourly.setDevice(device);
                hourly.setTimestamp(hourStart);

                aggregationMapper.mapAggregatedStats(result, hourly);

                metricsHourlyRepository.save(hourly);

            } catch (Exception e) {
                log.error("Error aggregating hourly for device {}", device.getId(), e);
            }
        }
    }

    @Job(name = "Aggregate Hourly to Daily")
    public void aggregateHourlyToDaily() {
        Instant now = Instant.now();
        Instant dayStart = now.truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.DAYS);
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);

        List<Device> devices = deviceRepository.findAll();
        for (Device device : devices) {
            try {
                AggregatedMetric result = metricsHourlyRepository.findAggregatedValues(device.getId(), dayStart, dayEnd);

                if (result == null || result.getAvgCpuUsage() == null)
                    continue;

                MetricsDaily daily = new MetricsDaily();
                daily.setId(UuidGenerator.v7());
                daily.setDevice(device);
                daily.setTimestamp(dayStart);

                aggregationMapper.mapAggregatedStats(result, daily);

                metricsDailyRepository.save(daily);

            } catch (Exception e) {
                log.error("Error aggregating daily for device {}", device.getId(), e);
            }
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
}