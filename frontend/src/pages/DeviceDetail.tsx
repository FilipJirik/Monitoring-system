import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
    ArrowLeft,
    Cpu,
    Gauge,
    MemoryStick,
    HardDrive,
    Thermometer,
    Clock,
    Wifi,
    WifiOff,
    Loader2,
    Activity,
    Shield,
    AlertTriangle,
    Bell,
    ListTree,
    Network,
    Radio,
} from 'lucide-react';
import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
} from 'recharts';
import {
    getLatestMetrics,
    getMetricsHistory,
    type MetricsStatusModel,
    type MetricsHistoryModel,
    type MetricType,
    type Period,
} from '../api/metricsService';
import { getDevice, type DeviceResponse } from '../api/deviceService';
import { useWebSocketStore } from '../stores/websocketStore';
import { useAuthStore } from '../stores/authStore';
import { toast } from 'sonner';
import DeviceMap from '../components/devices/DeviceMap';
import DeviceImage from '../components/devices/DeviceImage';
import DeviceInfoPanel from '../components/devices/DeviceInfoPanel';
import ThresholdsPanel from '../components/devices/ThresholdsPanel';
import AccessControlPanel from '../components/devices/AccessControlPanel';
import RecipientsPanel from '../components/devices/RecipientsPanel';

/* ── Helpers ────────────────────────────────────────────────────────────── */
const formatUptime = (s?: number) => {
    if (!s) return '—';
    const d = Math.floor(s / 86400);
    const h = Math.floor((s % 86400) / 3600);
    const m = Math.floor((s % 3600) / 60);
    if (d > 0) return `${d}d ${h}h`;
    if (h > 0) return `${h}h ${m}m`;
    return `${m}m`;
};

const CHART_METRICS: {
    type: MetricType;
    label: string;
    color: string;
    unit: string;
}[] = [
        { type: 'CPU_USAGE', label: 'CPU Usage', color: '#22d3ee', unit: '%' },
        { type: 'CPU_TEMP', label: 'CPU Temp', color: '#f43f5e', unit: '°C' },
        { type: 'CPU_FREQ', label: 'CPU Frequency', color: '#818cf8', unit: 'MHz' },
        { type: 'RAM_USAGE', label: 'RAM Usage', color: '#f472b6', unit: 'MB' },
        { type: 'DISK_USAGE', label: 'Disk Usage', color: '#f97316', unit: '%' },
        { type: 'NETWORK_IN', label: 'Network In', color: '#34d399', unit: 'Kbps' },
        { type: 'NETWORK_OUT', label: 'Network Out', color: '#fbbf24', unit: 'Kbps' },
    ];

const PERIODS: { value: Period; label: string }[] = [
    { value: 'HOUR_1', label: '1H' },
    { value: 'HOUR_6', label: '6H' },
    { value: 'HOUR_12', label: '12H' },
    { value: 'HOUR_24', label: '24H' },
    { value: 'DAY_3', label: '3D' },
    { value: 'DAY_7', label: '7D' },
    { value: 'DAY_30', label: '30D' },
];

/**
 * Map WS payload field-names → MetricType for chart data points.
 * Exact backend WS fields: cpuUsagePercent, cpuTempCelsius, cpuFreqAvgMhz,
 * ramUsageMb, diskUsagePercent, networkInKbps, networkOutKbps, uptimeSeconds.
 * We also keep MetricsStatusModel-style keys as fallback.
 */
