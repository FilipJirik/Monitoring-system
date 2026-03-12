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
    | 'LISTENING_PORTS_COUNT'
    | 'DEVICE_OFFLINE';

export type Operator = 'GREATER_THAN' | 'LESS_THAN' | 'EQUAL' | 'IS_NULL' | 'IS_NOT_NULL';
export type Severity = 'WARNING' | 'CRITICAL';

export interface ThresholdResponseModel {
    id: string;
    metricType: MetricType;
    operator: Operator;
    thresholdValue: number;
    severity: Severity;
}

export interface CreateThresholdModel {
    metricType: MetricType;
    operator: Operator;
    thresholdValue: number;
    severity: Severity;
}

// ── API Functions ────────────────────────────────────────────────────────────

export async function getThresholds(
    deviceId: string,
): Promise<ThresholdResponseModel[]> {
    const { data } = await api.get(`/devices/${deviceId}/thresholds`);
    return data;
}

export async function createThreshold(
    deviceId: string,
    payload: CreateThresholdModel,
): Promise<ThresholdResponseModel> {
    const { data } = await api.post(`/devices/${deviceId}/thresholds`, payload);
    return data;
}

export async function deleteThreshold(
    deviceId: string,
    thresholdId: string,
): Promise<void> {
    await api.delete(`/devices/${deviceId}/thresholds/${thresholdId}`);
}

export async function updateThreshold(
    deviceId: string,
    thresholdId: string,
    payload: CreateThresholdModel,
): Promise<ThresholdResponseModel> {
    const { data } = await api.put(`/devices/${deviceId}/thresholds/${thresholdId}`, payload);
    return data;
}
