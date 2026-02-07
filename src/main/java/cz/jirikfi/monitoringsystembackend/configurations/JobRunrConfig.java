package cz.jirikfi.monitoringsystembackend.configurations;

import cz.jirikfi.monitoringsystembackend.components.MetricsProcessingJob;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@RequiredArgsConstructor
public class JobRunrConfig {

    private final JobScheduler jobScheduler;
    @Lazy
    private final MetricsProcessingJob metricsJob;

    @PostConstruct
    public void scheduleRecurrentJobs() {
        // Hourly aggregation: Run every hour at minute 05
        jobScheduler.scheduleRecurrently("aggregate-hourly", Cron.hourly(5), metricsJob::aggregateRawToHourly);

        // Daily aggregation: Run every day at 00:30
        jobScheduler.scheduleRecurrently("aggregate-daily", Cron.daily(0, 30), metricsJob::aggregateHourlyToDaily);

        // Retention cleanup: Run every day at 03:00
        jobScheduler.scheduleRecurrently("retention-cleanup", Cron.daily(3, 0), metricsJob::cleanupOldData);
    }
}