import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { useAuthStore } from '../stores/authStore';
import {
    Server,
    WifiOff,
    Wifi,
    AlertTriangle,
    Bell,
    Loader2,
    ChevronRight,
    Activity,
    Database,
    HardDrive,
    Mail,
    Cog,
} from 'lucide-react';
import { getDevices, type DeviceResponse } from '../api/deviceService';
import { getAlerts, type AlertResponseModel } from '../api/alertService';
import { getSettings } from '../api/settingsService';
import { useWebSocketStore } from '../stores/websocketStore';

/* ── Helpers ──────────────────────────────────────────────────────────────── */
const METRIC_LABELS: Record<string, string> = {
    CPU_USAGE: 'CPU Usage',
    CPU_TEMP: 'CPU Temp',
    CPU_FREQ: 'CPU Freq',
    RAM_USAGE: 'RAM Usage',
    DISK_USAGE: 'Disk Usage',
    NETWORK_IN: 'Net In',
    NETWORK_OUT: 'Net Out',
    UPTIME: 'Uptime',
    PROCESS_COUNT: 'Processes',
    TCP_CONNECTIONS_COUNT: 'TCP Conns',
    LISTENING_PORTS_COUNT: 'Ports',
    DEVICE_OFFLINE: 'Device Offline',
};

function timeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
}

