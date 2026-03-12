import { create } from 'zustand';
import api from '../api/axios';

export interface UserInfo {
    userId: string;
    username: string;
    email: string;
    role: 'USER' | 'ADMIN';
}

interface AuthState {
    user: UserInfo | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;

    login: (email: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
    initialize: () => Promise<void>;
    clearError: () => void;
}

export const useAuthStore = create<AuthState>((set) => {
    // ── Listen for forced logout from the axios interceptor ──────────────
    const handleForceLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        set({ user: null, isAuthenticated: false, isLoading: false, error: null });
    };
    window.addEventListener('auth:logout', handleForceLogout);

    return {
        user: null,
        isAuthenticated: false,
        isLoading: true,
        error: null,

        login: async (email: string, password: string) => {
            set({ isLoading: true, error: null });
            try {
                const { data } = await api.post('/auth/login', { email, password });

                localStorage.setItem('token', data.token);
                localStorage.setItem('refreshToken', data.refreshToken);

                // Fetch full user info (includes role)
                const { data: userInfo } = await api.get('/auth/me');

                set({
                    user: userInfo,
                    isAuthenticated: true,
                    isLoading: false,
                    error: null,
                });
            } catch (err: unknown) {
                const message =
                    (err as { response?: { data?: { message?: string } } })?.response?.data
                        ?.message || 'Invalid email or password';
                set({ isLoading: false, error: message });
                throw err;
            }
        },

        logout: async () => {
            try {
                await api.post('/auth/logout');
            } catch {
                // Logout endpoint might fail if token is already expired — ignore
            } finally {
                localStorage.removeItem('token');
                localStorage.removeItem('refreshToken');
                set({ user: null, isAuthenticated: false, isLoading: false });
            }
        },

        initialize: async () => {
            const token = localStorage.getItem('token');
            if (!token) {
                set({ isLoading: false, isAuthenticated: false });
                return;
            }

            try {
                const { data: userInfo } = await api.get('/auth/me');
                set({
                    user: userInfo,
                    isAuthenticated: true,
                });
            } catch {
                localStorage.removeItem('token');
                localStorage.removeItem('refreshToken');
                set({ user: null, isAuthenticated: false });
            } finally {
                set({ isLoading: false });
            }
        },

        clearError: () => set({ error: null }),
    };
});
