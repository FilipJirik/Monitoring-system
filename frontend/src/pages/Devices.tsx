import { useState, useEffect, useCallback, useRef } from 'react';
import { createPortal } from 'react-dom';
import { Link, useNavigate } from 'react-router-dom';
import {
    Search,
    Plus,
    ChevronLeft,
    ChevronRight,
    Server,
    Loader2,
    Wifi,
    WifiOff,
    Trash2,
    Pencil,
    MoreHorizontal,
    KeyRound,
    SlidersHorizontal,
    Users,
} from 'lucide-react';
import {
    getDevices,
    deleteDevice,
    type DeviceResponse,
    type PageResponse,
} from '../api/deviceService';
import { useDebouncedCallback } from '../hooks/useDebouncedCallback';
import CreateDeviceModal from '../components/devices/CreateDeviceModal';
import EditDeviceModal from '../components/devices/EditDeviceModal';
import RegenerateKeyModal from '../components/devices/RegenerateKeyModal';
import ThresholdsModal from '../components/devices/ThresholdsModal';
import RecipientsModal from '../components/devices/RecipientsModal';
import DeviceUsersModal from '../components/devices/DeviceUsersModal';
import { toast } from 'sonner';
import { getSettings } from '../api/settingsService';

/* ── Portal-based dropdown ──────────────────────────────────────────────── */
function ActionMenu({
    onEdit,
    onRegenKey,
    onDelete,
    onThresholds,
    onRecipients,
    onManageUsers,
    onClose,
    anchorRef,
}: {
    onEdit: () => void;
    onRegenKey: () => void;
    onDelete: () => void;
    onThresholds: () => void;
    onRecipients: () => void;
    onManageUsers: () => void;
    onClose: () => void;
    anchorRef: React.RefObject<HTMLButtonElement | null>;
}) {
    const menuRef = useRef<HTMLDivElement>(null);
    const [pos, setPos] = useState({ top: 0, left: 0 });

    useEffect(() => {
        if (!anchorRef.current) return;
        const rect = anchorRef.current.getBoundingClientRect();
        const menuWidth = 176; // w-44
        let left = rect.right - menuWidth;
        if (left < 8) left = 8;
        let top = rect.bottom + 4;
        // Flip up if not enough space below
        if (top + 200 > window.innerHeight) {
            top = rect.top - 4 - 200;
            if (top < 8) top = 8;
        }
        setPos({ top, left });
    }, [anchorRef]);

    return createPortal(
        <>
            <div className="fixed inset-0 z-[100]" onMouseDown={onClose} />
            <div
                ref={menuRef}
                className="fixed z-[101] w-44 bg-zinc-900 border border-zinc-800 rounded-xl shadow-xl shadow-black/30 py-1"
                style={{ top: pos.top, left: pos.left }}
            >
                <button
                    onClick={(e) => { e.stopPropagation(); onClose(); onEdit(); }}
                    className="flex items-center gap-2.5 w-full px-3 py-2 text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                >
                    <Pencil className="w-3.5 h-3.5" />
                    Edit
                </button>
                <button
                    onClick={(e) => { e.stopPropagation(); onClose(); onThresholds(); }}
                    className="flex items-center gap-2.5 w-full px-3 py-2 text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                >
                    <SlidersHorizontal className="w-3.5 h-3.5" />
                    Thresholds
                </button>
                <button
                    onClick={(e) => { e.stopPropagation(); onClose(); onRecipients(); }}
                    className="flex items-center gap-2.5 w-full px-3 py-2 text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                >
                    <Users className="w-3.5 h-3.5" />
                    Recipients
                </button>
                <button
                    onClick={(e) => { e.stopPropagation(); onClose(); onRegenKey(); }}
                    className="flex items-center gap-2.5 w-full px-3 py-2 text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                >
                    <KeyRound className="w-3.5 h-3.5" />
                    Regenerate Key
                </button>
                <button
                    onClick={(e) => { e.stopPropagation(); onClose(); onManageUsers(); }}
                    className="flex items-center gap-2.5 w-full px-3 py-2 text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                >
                    <Users className="w-3.5 h-3.5" />
                    Manage Users
                </button>
                <div className="my-1 border-t border-zinc-800/60" />
                <button
                    onClick={(e) => { e.stopPropagation(); onClose(); onDelete(); }}
                    className="flex items-center gap-2.5 w-full px-3 py-2 text-sm text-red-400 hover:text-red-300 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                >
                    <Trash2 className="w-3.5 h-3.5" />
                    Delete
                </button>
            </div>
        </>,
        document.body,
    );
}