const FIELD_TO_METRIC: [string, MetricType][] = [
    // CPU usage
    ['cpuUsagePercent', 'CPU_USAGE'],
    ['currentCpuUsage', 'CPU_USAGE'],
    // CPU temp
    ['cpuTempCelsius', 'CPU_TEMP'],
    ['currentCpuTemp', 'CPU_TEMP'],
    // CPU freq
    ['cpuFreqAvgMhz', 'CPU_FREQ'],
    ['currentCpuFreqMhz', 'CPU_FREQ'],
    // RAM usage (backend sends MB)
    ['ramUsageMb', 'RAM_USAGE'],
    ['currentRamUsage', 'RAM_USAGE'],
    // Disk usage
    ['diskUsagePercent', 'DISK_USAGE'],
    ['currentDiskUsage', 'DISK_USAGE'],
    // Network
    ['networkInKbps', 'NETWORK_IN'],
    ['currentNetworkInKbps', 'NETWORK_IN'],
    ['networkOutKbps', 'NETWORK_OUT'],
    ['currentNetworkOutKbps', 'NETWORK_OUT'],
    // Process / connection counters
    ['processCount', 'PROCESS_COUNT'],
    ['tcpConnectionsCount', 'TCP_CONNECTIONS_COUNT'],
    ['listeningPortsCount', 'LISTENING_PORTS_COUNT'],
];

/** Map WS payload fields → MetricsStatusModel keys for stat-card updates */
const FIELD_TO_STATUS: Record<string, keyof MetricsStatusModel> = {
    cpuUsagePercent: 'currentCpuUsage',
    cpuTempCelsius: 'currentCpuTemp',
    cpuFreqAvgMhz: 'currentCpuFreqMhz',
    ramUsageMb: 'currentRamUsage',
    diskUsagePercent: 'currentDiskUsage',
    uptimeSeconds: 'uptimeSeconds',
    processCount: 'currentProcessCount',
    tcpConnectionsCount: 'currentTcpConnectionsCount',
    listeningPortsCount: 'currentListeningPortsCount',
};

/** Periods where raw real-time WS data should be appended to charts */
const LIVE_APPEND_PERIODS: Set<Period> = new Set(['HOUR_1', 'HOUR_6', 'HOUR_12', 'HOUR_24']);

/* ── Custom Tooltip ─────────────────────────────────────────────────────── */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function ChartTooltip({ active, payload, label, unit }: any) {
    if (!active || !payload?.[0]) return null;
    return (
        <div className="bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 shadow-lg shadow-black/30 text-xs">
            <p className="text-zinc-500 mb-0.5">{new Date(label).toLocaleString()}</p>
            <p className="text-zinc-100 font-medium">
                {payload[0].value?.toFixed(1)} {unit}
            </p>
        </div>
    );
}

/** Smart X-axis tick formatter — shows date for multi-day periods, time-only for short */
function formatTick(ts: number, period: Period): string {
    const d = new Date(ts);
    const hh = d.getHours().toString().padStart(2, '0');
    const mm = d.getMinutes().toString().padStart(2, '0');
    if (period === 'DAY_3' || period === 'DAY_7' || period === 'DAY_30') {
        return `${d.getMonth() + 1}/${d.getDate()} ${hh}:${mm}`;
    }
    return `${hh}:${mm}`;
}

/* ── MetricChart Component ──────────────────────────────────────────────── */

/** A data point pushed via WebSocket for a specific metric type */
export interface LiveDataPoint {
    type: MetricType;
    timestamp: string;
    value: number;
}

/** Internal chart data shape — uses numeric Unix ms for proper time-scale */
interface ChartPoint {
    ts: number;
    value: number;
}

