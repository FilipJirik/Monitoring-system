package cz.jirikfi.monitoringsystembackend.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettings {
    public static final int ID = 1;
    public static final int DEFAULT_RAW_RETENTION = 7;
    public static final int DEFAULT_HOURLY_RETENTION = 90;
    public static final int DEFAULT_DAILY_RETENTION = 365;
    public static final int DEFAULT_OFFLINE_THRESHOLD = 120;

    @Id
    private Integer id;

    @Column(nullable = false)
    private Integer rawDataRetentionDays;

    @Column(nullable = false)
    private Integer hourlyDataRetentionDays;

    @Column(nullable = false)
    private Integer dailyDataRetentionDays;

    @Column(nullable = false)
    private Integer deviceOfflineThresholdSeconds;
}