/* ── Devices Page ───────────────────────────────────────────────────────── */
export default function Devices() {
    const navigate = useNavigate();
    const [page, setPage] = useState(0);
    const [keyword, setKeyword] = useState('');
    const [searchInput, setSearchInput] = useState('');
    const [data, setData] = useState<PageResponse<DeviceResponse> | null>(null);
    const [loading, setLoading] = useState(true);
    const [createOpen, setCreateOpen] = useState(false);
    const [editTarget, setEditTarget] = useState<DeviceResponse | null>(null);
    const [regenTarget, setRegenTarget] = useState<DeviceResponse | null>(null);
    const [threshTarget, setThreshTarget] = useState<DeviceResponse | null>(null);
    const [recipTarget, setRecipTarget] = useState<DeviceResponse | null>(null);
    const [deleteTarget, setDeleteTarget] = useState<DeviceResponse | null>(null);
    const [deleting, setDeleting] = useState(false);
    const [openMenu, setOpenMenu] = useState<string | null>(null);
    const menuBtnRefs = useRef<Map<string, HTMLButtonElement | null>>(new Map());
    const [manageUsersTarget, setManageUsersTarget] = useState<DeviceResponse | null>(null);

    // Tick counter: incremented every 10s to force re-render so isOnline() recalculates
    const [tick, setTick] = useState(0);
    useEffect(() => {
        const interval = setInterval(() => setTick((t) => t + 1), 10_000);
        return () => clearInterval(interval);
    }, []);

    // Fetch device offline threshold from system settings
    const [offlineThreshold, setOfflineThreshold] = useState(300); // default 5 min
    useEffect(() => {
        getSettings()
            .then((s) => setOfflineThreshold(s.deviceOfflineThresholdSeconds))
            .catch(() => { /* keep default */ });
    }, []);

    const pageSize = 10;

    const fetchDevices = useCallback(async () => {
        setLoading(true);
        try {
            const res = await getDevices(page, pageSize, keyword);
            setData(res);
        } catch {
            // handled by interceptors
        } finally {
            setLoading(false);
        }
    }, [page, keyword]);

    useEffect(() => {
        fetchDevices();
    }, [fetchDevices]);

    const debouncedSearch = useDebouncedCallback((value: string) => {
        setKeyword(value);
        setPage(0);
    }, 400);

    const handleSearchChange = (value: string) => {
        setSearchInput(value);
        debouncedSearch(value);
    };

    const handleDelete = async () => {
        if (!deleteTarget) return;
        setDeleting(true);
        try {
            await deleteDevice(deleteTarget.id);
            toast.success(`${deleteTarget.name} deleted`);
            setDeleteTarget(null);
            fetchDevices();
        } catch {
            toast.error('Failed to delete device');
        } finally {
            setDeleting(false);
        }
    };

    const isOnline = (lastSeen?: string) => {
        if (!lastSeen) return false;
        const dateStr = lastSeen.endsWith('Z') ? lastSeen : `${lastSeen}Z`;
        return (Date.now() - new Date(dateStr).getTime()) / 1000 <= offlineThreshold;
    };

    const formatDate = (date?: string) => {
        if (!date) return '—';
        const dateStr = date.endsWith('Z') ? date : `${date}Z`;
        return new Intl.DateTimeFormat('en-US', {
            month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
        }).format(new Date(dateStr));
    };

    const devices = data?.content ?? [];
    const totalPages = data?.totalPages ?? 0;
    const totalElements = data?.totalElements ?? 0;

    // Reference tick to prevent tree-shaking (it forces re-renders for isOnline)
    void tick;

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-100 tracking-tight">Devices</h1>
                    <p className="text-sm text-zinc-500 mt-0.5">
                        {totalElements} device{totalElements !== 1 ? 's' : ''} registered
                    </p>
                </div>
                <button
                    onClick={() => setCreateOpen(true)}
                    className="flex items-center gap-2 px-4 py-2.5 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 active:bg-zinc-300 transition-colors cursor-pointer"
                >
                    <Plus className="w-4 h-4" />
                    Add Device
                </button>
            </div>

            {/* Search */}
            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-600" />
                <input
                    type="text"
                    value={searchInput}
                    onChange={(e) => handleSearchChange(e.target.value)}
                    placeholder="Search devices…"
                    className="w-full pl-9 pr-4 py-2.5 rounded-lg bg-zinc-900/60 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm outline-none transition-all duration-200 focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700"
                />
            </div>

            {/* Table */}
            <div className="bg-zinc-900/40 border border-zinc-800/60 rounded-xl overflow-x-auto">
                <div className="grid grid-cols-[1fr_100px_48px] md:grid-cols-[1fr_140px_140px_120px_100px_48px] gap-4 px-5 py-3 border-b border-zinc-800/60 text-xs font-medium text-zinc-500 uppercase tracking-wider min-w-0">
                    <span>Device</span><span className="hidden md:block">IP Address</span><span className="hidden md:block">OS</span><span className="hidden md:block">Last Seen</span><span>Status</span><span />
                </div>

                {loading && (
                    <div className="flex items-center justify-center py-16">
                        <Loader2 className="w-5 h-5 text-zinc-600 animate-spin" />
                    </div>
                )}

                {!loading && devices.length === 0 && (
                    <div className="flex flex-col items-center justify-center py-16 gap-3">
                        <div className="w-12 h-12 rounded-xl bg-zinc-800/60 flex items-center justify-center">
                            <Server className="w-6 h-6 text-zinc-600" />
                        </div>
                        <p className="text-sm text-zinc-600">{keyword ? 'No devices match your search' : 'No devices yet'}</p>
                    </div>
                )}

                {!loading && devices.map((device) => {
                    const online = isOnline(device.lastSeen);
                    return (
                        <div key={device.id} className="grid grid-cols-[1fr_100px_48px] md:grid-cols-[1fr_140px_140px_120px_100px_48px] gap-4 px-5 py-3.5 border-b border-zinc-800/30 last:border-b-0 items-center hover:bg-zinc-800/20 transition-colors group cursor-pointer" onClick={() => navigate(`/devices/${device.id}`)}>
                            <div className="flex items-center gap-3 min-w-0">
                                <div className="w-8 h-8 rounded-lg bg-zinc-800/80 border border-zinc-700/40 flex items-center justify-center shrink-0">
                                    <Server className="w-4 h-4 text-zinc-400" />
                                </div>
                                <div className="min-w-0">
                                    <Link to={`/devices/${device.id}`} onClick={(e) => e.stopPropagation()} className="text-sm font-medium text-zinc-200 hover:text-zinc-50 truncate block transition-colors">{device.name}</Link>
                                    {device.ownerUsername && <p className="text-xs text-zinc-600 truncate">{device.ownerUsername}</p>}
                                </div>
                            </div>
                            <span className="hidden md:block text-sm text-zinc-400 font-mono truncate">{device.ipAddress}</span>
                            <span className="hidden md:block text-sm text-zinc-500 truncate">{device.operatingSystem}</span>
                            <span className="hidden md:block text-xs text-zinc-600">{formatDate(device.lastSeen)}</span>
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
                            <div>
                                <button
                                    ref={(el) => { menuBtnRefs.current.set(device.id, el); }}
                                    onClick={(e) => { e.stopPropagation(); setOpenMenu(openMenu === device.id ? null : device.id); }}
                                    className="p-1.5 rounded-lg text-zinc-600 hover:text-zinc-300 hover:bg-zinc-800 transition-colors opacity-0 group-hover:opacity-100 cursor-pointer"
                                >
                                    <MoreHorizontal className="w-4 h-4" />
                                </button>
                                {openMenu === device.id && (
                                    <ActionMenu
                                        anchorRef={{ current: menuBtnRefs.current.get(device.id) ?? null }}
                                        onClose={() => setOpenMenu(null)}
                                        onEdit={() => setEditTarget(device)}
                                        onRegenKey={() => setRegenTarget(device)}
                                        onDelete={() => setDeleteTarget(device)}
                                        onThresholds={() => setThreshTarget(device)}
                                        onRecipients={() => setRecipTarget(device)}
                                        onManageUsers={() => setManageUsersTarget(device)}
                                    />
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
                <div className="flex items-center justify-between">
                    <p className="text-xs text-zinc-600">Page {page + 1} of {totalPages}</p>
                    <div className="flex items-center gap-1">
                        <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0} className="p-2 rounded-lg text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer">
                            <ChevronLeft className="w-4 h-4" />
                        </button>
                        {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                            let pn: number;
                            if (totalPages <= 5) pn = i;
                            else if (page < 3) pn = i;
                            else if (page > totalPages - 4) pn = totalPages - 5 + i;
                            else pn = page - 2 + i;
                            return (
                                <button key={pn} onClick={() => setPage(pn)} className={`w-8 h-8 rounded-lg text-sm font-medium transition-colors cursor-pointer ${pn === page ? 'bg-zinc-800 text-zinc-100' : 'text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800/60'}`}>
                                    {pn + 1}
                                </button>
                            );
                        })}
                        <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="p-2 rounded-lg text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer">
                            <ChevronRight className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            )}

            {/* Modals */}
            <CreateDeviceModal open={createOpen} onClose={() => setCreateOpen(false)} onCreated={fetchDevices} />
            <EditDeviceModal open={!!editTarget} device={editTarget} onClose={() => setEditTarget(null)} onUpdated={fetchDevices} />
            <RegenerateKeyModal open={!!regenTarget} deviceId={regenTarget?.id ?? null} deviceName={regenTarget?.name ?? ''} onClose={() => setRegenTarget(null)} />
            <ThresholdsModal open={!!threshTarget} deviceId={threshTarget?.id ?? null} deviceName={threshTarget?.name ?? ''} onClose={() => setThreshTarget(null)} />
            <RecipientsModal open={!!recipTarget} deviceId={recipTarget?.id ?? null} deviceName={recipTarget?.name ?? ''} onClose={() => setRecipTarget(null)} />
            <DeviceUsersModal open={!!manageUsersTarget} deviceId={manageUsersTarget?.id ?? null} deviceName={manageUsersTarget?.name ?? ''} onClose={() => setManageUsersTarget(null)} />

            {/* Delete confirmation */}
            {deleteTarget && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => !deleting && setDeleteTarget(null)} />
                    <div className="relative w-full max-w-sm bg-zinc-900 border border-zinc-800 rounded-2xl shadow-2xl shadow-black/40 p-6">
                        <h3 className="text-base font-semibold text-zinc-100 mb-2">Delete Device</h3>
                        <p className="text-sm text-zinc-400 mb-5">
                            Are you sure you want to delete <span className="text-zinc-200 font-medium">{deleteTarget.name}</span>? This action cannot be undone.
                        </p>
                        <div className="flex justify-end gap-2">
                            <button onClick={() => setDeleteTarget(null)} disabled={deleting} className="px-4 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer">Cancel</button>
                            <button onClick={handleDelete} disabled={deleting} className="px-4 py-2 rounded-lg bg-red-500/20 border border-red-500/30 text-red-400 text-sm font-medium hover:bg-red-500/30 disabled:opacity-50 transition-colors flex items-center gap-2 cursor-pointer">
                                {deleting ? <><Loader2 className="w-4 h-4 animate-spin" />Deleting…</> : 'Delete'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
