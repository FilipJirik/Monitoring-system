import { useState, useEffect, useCallback } from 'react';
import { Loader2, Server, Trash2, Plus, Search } from 'lucide-react';
import Modal from '../ui/Modal';
import {
    getUserDeviceAccesses,
    grantDeviceAccess,
    updateDeviceAccess,
    revokeDeviceAccess,
    type DeviceAccessResponse,
} from '../../api/permissionService';
import { getDevices, type DeviceResponse } from '../../api/deviceService';
import { toast } from 'sonner';

interface Props {
    open: boolean;
    userId: string | null;
    username: string;
    onClose: () => void;
}

export default function UserDevicesModal({ open, userId, username, onClose }: Props) {
    const [accesses, setAccesses] = useState<DeviceAccessResponse[]>([]);
    const [loading, setLoading] = useState(false);

    // Grant form
    const [allDevices, setAllDevices] = useState<DeviceResponse[]>([]);
    const [deviceSearch, setDeviceSearch] = useState('');
    const [selectedDeviceId, setSelectedDeviceId] = useState('');
    const [grantLevel, setGrantLevel] = useState<'READ' | 'EDIT'>('READ');
    const [granting, setGranting] = useState(false);
    const [showDropdown, setShowDropdown] = useState(false);

    // Revoke confirmation
    const [revokeTarget, setRevokeTarget] = useState<DeviceAccessResponse | null>(null);
    const [revoking, setRevoking] = useState(false);

    const fetchAccesses = useCallback(async () => {
        if (!userId) return;
        setLoading(true);
        try {
            const data = await getUserDeviceAccesses(userId);
            setAccesses(data.content ?? []);
        } catch {
            toast.error('Failed to load device accesses');
        } finally {
            setLoading(false);
        }
    }, [userId]);

    useEffect(() => {
        if (open && userId) {
            fetchAccesses();
            getDevices(0, 200).then((d) => setAllDevices(d.content ?? [])).catch(() => { });
        }
        if (!open) {
            setAccesses([]);
            setSelectedDeviceId('');
            setDeviceSearch('');
            setRevokeTarget(null);
        }
    }, [open, userId, fetchAccesses]);

    const handleGrant = async () => {
        if (!userId || !selectedDeviceId) return;
        setGranting(true);
        try {
            await grantDeviceAccess(userId, selectedDeviceId, grantLevel);
            toast.success('Device access granted');
            setSelectedDeviceId('');
            setDeviceSearch('');
            fetchAccesses();
        } catch {
            toast.error('Failed to grant access');
        } finally {
            setGranting(false);
        }
    };

    const handleToggle = async (access: DeviceAccessResponse) => {
        if (!userId || access.permissionLevel === 'OWNER') return;
        const newLevel = access.permissionLevel === 'READ' ? 'EDIT' : 'READ';
        try {
            await updateDeviceAccess(userId, access.deviceId, newLevel);
            setAccesses((prev) =>
                prev.map((a) =>
                    a.deviceId === access.deviceId ? { ...a, permissionLevel: newLevel } : a,
                ),
            );
        } catch {
            toast.error('Failed to update permission');
        }
    };

    const handleRevoke = async () => {
        if (!userId || !revokeTarget) return;
        setRevoking(true);
        try {
            await revokeDeviceAccess(userId, revokeTarget.deviceId);
            toast.success('Access revoked');
            setRevokeTarget(null);
            fetchAccesses();
        } catch {
            toast.error('Failed to revoke access');
        } finally {
            setRevoking(false);
        }
    };

    const assignedIds = new Set(accesses.map((a) => a.deviceId));
    const filteredDevices = allDevices.filter(
        (d) =>
            !assignedIds.has(d.id) &&
            d.name.toLowerCase().includes(deviceSearch.toLowerCase()),
    );
    const selectedDevice = allDevices.find((d) => d.id === selectedDeviceId);

    return (
        <>
            <Modal open={open} onClose={onClose} title={`Devices — ${username}`} maxWidth="max-w-lg">
                {/* Grant new access */}
                <div className="mb-5 p-3 rounded-lg bg-zinc-800/30 border border-zinc-800/60 space-y-3">
                    <p className="text-xs font-medium text-zinc-400 uppercase tracking-wider">Grant access</p>
                    <div className="flex items-center gap-2">
                        <div className="relative flex-1">
                            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-zinc-600" />
                            <input
                                value={selectedDevice ? selectedDevice.name : deviceSearch}
                                onChange={(e) => {
                                    setDeviceSearch(e.target.value);
                                    setSelectedDeviceId('');
                                    setShowDropdown(true);
                                }}
                                onFocus={() => setShowDropdown(true)}
                                placeholder="Search devices…"
                                className="w-full pl-8 pr-3 py-2 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm outline-none focus:border-zinc-600"
                            />
                            {showDropdown && !selectedDeviceId && deviceSearch && (
                                <div className="absolute z-10 top-full mt-1 w-full max-h-40 overflow-y-auto bg-zinc-900 border border-zinc-800 rounded-lg shadow-xl">
                                    {filteredDevices.length === 0 ? (
                                        <p className="px-3 py-2 text-xs text-zinc-600">No devices found</p>
                                    ) : (
                                        filteredDevices.map((d) => (
                                            <button
                                                key={d.id}
                                                onClick={() => {
                                                    setSelectedDeviceId(d.id);
                                                    setDeviceSearch('');
                                                    setShowDropdown(false);
                                                }}
                                                className="flex items-center gap-2 w-full px-3 py-2 text-sm text-zinc-300 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                                            >
                                                <Server className="w-3.5 h-3.5 text-zinc-500" />
                                                {d.name}
                                            </button>
                                        ))
                                    )}
                                </div>
                            )}
                        </div>
                        <select
                            value={grantLevel}
                            onChange={(e) => setGrantLevel(e.target.value as 'READ' | 'EDIT')}
                            className="px-3 py-2 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 text-sm outline-none cursor-pointer"
                        >
                            <option value="READ">READ</option>
                            <option value="EDIT">EDIT</option>
                        </select>
                        <button
                            onClick={handleGrant}
                            disabled={!selectedDeviceId || granting}
                            className="flex items-center gap-1.5 px-3 py-2 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 disabled:opacity-40 transition-colors cursor-pointer"
                        >
                            {granting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Plus className="w-3.5 h-3.5" />}
                            Grant
                        </button>
                    </div>
                </div>

                {/* List */}
                {loading ? (
                    <div className="flex justify-center py-10">
                        <Loader2 className="w-5 h-5 text-zinc-600 animate-spin" />
                    </div>
                ) : accesses.length === 0 ? (
                    <div className="flex flex-col items-center py-10 gap-3">
                        <div className="w-12 h-12 rounded-xl bg-zinc-800/60 flex items-center justify-center">
                            <Server className="w-6 h-6 text-zinc-600" />
                        </div>
                        <p className="text-sm text-zinc-600">No device access configured</p>
                    </div>
                ) : (
                    <div className="space-y-1 max-h-[340px] overflow-y-auto pr-1">
                        {accesses.map((a) => (
                            <div key={a.deviceId} className="flex items-center justify-between px-3 py-2.5 rounded-lg hover:bg-zinc-800/30 transition-colors group">
                                <div className="flex items-center gap-3 min-w-0">
                                    <Server className="w-4 h-4 text-zinc-500 shrink-0" />
                                    <span className="text-sm text-zinc-200 truncate">{a.deviceName}</span>
                                </div>
                                <div className="flex items-center gap-2">
                                    {a.permissionLevel === 'OWNER' ? (
                                        <span className="px-2.5 py-1 rounded-md text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20">
                                            OWNER
                                        </span>
                                    ) : (
                                        <button
                                            onClick={() => handleToggle(a)}
                                            className={`px-2.5 py-1 rounded-md text-xs font-medium transition-colors cursor-pointer ${a.permissionLevel === 'EDIT'
                                                    ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 hover:bg-cyan-500/20'
                                                    : 'bg-zinc-800/60 text-zinc-400 border border-zinc-700/40 hover:bg-zinc-800'
                                                }`}
                                        >
                                            {a.permissionLevel}
                                        </button>
                                    )}
                                    {a.permissionLevel !== 'OWNER' && (
                                        <button
                                            onClick={() => setRevokeTarget(a)}
                                            className="p-1.5 rounded-lg text-zinc-600 hover:text-red-400 hover:bg-red-500/10 transition-colors opacity-0 group-hover:opacity-100 cursor-pointer"
                                            title="Revoke access"
                                        >
                                            <Trash2 className="w-3.5 h-3.5" />
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </Modal>

            {/* Revoke confirm */}
            {revokeTarget && (
                <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => !revoking && setRevokeTarget(null)} />
                    <div className="relative w-full max-w-sm bg-zinc-900 border border-zinc-800 rounded-2xl shadow-2xl shadow-black/40 p-6">
                        <h3 className="text-base font-semibold text-zinc-100 mb-2">Revoke Access</h3>
                        <p className="text-sm text-zinc-400 mb-5">
                            Remove <span className="text-zinc-200 font-medium">{username}</span>'s access to <span className="text-zinc-200 font-medium">{revokeTarget.deviceName}</span>?
                        </p>
                        <div className="flex justify-end gap-2">
                            <button onClick={() => setRevokeTarget(null)} disabled={revoking} className="px-4 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer">Cancel</button>
                            <button onClick={handleRevoke} disabled={revoking} className="px-4 py-2 rounded-lg bg-red-500/20 border border-red-500/30 text-red-400 text-sm font-medium hover:bg-red-500/30 disabled:opacity-50 transition-colors flex items-center gap-2 cursor-pointer">
                                {revoking ? <><Loader2 className="w-4 h-4 animate-spin" />Revoking…</> : 'Revoke'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