function MetricChart({
    deviceId,
    metric,
    period,
    livePoint,
}: {
    deviceId: string;
    metric: (typeof CHART_METRICS)[number];
    period: Period;
    /** Latest real-time point from WS — appended to the chart */
    livePoint: LiveDataPoint | null;
}) {
    const [history, setHistory] = useState<MetricsHistoryModel | null>(null);
    const [chartData, setChartData] = useState<ChartPoint[]>([]);
    const [loading, setLoading] = useState(true);

    // Keep a counter to avoid duplicate live points
    const lastLiveTs = useRef<string | null>(null);

    // Fetch initial history
    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        getMetricsHistory(deviceId, metric.type, period)
            .then((d) => {
                if (!cancelled) {
                    setHistory(d);
                    setChartData(
                        d.data?.map((p) => ({ ts: new Date(p.timestamp).getTime(), value: p.value })) ?? [],
                    );
                }
            })
            .catch(() => { if (!cancelled) { setHistory(null); setChartData([]); } })
            .finally(() => { if (!cancelled) setLoading(false); });
        return () => { cancelled = true; };
    }, [deviceId, metric.type, period]);

    // Append live data point — only for short-term (raw) periods
    useEffect(() => {
        if (!livePoint || livePoint.type !== metric.type) return;
        if (!LIVE_APPEND_PERIODS.has(period)) return; // skip aggregated periods
        if (livePoint.timestamp === lastLiveTs.current) return;
        lastLiveTs.current = livePoint.timestamp;
        setChartData((prev) => [
            ...prev,
            { ts: new Date(livePoint.timestamp).getTime(), value: livePoint.value },
        ]);
    }, [livePoint, metric.type, period]);

    return (
        <div className="bg-zinc-900/40 border border-zinc-800/40 rounded-xl p-5">
            <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                    <div className="w-2 h-2 rounded-full" style={{ backgroundColor: metric.color }} />
                    <span className="text-sm font-medium text-zinc-300">{metric.label}</span>
                </div>
                <span className="text-xs text-zinc-600">{history?.unit ?? metric.unit}</span>
            </div>
            {loading ? (
                <div className="flex items-center justify-center h-48">
                    <Loader2 className="w-5 h-5 text-zinc-700 animate-spin" />
                </div>
            ) : chartData.length === 0 ? (
                <div className="flex items-center justify-center h-48">
                    <p className="text-xs text-zinc-700">No data available</p>
                </div>
            ) : (
                <ResponsiveContainer width="100%" height={200}>
                    <LineChart data={chartData} margin={{ top: 4, right: 8, bottom: 0, left: -16 }}>
                        <CartesianGrid stroke="#27272a" strokeDasharray="3 3" vertical={false} />
                        <XAxis
                            dataKey="ts"
                            type="number"
                            scale="time"
                            domain={['dataMin', 'dataMax']}
                            tick={{ fill: '#52525b', fontSize: 10 }}
                            tickLine={false}
                            axisLine={{ stroke: '#27272a' }}
                            tickFormatter={(v: number) => formatTick(v, period)}
                            minTickGap={40}
                        />
                        <YAxis
                            tick={{ fill: '#52525b', fontSize: 10 }}
                            tickLine={false}
                            axisLine={false}
                            width={52}
                        />
                        <Tooltip content={<ChartTooltip unit={history?.unit ?? metric.unit} />} />
                        <Line
                            type="monotone"
                            dataKey="value"
                            stroke={metric.color}
                            strokeWidth={1.5}
                            dot={false}
                            activeDot={{ r: 3, fill: metric.color, stroke: '#09090b', strokeWidth: 2 }}
                            isAnimationActive={false}
                            connectNulls={false}
                        />
                    </LineChart>
                </ResponsiveContainer>
            )}
        </div>
    );
}

