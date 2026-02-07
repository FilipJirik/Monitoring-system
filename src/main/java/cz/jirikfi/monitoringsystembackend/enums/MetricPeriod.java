package cz.jirikfi.monitoringsystembackend.enums;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public enum MetricPeriod {
    HOUR_1(1, ChronoUnit.HOURS),
    HOUR_6(6, ChronoUnit.HOURS),
    HOUR_12(12, ChronoUnit.HOURS),
    HOUR_24(24, ChronoUnit.HOURS),
    DAY_3(3, ChronoUnit.DAYS),
    DAY_7(7, ChronoUnit.DAYS),
    DAY_30(30, ChronoUnit.DAYS),
    DAY_90(90, ChronoUnit.DAYS),
    YEAR_1(365, ChronoUnit.DAYS);

    private final int value;
    private final ChronoUnit unit;

    MetricPeriod(int value, ChronoUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public Instant getStartInstant() {
        return Instant.now().minus(value, unit);
    }
    public long getTotalHours() {
        return unit.getDuration().toHours() * value;
    }
}
