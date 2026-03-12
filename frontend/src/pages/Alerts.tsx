import { useState, useEffect, useCallback, useRef } from 'react';
import {
    Bell,
    ChevronLeft,
    ChevronRight,
    Loader2,
    CheckCircle2,
    AlertTriangle,
    AlertOctagon,
    ChevronDown,
    X,
    Trash2,
} from 'lucide-react';
import {
    getAlerts,
    resolveAlert,
    deleteAlert,
    type AlertResponseModel,
    type SeverityFilter,
} from '../api/alertService';
import type { PageResponse } from '../api/deviceService';
import { useWebSocketStore } from '../stores/websocketStore';
import { useAuthStore } from '../stores/authStore';
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

export default function Alerts() {
    const [page, setPage] = useState(0);
    const [data, setData] = useState<PageResponse<AlertResponseModel> | null>(
        null,
    );
    const [loading, setLoading] = useState(true);
    const [resolvedFilter, setResolvedFilter] = useState<boolean | undefined>(
        undefined,
    );
    const [severityFilter, setSeverityFilter] = useState<SeverityFilter>(
        undefined,
    );
    const [resolvingId, setResolvingId] = useState<string | null>(null);
    const [showResolvedMenu, setShowResolvedMenu] = useState(false);
    const [showSeverityMenu, setShowSeverityMenu] = useState(false);

    const pageSize = 10;

    // Track ref for WS-driven refresh
    const lastAlertRef = useRef<AlertResponseModel | null>(null);
    const lastAlert = useWebSocketStore((s) => s.lastAlert);

    const fetchAlerts = useCallback(async () => {
        setLoading(true);
        try {
            const res = await getAlerts(page, pageSize, resolvedFilter, severityFilter);
            setData(res);
        } catch {
            // handled by interceptors
        } finally {
            setLoading(false);
        }
    }, [page, resolvedFilter, severityFilter]);

    useEffect(() => {
        fetchAlerts();
    }, [fetchAlerts]);

    // React to WebSocket alert updates — just refresh table
    // (toasts are fired globally from the WS store)
    useEffect(() => {
        if (lastAlert && lastAlert !== lastAlertRef.current) {
            lastAlertRef.current = lastAlert;
            fetchAlerts();
        }
    }, [lastAlert, fetchAlerts]);

    const [resolveConfirm, setResolveConfirm] = useState<string | null>(null);
    const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
    const [deletingId, setDeletingId] = useState<string | null>(null);

    const isAdmin = useAuthStore((s) => s.user?.role === 'ADMIN');

    const handleResolve = async () => {
        if (!resolveConfirm) return;
        setResolvingId(resolveConfirm);
        try {
            await resolveAlert(resolveConfirm);
            toast.success('Alert resolved');
            setResolveConfirm(null);
            fetchAlerts();
        } catch {
            toast.error('Failed to resolve alert');
        } finally {
            setResolvingId(null);
        }
    };

    const handleDelete = async () => {
        if (!deleteConfirm) return;
        setDeletingId(deleteConfirm);
        try {
            await deleteAlert(deleteConfirm);
            toast.success('Alert deleted');
            setDeleteConfirm(null);
            // Remove from local state for instant feedback
            setData((prev) =>
                prev
                    ? {
                        ...prev,
                        content: prev.content.filter((a) => a.id !== deleteConfirm),
                        totalElements: prev.totalElements - 1,
                    }
                    : prev,
            );
        } catch {
            toast.error('Failed to delete alert');
        } finally {
            setDeletingId(null);
        }
    };

    const formatDate = (date: string) => {
        const dateStr = date.endsWith('Z') ? date : `${date}Z`;
        return new Intl.DateTimeFormat('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        }).format(new Date(dateStr));
    };

    const alerts = data?.content ?? [];
    const totalPages = data?.totalPages ?? 0;
    const totalElements = data?.totalElements ?? 0;

    const resolvedLabel =
        resolvedFilter === undefined
            ? 'All Status'
            : resolvedFilter
                ? 'Resolved'
                : 'Unresolved';

    const severityLabel = severityFilter ?? 'All Severity';

    return (
        <div className="space-y-6">
            {/* ── Header ─────────────────────────────────────────────────────── */}
            <div>
                <h1 className="text-2xl font-semibold text-zinc-100 tracking-tight">
                    Alerts
                </h1>
                <p className="text-sm text-zinc-500 mt-0.5">
                    {totalElements} alert{totalElements !== 1 ? 's' : ''}
                </p>
            </div>

            {/* ── Filters ────────────────────────────────────────────────────── */}
            <div className="flex items-center gap-3 flex-wrap">
                {/* Resolved filter */}
                <div className="relative">
                    <button
                        onClick={() => {
                            setShowResolvedMenu(!showResolvedMenu);
                            setShowSeverityMenu(false);
                        }}
                        className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-zinc-900/60 border border-zinc-800 text-sm text-zinc-300 hover:border-zinc-700 transition-colors cursor-pointer"
                    >
                        {resolvedLabel}
                        <ChevronDown className="w-3.5 h-3.5 text-zinc-500" />
                    </button>
                    {showResolvedMenu && (
                        <>
                            <div
                                className="fixed inset-0 z-40"
                                onClick={() => setShowResolvedMenu(false)}
                            />
                            <div className="absolute left-0 top-full mt-1 z-50 w-40 bg-zinc-900 border border-zinc-800 rounded-xl shadow-xl shadow-black/30 py-1">
                                {[
                                    { label: 'All Status', value: undefined },
                                    { label: 'Unresolved', value: false },
                                    { label: 'Resolved', value: true },
                                ].map((opt) => (
                                    <button
                                        key={opt.label}
                                        onClick={() => {
                                            setResolvedFilter(opt.value);
                                            setPage(0);
                                            setShowResolvedMenu(false);
                                        }}
                                        className={`w-full px-3 py-2 text-left text-sm transition-colors cursor-pointer ${resolvedFilter === opt.value
                                            ? 'text-zinc-100 bg-zinc-800/60'
                                            : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/40'
                                            }`}
                                    >
                                        {opt.label}
                                    </button>
                                ))}
                            </div>
                        </>
                    )}
                </div>

                {/* Severity filter */}
                <div className="relative">
                    <button
                        onClick={() => {
                            setShowSeverityMenu(!showSeverityMenu);
                            setShowResolvedMenu(false);
                        }}
                        className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-zinc-900/60 border border-zinc-800 text-sm text-zinc-300 hover:border-zinc-700 transition-colors cursor-pointer"
                    >
                        {severityLabel}
                        <ChevronDown className="w-3.5 h-3.5 text-zinc-500" />
                    </button>
                    {showSeverityMenu && (
                        <>
                            <div
                                className="fixed inset-0 z-40"
                                onClick={() => setShowSeverityMenu(false)}
                            />
                            <div className="absolute left-0 top-full mt-1 z-50 w-40 bg-zinc-900 border border-zinc-800 rounded-xl shadow-xl shadow-black/30 py-1">
                                {[
                                    { label: 'All Severity', value: undefined as SeverityFilter },
                                    { label: 'Warning', value: 'WARNING' as SeverityFilter },
                                    { label: 'Critical', value: 'CRITICAL' as SeverityFilter },
                                ].map((opt) => (
                                    <button
                                        key={opt.label}
                                        onClick={() => {
                                            setSeverityFilter(opt.value);
                                            setPage(0);
                                            setShowSeverityMenu(false);
                                        }}
                                        className={`w-full px-3 py-2 text-left text-sm transition-colors cursor-pointer ${severityFilter === opt.value
                                            ? 'text-zinc-100 bg-zinc-800/60'
                                            : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/40'
                                            }`}
                                    >
                                        {opt.label}
                                    </button>
                                ))}
                            </div>
                        </>
                    )}
                </div>

                {/* Active filter badges */}
                {(resolvedFilter !== undefined || severityFilter !== undefined) && (
                    <button
                        onClick={() => {
                            setResolvedFilter(undefined);
                            setSeverityFilter(undefined);
                            setPage(0);
                        }}
                        className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800/40 transition-colors cursor-pointer"
                    >
                        <X className="w-3 h-3" />
                        Clear filters
                    </button>
                )}
            </div>

            {/* ── Table ──────────────────────────────────────────────────────── */}
            <div className="bg-zinc-900/40 border border-zinc-800/60 rounded-xl">
                {/* Head */}
                <div className={`grid gap-4 px-5 py-3 border-b border-zinc-800/60 text-xs font-medium text-zinc-500 uppercase tracking-wider ${isAdmin
                    ? 'grid-cols-[1fr_120px_120px_100px_140px_80px_48px]'
                    : 'grid-cols-[1fr_120px_120px_100px_140px_80px]'
                    }`}>
                    <span>Device</span>
                    <span>Metric</span>
                    <span>Severity</span>
                    <span>Status</span>
                    <span>Created</span>
                    <span />
                    {isAdmin && <span />}
                </div>

                {/* Loading */}
                {loading && (
                    <div className="flex items-center justify-center py-16">
                        <Loader2 className="w-5 h-5 text-zinc-600 animate-spin" />
                    </div>
                )}

                {/* Empty */}
                {!loading && alerts.length === 0 && (
                    <div className="flex flex-col items-center justify-center py-16 gap-3">
                        <div className="w-12 h-12 rounded-xl bg-zinc-800/60 flex items-center justify-center">
                            <Bell className="w-6 h-6 text-zinc-600" />
                        </div>
                        <p className="text-sm text-zinc-600">No alerts found</p>
                    </div>
                )}

                {/* Rows */}
                {!loading &&
                    alerts.map((alert) => (
                        <div
                            key={alert.id}
                            className={`grid gap-4 px-5 py-3.5 border-b border-zinc-800/30 last:border-b-0 items-center hover:bg-zinc-800/20 transition-colors ${isAdmin
                                ? 'grid-cols-[1fr_120px_120px_100px_140px_80px_48px]'
                                : 'grid-cols-[1fr_120px_120px_100px_140px_80px]'
                                }`}
                        >
                            {/* Device name */}
                            <div className="flex items-center gap-3 min-w-0">
                                <div
                                    className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${alert.severity === 'CRITICAL'
                                        ? 'bg-red-500/10'
                                        : 'bg-amber-500/10'
                                        }`}
                                >
                                    {alert.severity === 'CRITICAL' ? (
                                        <AlertOctagon className="w-4 h-4 text-red-400" />
                                    ) : (
                                        <AlertTriangle className="w-4 h-4 text-amber-400" />
                                    )}
                                </div>
                                <span className="text-sm font-medium text-zinc-200 truncate">
                                    {alert.deviceName}
                                </span>
                            </div>

                            {/* Metric */}
                            <span className="text-sm text-zinc-400 truncate">
                                {METRIC_LABELS[alert.metricType] ?? alert.metricType}
                            </span>

                            {/* Severity */}
                            <div>
                                <span
                                    className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium ${alert.severity === 'CRITICAL'
                                        ? 'bg-red-500/10 text-red-400 border border-red-500/20'
                                        : 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                                        }`}
                                >
                                    {alert.severity}
                                </span>
                            </div>

                            {/* Status */}
                            <div>
                                {alert.isResolved ? (
                                    <span className="inline-flex items-center gap-1 text-xs font-medium text-emerald-400">
                                        <CheckCircle2 className="w-3.5 h-3.5" />
                                        Resolved
                                    </span>
                                ) : (
                                    <span className="inline-flex items-center gap-1 text-xs font-medium text-amber-400">
                                        <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
                                        Active
                                    </span>
                                )}
                            </div>

                            {/* Created */}
                            <span className="text-xs text-zinc-600">
                                {formatDate(alert.createdAt)}
                            </span>

                            {/* Action */}
                            <div>
                                {!alert.isResolved && (
                                    <button
                                        onClick={() => setResolveConfirm(alert.id)}
                                        disabled={resolvingId === alert.id}
                                        title="Resolve alert"
                                        className="p-1.5 rounded-lg text-zinc-500 hover:text-emerald-400 hover:bg-emerald-500/10 disabled:opacity-50 transition-colors cursor-pointer"
                                    >
                                        {resolvingId === alert.id ? (
                                            <Loader2 className="w-4 h-4 animate-spin" />
                                        ) : (
                                            <CheckCircle2 className="w-4 h-4" />
                                        )}
                                    </button>
                                )}
                            </div>

                            {/* Delete (admin only) */}
                            {isAdmin && (
                                <div>
                                    <button
                                        onClick={() => setDeleteConfirm(alert.id)}
                                        disabled={deletingId === alert.id}
                                        title="Delete alert"
                                        className="p-1.5 rounded-lg text-zinc-500 hover:text-red-400 hover:bg-red-500/10 disabled:opacity-50 transition-colors cursor-pointer"
                                    >
                                        {deletingId === alert.id ? (
                                            <Loader2 className="w-4 h-4 animate-spin" />
                                        ) : (
                                            <Trash2 className="w-4 h-4" />
                                        )}
                                    </button>
                                </div>
                            )}
                        </div>
                    ))}
            </div>

            {/* ── Pagination ─────────────────────────────────────────────────── */}
            {totalPages > 1 && (
                <div className="flex items-center justify-between">
                    <p className="text-xs text-zinc-600">
                        Page {page + 1} of {totalPages}
                    </p>
                    <div className="flex items-center gap-1">
                        <button
                            onClick={() => setPage((p) => Math.max(0, p - 1))}
                            disabled={page === 0}
                            className="p-2 rounded-lg text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
                        >
                            <ChevronLeft className="w-4 h-4" />
                        </button>
                        {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                            let pageNum: number;
                            if (totalPages <= 5) {
                                pageNum = i;
                            } else if (page < 3) {
                                pageNum = i;
                            } else if (page > totalPages - 4) {
                                pageNum = totalPages - 5 + i;
                            } else {
                                pageNum = page - 2 + i;
                            }
                            return (
                                <button
                                    key={pageNum}
                                    onClick={() => setPage(pageNum)}
                                    className={`w-8 h-8 rounded-lg text-sm font-medium transition-colors cursor-pointer ${pageNum === page
                                        ? 'bg-zinc-800 text-zinc-100'
                                        : 'text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800/60'
                                        }`}
                                >
                                    {pageNum + 1}
                                </button>
                            );
                        })}
                        <button
                            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                            disabled={page >= totalPages - 1}
                            className="p-2 rounded-lg text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
                        >
                            <ChevronRight className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            )}

            {/* Resolve confirmation */}
            {resolveConfirm && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => !resolvingId && setResolveConfirm(null)} />
                    <div className="relative w-full max-w-sm bg-zinc-900 border border-zinc-800 rounded-2xl shadow-2xl shadow-black/40 p-6">
                        <h3 className="text-base font-semibold text-zinc-100 mb-2">Resolve Alert</h3>
                        <p className="text-sm text-zinc-400 mb-5">
                            Are you sure you want to mark this alert as resolved?
                        </p>
                        <div className="flex justify-end gap-2">
                            <button onClick={() => setResolveConfirm(null)} disabled={!!resolvingId} className="px-4 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer">Cancel</button>
                            <button onClick={handleResolve} disabled={!!resolvingId} className="px-4 py-2 rounded-lg bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 text-sm font-medium hover:bg-emerald-500/30 disabled:opacity-50 transition-colors flex items-center gap-2 cursor-pointer">
                                {resolvingId ? <><Loader2 className="w-4 h-4 animate-spin" />Resolving…</> : 'Resolve'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Delete confirmation (admin) */}
            {deleteConfirm && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => !deletingId && setDeleteConfirm(null)} />
                    <div className="relative w-full max-w-sm bg-zinc-900 border border-zinc-800 rounded-2xl shadow-2xl shadow-black/40 p-6">
                        <h3 className="text-base font-semibold text-zinc-100 mb-2">Delete Alert</h3>
                        <p className="text-sm text-zinc-400 mb-5">
                            Are you sure you want to permanently delete this alert? This action cannot be undone.
                        </p>
                        <div className="flex justify-end gap-2">
                            <button onClick={() => setDeleteConfirm(null)} disabled={!!deletingId} className="px-4 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer">Cancel</button>
                            <button onClick={handleDelete} disabled={!!deletingId} className="px-4 py-2 rounded-lg bg-red-500/20 border border-red-500/30 text-red-400 text-sm font-medium hover:bg-red-500/30 disabled:opacity-50 transition-colors flex items-center gap-2 cursor-pointer">
                                {deletingId ? <><Loader2 className="w-4 h-4 animate-spin" />Deleting…</> : 'Delete'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