/* ── Component ────────────────────────────────────────────────────────────── */
export default function Dashboard() {
    const user = useAuthStore((s) => s.user);
    const lastAlert = useWebSocketStore((s) => s.lastAlert);

    const [totalDevices, setTotalDevices] = useState<number | null>(null);
    const [criticalAlerts, setCriticalAlerts] = useState<number | null>(null);
    const [recentAlerts, setRecentAlerts] = useState<AlertResponseModel[]>([]);
    const [loading, setLoading] = useState(true);
    const [deviceList, setDeviceList] = useState<DeviceResponse[]>([]);

    // System Health from /actuator/health
    interface HealthData {
        status: string;
        db: { status: string; database: string };
        jobRunr: string;
        mail: string;
        disk: { status: string; freeGb: number; totalGb: number };
    }
    const [health, setHealth] = useState<HealthData | null>(null);
    const [healthError, setHealthError] = useState(false);

    const toGb = (bytes: number) => Math.round(bytes / (1024 ** 3));

    const fetchHealth = useCallback(async () => {
        try {
            const { data } = await axios.get('/actuator/health');
            const c = data.components ?? {};
            setHealth({
                status: data.status ?? 'UNKNOWN',
                db: {
                    status: c.db?.status ?? 'UNKNOWN',
                    database: c.db?.details?.database ?? 'Database',
                },
                jobRunr: c.JobRunr?.status ?? c.jobRunr?.status ?? 'UNKNOWN',
                mail: c.mail?.status ?? 'UNKNOWN',
                disk: {
                    status: c.diskSpace?.status ?? 'UNKNOWN',
                    freeGb: toGb(c.diskSpace?.details?.free ?? 0),
                    totalGb: toGb(c.diskSpace?.details?.total ?? 0),
                },
            });
            setHealthError(false);
        } catch {
            setHealthError(true);
            setHealth(null);
        }
    }, []);

    // Fetch health on mount + poll every 60s (independent from main data)
    useEffect(() => {
        fetchHealth();
        const interval = setInterval(fetchHealth, 60_000);
        return () => clearInterval(interval);
    }, [fetchHealth]);

    // Tick counter: incremented every 10s so isOnline() recalculates
    const [tick, setTick] = useState(0);
    useEffect(() => {
        const interval = setInterval(() => setTick((t) => t + 1), 10_000);
        return () => clearInterval(interval);
    }, []);

    const [offlineThreshold, setOfflineThreshold] = useState(300);
    useEffect(() => {
        getSettings()
            .then((s) => setOfflineThreshold(s.deviceOfflineThresholdSeconds))
            .catch(() => { /* keep default */ });
    }, []);

    const isOnline = (lastSeen?: string) => {
        if (!lastSeen) return false;
        // Server sends UTC without trailing 'Z', ensure JS parses it correctly
        const dateStr = lastSeen.endsWith('Z') ? lastSeen : `${lastSeen}Z`;
        return (Date.now() - new Date(dateStr).getTime()) / 1000 <= offlineThreshold;
    };

    // Recalculate offline count whenever tick, device list, or threshold changes
    const offlineDevices = useMemo(() => {
        if (deviceList.length === 0 && loading) return null;
        return deviceList.filter((d) => !isOnline(d.lastSeen)).length;
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [deviceList, tick, offlineThreshold]);

    const fetchData = useCallback(async () => {
        try {
            const [devicesRes, alertsRes] = await Promise.all([
                getDevices(0, 200),
                getAlerts(0, 5, false),
            ]);

            setTotalDevices(devicesRes.totalElements);
            setDeviceList(devicesRes.content ?? []);

            const critCount = (alertsRes.content ?? []).filter(
                (a) => a.severity === 'CRITICAL' && !a.isResolved,
            ).length;
            setCriticalAlerts(critCount);
            setRecentAlerts(alertsRes.content ?? []);
        } catch {
            // interceptors handle
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    // Auto-refresh when a WS alert comes in
    useEffect(() => {
        if (lastAlert) fetchData();
    }, [lastAlert, fetchData]);

    const cards = [
        {
            label: 'Total Devices',
            value: totalDevices,
            icon: Server,
            color: 'text-blue-400',
            bg: 'bg-blue-500/10',
            border: 'border-blue-500/20',
        },
        {
            label: 'Offline Devices',
            value: offlineDevices,
            icon: WifiOff,
            color: offlineDevices && offlineDevices > 0 ? 'text-red-400' : 'text-emerald-400',
            bg: offlineDevices && offlineDevices > 0 ? 'bg-red-500/10' : 'bg-emerald-500/10',
            border: offlineDevices && offlineDevices > 0 ? 'border-red-500/20' : 'border-emerald-500/20',
            highlight: offlineDevices && offlineDevices > 0,
        },
        {
            label: 'Active Critical Alerts',
            value: criticalAlerts,
            icon: AlertTriangle,
            color: criticalAlerts && criticalAlerts > 0 ? 'text-red-400' : 'text-emerald-400',
            bg: criticalAlerts && criticalAlerts > 0 ? 'bg-red-500/10' : 'bg-emerald-500/10',
            border: criticalAlerts && criticalAlerts > 0 ? 'border-red-500/20' : 'border-emerald-500/20',
            highlight: criticalAlerts && criticalAlerts > 0,
        },
    ];

    return (
        <div className="space-y-8">
            {/* Header */}
            <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-xl bg-zinc-800/60 border border-zinc-700/40 flex items-center justify-center shrink-0">
                    <img src="/favicon.svg" alt="" className="w-7 h-7" />
                </div>
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-100 tracking-tight">
                        Dashboard
                    </h1>
                    <p className="text-sm text-zinc-500 mt-0.5">
                        Welcome back, <span className="text-zinc-300">{user?.username}</span>
                    </p>
                </div>
            </div>

            {/* Stat Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {cards.map((card) => (
                    <div
                        key={card.label}
                        className={`bg-zinc-900/50 border rounded-xl p-5 flex items-start gap-4 hover:border-zinc-700/60 transition-colors ${card.highlight ? card.border : 'border-zinc-800/60'
                            }`}
                    >
                        <div className={`flex items-center justify-center w-10 h-10 rounded-lg ${card.bg}`}>
                            <card.icon className={`w-5 h-5 ${card.color}`} />
                        </div>
                        <div>
                            <p className="text-sm text-zinc-500">{card.label}</p>
                            <p className={`text-2xl font-semibold mt-0.5 ${card.highlight ? card.color : 'text-zinc-100'}`}>
                                {loading ? (
                                    <Loader2 className="w-5 h-5 animate-spin text-zinc-600" />
                                ) : (
                                    card.value ?? '—'
                                )}
                            </p>
                        </div>
                    </div>
                ))}
            </div>

            {/* System Health */}
            <div className="bg-zinc-900/50 border border-zinc-800/60 rounded-xl p-5">
                <div className="flex items-center gap-2 mb-4">
                    <Activity className="w-4 h-4 text-zinc-500" />
                    <h2 className="text-sm font-medium text-zinc-200">System Health</h2>
                </div>
                {healthError ? (
                    <p className="text-sm text-red-400">Unable to reach backend</p>
                ) : !health ? (
                    <div className="flex justify-center py-4"><Loader2 className="w-4 h-4 text-zinc-600 animate-spin" /></div>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-3">
                        {/* API */}
                        {(() => {
                            const up = health.status === 'UP';
                            return (
                                <div className="flex items-center gap-3">
                                    <div className={`w-2 h-2 rounded-full shrink-0 ${up ? 'bg-emerald-400' : 'bg-red-400'}`} />
                                    <Activity className="w-3.5 h-3.5 text-zinc-500" />
                                    <span className={`text-sm ${up ? 'text-zinc-200' : 'text-red-400'}`}>API</span>
                                </div>
                            );
                        })()}
                        {/* Database */}
                        {(() => {
                            const up = health.db.status === 'UP';
                            return (
                                <div className="flex items-center gap-3">
                                    <div className={`w-2 h-2 rounded-full shrink-0 ${up ? 'bg-emerald-400' : 'bg-red-400'}`} />
                                    <Database className="w-3.5 h-3.5 text-zinc-500" />
                                    <span className={`text-sm ${up ? 'text-zinc-200' : 'text-red-400'}`}>{health.db.database}</span>
                                </div>
                            );
                        })()}
                        {/* JobRunr */}
                        {(() => {
                            const up = health.jobRunr === 'UP';
                            return (
                                <div className="flex items-center gap-3">
                                    <div className={`w-2 h-2 rounded-full shrink-0 ${up ? 'bg-emerald-400' : 'bg-red-400'}`} />
                                    <Cog className="w-3.5 h-3.5 text-zinc-500" />
                                    <span className={`text-sm ${up ? 'text-zinc-200' : 'text-red-400'}`}>JobRunr</span>
                                </div>
                            );
                        })()}
                        {/* Mail */}
                        {(() => {
                            const up = health.mail === 'UP';
                            return (
                                <div className="flex items-center gap-3">
                                    <div className={`w-2 h-2 rounded-full shrink-0 ${up ? 'bg-emerald-400' : 'bg-red-400'}`} />
                                    <Mail className="w-3.5 h-3.5 text-zinc-500" />
                                    <span className={`text-sm ${up ? 'text-zinc-200' : 'text-red-400'}`}>Mail Server</span>
                                </div>
                            );
                        })()}
                        {/* Disk Space */}
                        {(() => {
                            const up = health.disk.status === 'UP';
                            const pct = health.disk.totalGb > 0 ? ((health.disk.totalGb - health.disk.freeGb) / health.disk.totalGb) * 100 : 0;
                            return (
                                <div className="flex items-center gap-3">
                                    <div className={`w-2 h-2 rounded-full shrink-0 ${up ? 'bg-emerald-400' : 'bg-red-400'}`} />
                                    <HardDrive className="w-3.5 h-3.5 text-zinc-500" />
                                    <span className={`text-sm ${up ? 'text-zinc-200' : 'text-red-400'}`}>Disk</span>
                                    <div className="ml-auto flex items-center gap-2">
                                        <div className="w-16 h-1 rounded-full bg-zinc-800 overflow-hidden">
                                            <div className={`h-full rounded-full ${pct > 90 ? 'bg-red-400/70' : pct > 70 ? 'bg-amber-400/50' : 'bg-zinc-600'}`} style={{ width: `${pct}%` }} />
                                        </div>
                                        <span className="text-[11px] text-zinc-600 whitespace-nowrap">{health.disk.totalGb - health.disk.freeGb} / {health.disk.totalGb} GB</span>
                                    </div>
                                </div>
                            );
                        })()}
                    </div>
                )}
            </div>

            {/* Recent Alerts */}
            <div className="bg-zinc-900/40 border border-zinc-800/60 rounded-xl">
                <div className="flex items-center justify-between px-5 py-4 border-b border-zinc-800/60">
                    <div className="flex items-center gap-2">
                        <Bell className="w-4 h-4 text-zinc-500" />
                        <h2 className="text-sm font-medium text-zinc-200">Recent Active Alerts</h2>
                    </div>
                    <Link to="/alerts" className="flex items-center gap-1 text-xs text-zinc-500 hover:text-zinc-300 transition-colors">
                        View all <ChevronRight className="w-3.5 h-3.5" />
                    </Link>
                </div>

                {loading ? (
                    <div className="flex justify-center py-12">
                        <Loader2 className="w-5 h-5 text-zinc-600 animate-spin" />
                    </div>
                ) : recentAlerts.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-12 gap-3">
                        <div className="w-12 h-12 rounded-xl bg-zinc-800/60 flex items-center justify-center">
                            <Bell className="w-6 h-6 text-zinc-600" />
                        </div>
                        <p className="text-sm text-zinc-600">No active alerts — all clear</p>
                    </div>
                ) : (
                    <div>
                        {recentAlerts.map((alert) => (
                            <div
                                key={alert.id}
                                className="grid grid-cols-[1fr_120px_100px_80px] gap-4 px-5 py-3 border-b border-zinc-800/30 last:border-b-0 items-center hover:bg-zinc-800/20 transition-colors"
                            >
                                <div className="flex items-center gap-3 min-w-0">
                                    <div className={`w-2 h-2 rounded-full shrink-0 ${alert.severity === 'CRITICAL' ? 'bg-red-400' : 'bg-amber-400'
                                        }`} />
                                    <span className="text-sm text-zinc-200 truncate">{alert.deviceName}</span>
                                </div>
                                <span className="text-sm text-zinc-400">{METRIC_LABELS[alert.metricType] ?? alert.metricType}</span>
                                <span className={`text-xs font-medium ${alert.severity === 'CRITICAL' ? 'text-red-400' : 'text-amber-400'
                                    }`}>
                                    {alert.severity}
                                </span>
                                <span className="text-xs text-zinc-600 text-right">{timeAgo(alert.createdAt)}</span>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Device Status */}
            <div className="bg-zinc-900/40 border border-zinc-800/60 rounded-xl">
                <div className="flex items-center justify-between px-5 py-4 border-b border-zinc-800/60">
                    <div className="flex items-center gap-2">
                        <Server className="w-4 h-4 text-zinc-500" />
                        <h2 className="text-sm font-medium text-zinc-200">Device Status</h2>
                    </div>
                    <Link to="/devices" className="flex items-center gap-1 text-xs text-zinc-500 hover:text-zinc-300 transition-colors">
                        View all <ChevronRight className="w-3.5 h-3.5" />
                    </Link>
                </div>

                {loading ? (
                    <div className="flex justify-center py-12">
                        <Loader2 className="w-5 h-5 text-zinc-600 animate-spin" />
                    </div>
                ) : deviceList.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-12 gap-3">
                        <div className="w-12 h-12 rounded-xl bg-zinc-800/60 flex items-center justify-center">
                            <Server className="w-6 h-6 text-zinc-600" />
                        </div>
                        <p className="text-sm text-zinc-600">No devices registered yet</p>
                    </div>
                ) : (
                    <div>
                        {deviceList.slice(0, 5).map((device) => {
                            const online = isOnline(device.lastSeen);
                            return (
                                <Link
                                    key={device.id}
                                    to={`/devices/${device.id}`}
                                    className="grid grid-cols-[1fr_100px_100px] sm:grid-cols-[1fr_140px_100px_100px] gap-4 px-5 py-3 border-b border-zinc-800/30 last:border-b-0 items-center hover:bg-zinc-800/20 transition-colors"
                                >
                                    <div className="flex items-center gap-3 min-w-0">
                                        <div className="w-7 h-7 rounded-lg bg-zinc-800/80 border border-zinc-700/40 flex items-center justify-center shrink-0">
                                            <Server className="w-3.5 h-3.5 text-zinc-400" />
                                        </div>
                                        <span className="text-sm text-zinc-200 truncate">{device.name}</span>
                                    </div>
                                    <span className="hidden sm:block text-sm text-zinc-500 truncate">{device.operatingSystem}</span>
                                    <div className="flex items-center gap-1.5">
                                        {online ? (
                                            <>
                                                <Wifi className="w-3.5 h-3.5 text-emerald-400" />
                                                <span className="text-xs font-medium text-emerald-400">Online</span>
                                            </>
                                        ) : (
                                            <>
                                                <WifiOff className="w-3.5 h-3.5 text-zinc-600" />
                                                <span className="text-xs font-medium text-zinc-600">Offline</span>
                                            </>
                                        )}
                                    </div>
                                    <span className="text-xs text-zinc-600 text-right">{device.lastSeen ? timeAgo(device.lastSeen) : '—'}</span>
                                </Link>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}
