import api from './axios';
import type { PageResponse } from './deviceService';

// ── Types ────────────────────────────────────────────────────────────────────

export type PermissionLevel = 'READ' | 'EDIT' | 'OWNER';

export interface DeviceAccessResponse {
    deviceId: string;
    deviceName: string;
    permissionLevel: string;   // READ | EDIT | OWNER
}

export interface UserAccessResponse {
    userId: string;
    username: string;
    email: string;
    permissionLevel: string;   // READ | EDIT | OWNER
}

export interface CreatePermissionRequest {
    permissionLevel: 'READ' | 'EDIT';
}

export interface UpdatePermissionRequest {
    permissionLevel: 'READ' | 'EDIT';
}

// ── API Functions ────────────────────────────────────────────────────────────

/** GET /api/users/{userId}/devices  →  paginated DeviceAccessResponseDto */
export async function getUserDeviceAccesses(
    userId: string,
    page = 0,
    size = 100,
): Promise<PageResponse<DeviceAccessResponse>> {
    const { data } = await api.get(`/users/${userId}/devices`, {
        params: { page, size },
    });
    return data;
}

/** GET /api/devices/{deviceId}/users  →  paginated UserAccessResponseDto */
export async function getDeviceUserAccesses(
    deviceId: string,
    page = 0,
    size = 100,
): Promise<PageResponse<UserAccessResponse>> {
    const { data } = await api.get(`/devices/${deviceId}/users`, {
        params: { page, size },
    });
    return data;
}

export async function grantDeviceAccess(
    userId: string,
    deviceId: string,
    permissionLevel: 'READ' | 'EDIT',
): Promise<void> {
    await api.post(`/users/${userId}/devices/${deviceId}`, {
        permissionLevel,
    });
}

export async function updateDeviceAccess(
    userId: string,
    deviceId: string,
    permissionLevel: 'READ' | 'EDIT',
): Promise<void> {
    await api.put(`/users/${userId}/devices/${deviceId}`, {
        permissionLevel,
    });
}

export async function revokeDeviceAccess(
    userId: string,
    deviceId: string,
): Promise<void> {
    await api.delete(`/users/${userId}/devices/${deviceId}`);
}
