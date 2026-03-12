import api from './axios';
import type { PageResponse } from './deviceService';

// ── Types ────────────────────────────────────────────────────────────────────

export interface RecipientStatusModel {
    userId: string;
    username: string;
    email: string;
    notifyEmail: boolean;
    notifyFrontend: boolean;
    owner: boolean;
    recipient: boolean;
    admin: boolean;
}

export interface CreateRecipientModel {
    userId: string;
    notifyEmail: boolean;
    notifyFrontend: boolean;
}

export interface UpdateRecipientModel {
    notifyEmail: boolean;
    notifyFrontend: boolean;
}

// ── API Functions ────────────────────────────────────────────────────────────

export async function getRecipients(
    deviceId: string,
    all = true,
    page = 0,
    size = 50,
): Promise<PageResponse<RecipientStatusModel>> {
    const { data } = await api.get(`/devices/${deviceId}/recipients`, {
        params: { all, page, size },
    });
    return data;
}

export async function addRecipient(
    deviceId: string,
    payload: CreateRecipientModel,
): Promise<void> {
    await api.post(`/devices/${deviceId}/recipients`, payload);
}

export async function updateRecipient(
    deviceId: string,
    recipientUserId: string,
    payload: UpdateRecipientModel,
): Promise<void> {
    await api.put(
        `/devices/${deviceId}/recipients/${recipientUserId}`,
        payload,
    );
}

export async function deleteRecipient(
    deviceId: string,
    recipientUserId: string,
): Promise<void> {
    await api.delete(`/devices/${deviceId}/recipients/${recipientUserId}`);
}
