import { create } from 'zustand';
import { Client } from '@stomp/stompjs';
import type { AlertResponseModel } from '../api/alertService';
import { toast } from 'sonner';

const METRIC_LABELS: Record<string, string> = {
    CPU_USAGE: 'CPU Usage',
    CPU_TEMP: 'CPU Temp',
    CPU_FREQ: 'CPU Freq',
    RAM_USAGE: 'RAM Usage',
    DISK_USAGE: 'Disk Usage',
    NETWORK_IN: 'Network In',
    NETWORK_OUT: 'Network Out',
    UPTIME: 'Uptime',
    PROCESS_COUNT: 'Process Count',
    TCP_CONNECTIONS_COUNT: 'TCP Connections',
    LISTENING_PORTS_COUNT: 'Listening Ports',
};

interface WebSocketState {
    client: Client | null;
    connected: boolean;
    /** Latest alert from WS — consumed by listeners (pages etc.) */
    lastAlert: AlertResponseModel | null;
    lastAlertTimestamp: number;
    connect: () => void;
    disconnect: () => void;
}

export const useWebSocketStore = create<WebSocketState>((set, get) => ({
    client: null,
    connected: false,
    lastAlert: null,
    lastAlertTimestamp: 0,

    connect: () => {
        // Avoid duplicate connections
        if (get().client?.active) return;

        const isDev = import.meta.env.DEV;
        const wsUrl = isDev
            ? `ws://${window.location.hostname}:8080/ws`
            : `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`;

        const client = new Client({
            brokerURL: wsUrl,
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,

            // Runs before EVERY connect & auto-reconnect — always injects the freshest token
            beforeConnect: () => {
                const freshToken = localStorage.getItem('token');
                client.connectHeaders = freshToken
                    ? { Authorization: `Bearer ${freshToken}` }
                    : {};
            },

            onConnect: () => {
                set({ connected: true });

                // ── New alert ──────────────────────────────────────────────
                client.subscribe('/topic/alerts', (message) => {
                    try {
                        const alert: AlertResponseModel = JSON.parse(message.body);
                        set({
                            lastAlert: alert,
                            lastAlertTimestamp: Date.now(),
                        });
                    } catch {
                        // ignore malformed messages
                    }
                });

                // ── Resolved alert ─────────────────────────────────────────
                client.subscribe('/topic/alerts/resolved', (message) => {
                    try {
                        const alert: AlertResponseModel = JSON.parse(message.body);
                        toast.success(`Alert resolved: ${alert.deviceName}`, {
                            description:
                                METRIC_LABELS[alert.metricType] ?? alert.metricType,
                        });
                    } catch {
                        // ignore
                    }
                });
            },

            onDisconnect: () => {
                set({ connected: false });
            },

            onStompError: (frame) => {
                console.error('STOMP error:', frame.headers['message']);
                set({ connected: false });
            },

            onWebSocketError: () => {
                set({ connected: false });
            },
        });

        client.activate();
        set({ client });
    },

    disconnect: () => {
        const { client } = get();
        if (client?.active) {
            client.deactivate();
        }
        set({ client: null, connected: false });
    },
}));
