package cz.jirikfi.monitoringsystembackend.enums;

import lombok.Getter;

@Getter
public enum MetricType {
    CPU_USAGE("CPU Usage", "%"),
    CPU_TEMP("CPU Temperature", "°C"),
    CPU_FREQ("CPU Frequency", "MHz"),
    RAM_USAGE("RAM Usage", "MB"),
    DISK_USAGE("Disk Usage", "%"),
    NETWORK_IN("Network In", "Kbps"),
    NETWORK_OUT("Network Out", "Kbps"),
    UPTIME("Uptime", "s"),
    PROCESS_COUNT("Process Count", ""),
    TCP_CONNECTIONS_COUNT("TCP Connections", ""),
    LISTENING_PORTS_COUNT("Listening Ports", ""),
    DEVICE_OFFLINE("Device Offline", "");

    private final String label;
    private final String unit;

    MetricType(String label, String unit) {
        this.label = label;
        this.unit = unit;
    }
}
