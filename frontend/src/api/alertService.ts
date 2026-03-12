import api from './axios';
import type { PageResponse } from './deviceService';

// ── Types ────────────────────────────────────────────────────────────────────

export interface AlertResponseModel {
    id: string;
    deviceId: string;
    deviceName: string;
    metricType:
    | 'CPU_USAGE'
    | 'CPU_TEMP'
    | 'CPU_FREQ'
    | 'RAM_USAGE'
    | 'DISK_USAGE'
    | 'NETWORK_IN'
    | 'NETWORK_OUT'
    | 'UPTIME';
    severity: 'WARNING' | 'CRITICAL';
    isResolved: boolean;
    createdAt: string;
}

export type SeverityFilter = 'WARNING' | 'CRITICAL' | undefined;

// ── API Functions ────────────────────────────────────────────────────────────

export async function getAlerts(
    page = 0,
    size = 10,
    isResolved?: boolean,
    severity?: SeverityFilter,
): Promise<PageResponse<AlertResponseModel>> {
    const params: Record<string, string | number | boolean> = { page, size };
    if (isResolved !== undefined) {
        params.isResolved = isResolved;
    }
    if (severity) {
        params.severity = severity;
    }
    const { data } = await api.get('/alerts', { params });
    return data;
}

export async function resolveAlert(
    id: string,
): Promise<AlertResponseModel> {
    const { data } = await api.post(`/alerts/${id}/resolve`);
    return data;
}

export async function deleteAlert(id: string): Promise<void> {
    await api.delete(`/alerts/${id}`);
}
