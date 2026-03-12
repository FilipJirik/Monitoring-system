import api from './axios';

// ── Types ────────────────────────────────────────────────────────────────────

export interface DeviceResponse {
    id: string;
    name: string;
    operatingSystem: string;
    ipAddress: string;
    macAddress?: string;
    description?: string;
    latitude?: number;
    longitude?: number;
    model?: string;
    sshEnabled?: boolean;
    lastSeen?: string;
    createdAt?: string;
    updatedAt?: string;
    ownerId?: string;
    ownerUsername?: string;
    imageFilename?: string;
    permissionLevel?: string;
}

export interface CreateDeviceModel {
    name: string;
    operatingSystem: string;
    ipAddress: string;
    macAddress?: string;
    description?: string;
    latitude?: number;
    longitude?: number;
    model?: string;
    sshEnabled?: boolean;
}

export interface UpdateDeviceModel {
    name?: string;
    operatingSystem?: string;
    ipAddress?: string;
    macAddress?: string;
    description?: string;
    latitude?: number;
    longitude?: number;
    model?: string;
    sshEnabled?: boolean;
}

export interface DeviceWithApiKeyModel {
    id: string;
    apiKey: string;
    setupCommand: string;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
    numberOfElements: number;
    empty: boolean;
}

// ── API Functions ────────────────────────────────────────────────────────────

export async function getDevices(
    page = 0,
    size = 10,
    keyword?: string,
): Promise<PageResponse<DeviceResponse>> {
    const params: Record<string, string | number> = { page, size };
    if (keyword?.trim()) {
        params.keyword = keyword.trim();
    }
    const { data } = await api.get('/devices', { params });
    return data;
}

export async function getDevice(id: string): Promise<DeviceResponse> {
    const { data } = await api.get(`/devices/${id}`);
    return data;
}

export async function createDevice(
    payload: CreateDeviceModel,
): Promise<DeviceWithApiKeyModel> {
    const { data } = await api.post('/devices', payload);
    return data;
}

export async function updateDevice(
    id: string,
    payload: UpdateDeviceModel,
): Promise<DeviceResponse> {
    const { data } = await api.put(`/devices/${id}`, payload);
    return data;
}

export async function deleteDevice(id: string): Promise<void> {
    await api.delete(`/devices/${id}`);
}

export async function regenerateApiKey(
    id: string,
): Promise<DeviceWithApiKeyModel> {
    const { data } = await api.post(`/devices/${id}/regenerate-api-key`);
    return data;
}

export async function uploadDeviceImage(
    id: string,
    file: File,
): Promise<void> {
    const formData = new FormData();
    formData.append('file', file);
    await api.post(`/devices/${id}/image`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
}

export async function deleteDeviceImage(id: string): Promise<void> {
    await api.delete(`/devices/${id}/image`);
}
