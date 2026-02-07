package cz.jirikfi.monitoringsystembackend.utils;

import cz.jirikfi.monitoringsystembackend.entities.AlertThreshold;
import cz.jirikfi.monitoringsystembackend.entities.BaseMetric;
import cz.jirikfi.monitoringsystembackend.enums.MetricType;

public class MetricUtil {

    public static boolean isThresholdBreached(Double value, AlertThreshold threshold) {
        if (value == null) return false;
        return switch (threshold.getOperator()) {
            case GREATER_THAN -> value > threshold.getThresholdValue();
            case LESS_THAN -> value < threshold.getThresholdValue();
            case EQUAL -> value.equals(threshold.getThresholdValue());
        };
    }

    public static Double getValue(BaseMetric metric, MetricType type) {
        return switch (type) {
            case CPU_USAGE -> metric.getCpuUsagePercent();
            case CPU_TEMP -> metric.getCpuTempCelsius();
            case CPU_FREQ -> metric.getCpuFreqAvgMhz() != null ? metric.getCpuFreqAvgMhz().doubleValue() : null;
            case RAM_USAGE -> metric.getRamUsageMb() != null ? metric.getRamUsageMb().doubleValue() : null;
            case DISK_USAGE -> metric.getDiskUsagePercent();
            case NETWORK_IN -> metric.getNetworkInKbps();
            case NETWORK_OUT -> metric.getNetworkOutKbps();
            case UPTIME -> metric.getUptimeSeconds() != null ? metric.getUptimeSeconds().doubleValue() : null;
        };
    }
}