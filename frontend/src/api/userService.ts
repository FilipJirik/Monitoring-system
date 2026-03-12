import api from './axios';
import type { PageResponse } from './deviceService';

// ── Types ────────────────────────────────────────────────────────────────────

export interface UserResponse {
    id: string;
    username: string;
    email: string;
}

export interface CreateUserModel {
    username: string;
    password: string;
    email?: string;
}

export interface UpdateUserModel {
    username?: string;
    password?: string;
    email?: string;
    role?: 'USER' | 'ADMIN';
}

// ── API Functions ────────────────────────────────────────────────────────────

export async function getUsers(
    page = 0,
    size = 10,
    keyword?: string,
): Promise<PageResponse<UserResponse>> {
    const params: Record<string, string | number> = { page, size };
    if (keyword?.trim()) {
        params.keyword = keyword.trim();
    }
    const { data } = await api.get('/users', { params });
    return data;
}

export async function getUser(id: string): Promise<UserResponse> {
    const { data } = await api.get(`/users/${id}`);
    return data;
}

export async function createUser(payload: CreateUserModel): Promise<UserResponse> {
    const { data } = await api.post('/users', payload);
    return data;
}

export async function updateUser(
    id: string,
    payload: UpdateUserModel,
): Promise<UserResponse> {
    const { data } = await api.put(`/users/${id}`, payload);
    return data;
}

export async function deleteUser(id: string): Promise<void> {
    await api.delete(`/users/${id}`);
}