/* ── DeviceDetail Page ──────────────────────────────────────────────────── */
export default function DeviceDetail() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [device, setDevice] = useState<DeviceResponse | null>(null);
    const [metrics, setMetrics] = useState<MetricsStatusModel | null>(null);
    const [loadingDevice, setLoadingDevice] = useState(true);
    const [loadingMetrics, setLoadingMetrics] = useState(true);
    const [period, setPeriod] = useState<Period>('HOUR_24');
    const [activeTab, setActiveTab] = useState<'metrics' | 'thresholds' | 'access' | 'recipients'>('metrics');

    // Live data point state — one per metric type, pushed from the WS subscription
    const [livePoints, setLivePoints] = useState<Record<string, LiveDataPoint>>({});

    // Smart Live indicator — tracks whether WS is actively delivering data
    const [isReceivingData, setIsReceivingData] = useState(false);
    const lastWsRef = useRef<number>(0);

    // WebSocket
    const client = useWebSocketStore((s) => s.client);
    const connected = useWebSocketStore((s) => s.connected);

    // Auth (for role-based tab visibility)
    const user = useAuthStore((s) => s.user);

    // 15-second staleness check for the Live indicator
    useEffect(() => {
        const interval = setInterval(() => {
            if (lastWsRef.current > 0 && Date.now() - lastWsRef.current > 15_000) {
                setIsReceivingData(false);
            }
        }, 5_000);
        return () => clearInterval(interval);
    }, []);

    const fetchData = useCallback(async () => {
        if (!id) return;
        setLoadingDevice(true);
        setLoadingMetrics(true);
        try {
            const [dev, met] = await Promise.all([
                getDevice(id),
                getLatestMetrics(id),
            ]);
            setDevice(dev);
            setMetrics(met);
        } catch {
            toast.error('Failed to load device');
            navigate('/devices', { replace: true });
        } finally {
            setLoadingDevice(false);
            setLoadingMetrics(false);
        }
    }, [id, navigate]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    // ── STOMP subscription for live metrics ──────────────────────────────
    useEffect(() => {
        if (!client || !connected || !id) return;

        const subscription = client.subscribe(`/topic/devices/${id}/metrics`, (message) => {
            try {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                const raw: Record<string, any> = JSON.parse(message.body);
                console.log('[DeviceDetail] WS raw payload:', raw);
                lastWsRef.current = Date.now();
                setIsReceivingData(true);

                // ── 1. Update stat-card values ────────────────────────────
                // Build a normalised MetricsStatusModel-compatible object
                const statusPatch: Partial<MetricsStatusModel> = { online: true };

                // Direct matches (backend may already use MetricsStatusModel keys)
                if (typeof raw.currentCpuUsage === 'number') statusPatch.currentCpuUsage = raw.currentCpuUsage;
                if (typeof raw.currentCpuTemp === 'number') statusPatch.currentCpuTemp = raw.currentCpuTemp;
                if (typeof raw.currentCpuFreqMhz === 'number') statusPatch.currentCpuFreqMhz = raw.currentCpuFreqMhz;
                if (typeof raw.currentRamUsage === 'number') statusPatch.currentRamUsage = raw.currentRamUsage;
                if (typeof raw.currentDiskUsage === 'number') statusPatch.currentDiskUsage = raw.currentDiskUsage;
                if (typeof raw.uptimeSeconds === 'number') statusPatch.uptimeSeconds = raw.uptimeSeconds;

                // Alternative field-name matches (normalise to MetricsStatusModel keys)
                for (const [field, statusKey] of Object.entries(FIELD_TO_STATUS)) {
                    if (typeof raw[field] === 'number' && statusPatch[statusKey] === undefined) {
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        (statusPatch as any)[statusKey] = raw[field];
                    }
                }

                console.log('[DeviceDetail] WS stat-card patch:', statusPatch);
                setMetrics((prev) => ({ ...prev, ...statusPatch }));

                // ── 2. Build live chart data points ───────────────────────
                const now = new Date().toISOString();
                const newPoints: Record<string, LiveDataPoint> = {};
                const seen = new Set<MetricType>();

                for (const [field, metricType] of FIELD_TO_METRIC) {
                    if (seen.has(metricType)) continue;       // first match wins
                    const val = raw[field];
                    if (typeof val === 'number') {
                        newPoints[metricType] = { type: metricType, timestamp: now, value: val };
                        seen.add(metricType);
                        console.log(`[DeviceDetail] WS metric mapped: ${field} → ${metricType} = ${val}`);
                    }
                }

                if (Object.keys(newPoints).length > 0) {
                    console.log('[DeviceDetail] WS live points update:', newPoints);
                    setLivePoints((prev) => ({ ...prev, ...newPoints }));
                } else {
                    console.warn('[DeviceDetail] WS message received but NO numeric fields matched any mapping. Raw keys:', Object.keys(raw));
                }
            } catch (err) {
                console.error('[DeviceDetail] WS parse error:', err);
            }
        });

        return () => {
            subscription.unsubscribe();
        };
    }, [client, connected, id]);

    if (loadingDevice) {
        return (
            <div className="flex items-center justify-center py-24">
                <Loader2 className="w-6 h-6 text-zinc-600 animate-spin" />
            </div>
        );
    }

    if (!device) return null;

    const statCards: {
        label: string;
        value: string;
        icon: typeof Cpu;
        color: string;
    }[] = [
            {
                label: 'CPU Usage',
                value: metrics?.currentCpuUsage != null ? `${metrics.currentCpuUsage.toFixed(1)}%` : '—',
                icon: Cpu,
                color: 'text-cyan-400',
            },
            {
                label: 'CPU Temp',
                value: metrics?.currentCpuTemp != null ? `${metrics.currentCpuTemp.toFixed(1)}°C` : '—',
                icon: Thermometer,
                color: 'text-rose-400',
            },
            {
                label: 'CPU Freq',
                value: metrics?.currentCpuFreqMhz != null ? `${metrics.currentCpuFreqMhz.toFixed(0)} MHz` : '—',
                icon: Gauge,
                color: 'text-indigo-400',
            },
            {
                label: 'RAM Usage',
                value: metrics?.currentRamUsage != null ? `${metrics.currentRamUsage.toFixed(0)} MB` : '—',
                icon: MemoryStick,
                color: 'text-pink-400',
            },
            {
                label: 'Disk Usage',
                value: metrics?.currentDiskUsage != null ? `${metrics.currentDiskUsage.toFixed(1)}%` : '—',
                icon: HardDrive,
                color: 'text-orange-400',
            },
            {
                label: 'Uptime',
                value: formatUptime(metrics?.uptimeSeconds),
                icon: Clock,
                color: 'text-emerald-400',
            },
            {
                label: 'Processes',
                value: metrics?.currentProcessCount != null ? `${metrics.currentProcessCount}` : '—',
                icon: ListTree,
                color: 'text-teal-400',
            },
            {
                label: 'TCP Conns',
                value: metrics?.currentTcpConnectionsCount != null ? `${metrics.currentTcpConnectionsCount}` : '—',
                icon: Network,
                color: 'text-sky-400',
            },
            {
                label: 'Listening Ports',
                value: metrics?.currentListeningPortsCount != null ? `${metrics.currentListeningPortsCount}` : '—',
                icon: Radio,
                color: 'text-amber-400',
            },
            {
                label: 'Status',
                value: metrics?.online ? 'Online' : 'Offline',
                icon: metrics?.online ? Wifi : WifiOff,
                color: metrics?.online ? 'text-emerald-400' : 'text-zinc-600',
            },
        ];

    const ALL_TABS = [
        { key: 'metrics' as const, label: 'Metrics', icon: Activity },
        { key: 'thresholds' as const, label: 'Thresholds', icon: AlertTriangle },
        { key: 'recipients' as const, label: 'Recipients', icon: Bell },
        { key: 'access' as const, label: 'Access Control', icon: Shield },
    ];

    const TABS = user?.role === 'ADMIN'
        ? ALL_TABS
        : ALL_TABS.filter((t) => t.key !== 'access');

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center gap-4">
                <Link
                    to="/devices"
                    className="p-2 rounded-lg text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800 transition-colors cursor-pointer"
                >
                    <ArrowLeft className="w-5 h-5" />
                </Link>
                <div className="flex-1 min-w-0">
                    <h1 className="text-2xl font-semibold text-zinc-100 tracking-tight truncate">
                        {device.name}
                    </h1>
                    <div className="flex items-center gap-3 mt-0.5">
                        <span className="text-sm text-zinc-500 font-mono">{device.ipAddress}</span>
                        <span className="text-zinc-700">·</span>
                        <span className="text-sm text-zinc-500">{device.operatingSystem}</span>
                        {connected && (
                            <>
                                <span className="text-zinc-700">·</span>
                                <span className={`flex items-center gap-1.5 text-xs ${isReceivingData ? 'text-emerald-500' : 'text-zinc-600'}`}>
                                    <span className={`w-1.5 h-1.5 rounded-full ${isReceivingData ? 'bg-emerald-500 animate-pulse' : 'bg-zinc-600'}`} />
                                    {isReceivingData ? 'Live' : 'Idle'}
                                </span>
                            </>
                        )}
                    </div>
                </div>
            </div>

            {/* ── Sidebar + Main grid ─────────────────────────────── */}
            <div className="flex flex-col lg:flex-row gap-6">
                {/* Sidebar */}
                <div className="w-full lg:w-80 shrink-0 space-y-4">
                    <DeviceImage
                        deviceId={device.id}
                        imageFilename={device.imageFilename}
                        onUploaded={() => fetchData()}
                    />
                    <DeviceInfoPanel device={device} />
                    <DeviceMap latitude={device.latitude} longitude={device.longitude} />
                </div>

                {/* Main content */}
                <div className="flex-1 min-w-0 space-y-5">
                    {/* Stat Cards */}
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                        {loadingMetrics
                            ? Array.from({ length: 10 }).map((_, i) => (
                                <div key={i} className="bg-zinc-900/40 border border-zinc-800/40 rounded-xl p-4 animate-pulse">
                                    <div className="w-8 h-8 bg-zinc-800/60 rounded-lg mb-3" />
                                    <div className="h-3 w-12 bg-zinc-800/60 rounded mb-2" />
                                    <div className="h-5 w-16 bg-zinc-800/60 rounded" />
                                </div>
                            ))
                            : statCards.map((card) => (
                                <div
                                    key={card.label}
                                    className="bg-zinc-900/40 border border-zinc-800/40 rounded-xl p-4 hover:border-zinc-800/80 transition-colors"
                                >
                                    <div className={`w-8 h-8 rounded-lg bg-zinc-800/60 flex items-center justify-center mb-3 ${card.color}`}>
                                        <card.icon className="w-4 h-4" />
                                    </div>
                                    <p className="text-xs text-zinc-500 mb-0.5">{card.label}</p>
                                    <p className="text-lg font-semibold text-zinc-100">{card.value}</p>
                                </div>
                            ))}
                    </div>

                    {/* Tabs */}
                    <div className="flex items-center gap-1 border-b border-zinc-800/60">
                        {TABS.map((tab) => (
                            <button
                                key={tab.key}
                                onClick={() => setActiveTab(tab.key)}
                                className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors cursor-pointer ${activeTab === tab.key
                                    ? 'border-zinc-100 text-zinc-100'
                                    : 'border-transparent text-zinc-500 hover:text-zinc-300'
                                    }`}
                            >
                                <tab.icon className="w-4 h-4" />
                                {tab.label}
                            </button>
                        ))}
                    </div>

                    {/* Tab: Metrics */}
                    {activeTab === 'metrics' && (
                        <div className="space-y-5">
                            {/* Period selector */}
                            <div className="flex items-center gap-2">
                                <Activity className="w-4 h-4 text-zinc-600" />
                                <span className="text-sm text-zinc-500 mr-2">History</span>
                                <div className="flex items-center gap-1 bg-zinc-900/40 rounded-lg p-1 border border-zinc-800/40">
                                    {PERIODS.map((p) => (
                                        <button
                                            key={p.value}
                                            onClick={() => setPeriod(p.value)}
                                            className={`px-2.5 py-1 rounded-md text-xs font-medium transition-colors cursor-pointer ${period === p.value
                                                ? 'bg-zinc-800 text-zinc-100'
                                                : 'text-zinc-500 hover:text-zinc-300'
                                                }`}
                                        >
                                            {p.label}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* Charts */}
                            <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
                                {CHART_METRICS.map((m) => (
                                    <MetricChart
                                        key={m.type}
                                        deviceId={device.id}
                                        metric={m}
                                        period={period}
                                        livePoint={livePoints[m.type] ?? null}
                                    />
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Tab: Thresholds */}
                    {activeTab === 'thresholds' && (
                        <ThresholdsPanel deviceId={device.id} />
                    )}

                    {/* Tab: Access Control */}
                    {/* Tab: Recipients */}
                    {activeTab === 'recipients' && (
                        <RecipientsPanel deviceId={device.id} />
                    )}

                    {/* Tab: Access Control */}
                    {activeTab === 'access' && (
                        <AccessControlPanel deviceId={device.id} />
                    )}
                </div>
            </div>
        </div>
    );
}
