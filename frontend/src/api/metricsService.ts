import api from './axios';

// ── Types ────────────────────────────────────────────────────────────────────

export type MetricType =
    | 'CPU_USAGE'
    | 'CPU_TEMP'
    | 'CPU_FREQ'
    | 'RAM_USAGE'
    | 'DISK_USAGE'
    | 'NETWORK_IN'
    | 'NETWORK_OUT'
    | 'UPTIME'
    | 'PROCESS_COUNT'
    | 'TCP_CONNECTIONS_COUNT'
    | 'LISTENING_PORTS_COUNT';

export type Period =
    | 'HOUR_1'
    | 'HOUR_6'
    | 'HOUR_12'
    | 'HOUR_24'
    | 'DAY_3'
    | 'DAY_7'
    | 'DAY_30'
    | 'DAY_90'
    | 'YEAR_1';

export interface MetricsStatusModel {
    lastSeen?: string;
    uptimeSeconds?: number;
    currentCpuUsage?: number;
    currentCpuFreqMhz?: number;
    currentRamUsage?: number;
    currentDiskUsage?: number;
    currentCpuTemp?: number;
    online?: boolean;
    currentProcessCount?: number;
    currentTcpConnectionsCount?: number;
    currentListeningPortsCount?: number;
}

export interface DataPoint {
    timestamp: string;
    value: number;
}

export interface MetricsHistoryModel {
    type: MetricType;
    label: string;
    unit: string;
    data: DataPoint[];
}

// ── API Functions ────────────────────────────────────────────────────────────

export async function getLatestMetrics(
    deviceId: string,
): Promise<MetricsStatusModel> {
    const { data } = await api.get(`/devices/${deviceId}/metrics/latest`);
    return data;
}

export async function getMetricsHistory(
    deviceId: string,
    type: MetricType,
    period: Period = 'HOUR_24',
): Promise<MetricsHistoryModel> {
    const { data } = await api.get(`/devices/${deviceId}/metrics/history`, {
        params: { type, period },
    });
    return data;
}
