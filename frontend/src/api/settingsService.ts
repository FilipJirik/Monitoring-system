import api from './axios';

// ── Types ────────────────────────────────────────────────────────────────────

export interface SettingsResponseModel {
    rawDataRetentionDays: number;
    hourlyDataRetentionDays: number;
    dailyDataRetentionDays: number;
    deviceOfflineThresholdSeconds: number;
}

export interface SettingsUpdateModel {
    rawDataRetentionDays: number;       // 1–365
    hourlyDataRetentionDays: number;    // 7–1825
    dailyDataRetentionDays: number;     // ≥30
    deviceOfflineThresholdSeconds: number; // 30–86400
}

// ── API Functions ────────────────────────────────────────────────────────────

export async function getSettings(): Promise<SettingsResponseModel> {
    const { data } = await api.get('/settings');
    return data;
}

export async function updateSettings(
    payload: SettingsUpdateModel,
): Promise<void> {
    await api.put('/settings', payload);
}
