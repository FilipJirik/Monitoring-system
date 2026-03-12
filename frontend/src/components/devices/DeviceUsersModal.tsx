import { useState, useEffect, useCallback } from 'react';
import { Loader2, Users as UsersIcon, Trash2, Plus, Search } from 'lucide-react';
import Modal from '../ui/Modal';
import {
    getDeviceUserAccesses,
    grantDeviceAccess,
    updateDeviceAccess,
    revokeDeviceAccess,
    type UserAccessResponse,
} from '../../api/permissionService';
import { getUsers, type UserResponse } from '../../api/userService';
import { toast } from 'sonner';

interface Props {
    open: boolean;
    deviceId: string | null;
    deviceName: string;
    onClose: () => void;
}

export default function DeviceUsersModal({ open, deviceId, deviceName, onClose }: Props) {
    const [accesses, setAccesses] = useState<UserAccessResponse[]>([]);
    const [loading, setLoading] = useState(false);

    // Grant form
    const [allUsers, setAllUsers] = useState<UserResponse[]>([]);
    const [userSearch, setUserSearch] = useState('');
    const [selectedUserId, setSelectedUserId] = useState('');
    const [grantLevel, setGrantLevel] = useState<'READ' | 'EDIT'>('READ');
    const [granting, setGranting] = useState(false);
    const [showDropdown, setShowDropdown] = useState(false);

    // Revoke confirmation
    const [revokeTarget, setRevokeTarget] = useState<UserAccessResponse | null>(null);
    const [revoking, setRevoking] = useState(false);

    const fetchAccesses = useCallback(async () => {
        if (!deviceId) return;
        setLoading(true);
        try {
            const data = await getDeviceUserAccesses(deviceId);
            setAccesses(data.content ?? []);
        } catch {
            toast.error('Failed to load user accesses');
        } finally {
            setLoading(false);
        }
    }, [deviceId]);

    useEffect(() => {
        if (open && deviceId) {
            fetchAccesses();
            getUsers(0, 200).then((d) => setAllUsers(d.content ?? [])).catch(() => { });
        }
        if (!open) {
            setAccesses([]);
            setSelectedUserId('');
            setUserSearch('');
            setRevokeTarget(null);
        }
    }, [open, deviceId, fetchAccesses]);

    const handleGrant = async () => {
        if (!deviceId || !selectedUserId) return;
        setGranting(true);
        try {
            await grantDeviceAccess(selectedUserId, deviceId, grantLevel);
            toast.success('User access granted');
            setSelectedUserId('');
            setUserSearch('');
            fetchAccesses();
        } catch {
            toast.error('Failed to grant access');
        } finally {
            setGranting(false);
        }
    };

    const handleToggle = async (access: UserAccessResponse) => {
        if (!deviceId || access.permissionLevel === 'OWNER') return;
        const newLevel = access.permissionLevel === 'READ' ? 'EDIT' : 'READ';
        try {
            await updateDeviceAccess(access.userId, deviceId, newLevel);
            setAccesses((prev) =>
                prev.map((a) =>
                    a.userId === access.userId ? { ...a, permissionLevel: newLevel } : a,
                ),
            );
        } catch {
            toast.error('Failed to update permission');
        }
    };

    const handleRevoke = async () => {
        if (!deviceId || !revokeTarget) return;
        setRevoking(true);
        try {
            await revokeDeviceAccess(revokeTarget.userId, deviceId);
            toast.success('Access revoked');
            setRevokeTarget(null);
            fetchAccesses();
        } catch {
            toast.error('Failed to revoke access');
        } finally {
            setRevoking(false);
        }
    };

    const assignedIds = new Set(accesses.map((a) => a.userId));
    const filteredUsers = allUsers.filter(
        (u) =>
            !assignedIds.has(u.id) &&
            (u.username.toLowerCase().includes(userSearch.toLowerCase()) ||
                (u.email ?? '').toLowerCase().includes(userSearch.toLowerCase())),
    );
    const selectedUser = allUsers.find((u) => u.id === selectedUserId);

    return (
        <>
            <Modal open={open} onClose={onClose} title={`Users — ${deviceName}`} maxWidth="max-w-lg">
                {/* Grant new access */}
                <div className="mb-5 p-3 rounded-lg bg-zinc-800/30 border border-zinc-800/60 space-y-3">
                    <p className="text-xs font-medium text-zinc-400 uppercase tracking-wider">Grant access</p>
                    <div className="flex items-center gap-2">
                        <div className="relative flex-1">
                            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-zinc-600" />
                            <input
                                value={selectedUser ? selectedUser.username : userSearch}
                                onChange={(e) => {
                                    setUserSearch(e.target.value);
                                    setSelectedUserId('');
                                    setShowDropdown(true);
                                }}
                                onFocus={() => setShowDropdown(true)}
                                placeholder="Search users…"
                                className="w-full pl-8 pr-3 py-2 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm outline-none focus:border-zinc-600"
                            />
                            {showDropdown && !selectedUserId && userSearch && (
                                <div className="absolute z-10 top-full mt-1 w-full max-h-40 overflow-y-auto bg-zinc-900 border border-zinc-800 rounded-lg shadow-xl">
                                    {filteredUsers.length === 0 ? (
                                        <p className="px-3 py-2 text-xs text-zinc-600">No users found</p>
                                    ) : (
                                        filteredUsers.map((u) => (
                                            <button
                                                key={u.id}
                                                onClick={() => {
                                                    setSelectedUserId(u.id);
                                                    setUserSearch('');
                                                    setShowDropdown(false);
                                                }}
                                                className="flex items-center gap-2 w-full px-3 py-2 text-sm text-zinc-300 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                                            >
                                                <div className="w-5 h-5 rounded-full bg-zinc-800 border border-zinc-700/40 flex items-center justify-center text-[10px] font-semibold text-zinc-400 uppercase shrink-0">
                                                    {u.username?.charAt(0) ?? '?'}
                                                </div>
                                                <span className="truncate">{u.username}</span>
                                                {u.email && <span className="text-zinc-600 text-xs ml-auto truncate">{u.email}</span>}
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
                            disabled={!selectedUserId || granting}
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
                            <UsersIcon className="w-6 h-6 text-zinc-600" />
                        </div>
                        <p className="text-sm text-zinc-600">No user access configured</p>
                    </div>
                ) : (
                    <div className="space-y-1 max-h-[340px] overflow-y-auto pr-1">
                        {accesses.map((a) => (
                            <div key={a.userId} className="flex items-center justify-between px-3 py-2.5 rounded-lg hover:bg-zinc-800/30 transition-colors group">
                                <div className="flex items-center gap-3 min-w-0">
                                    <div className="w-7 h-7 rounded-full bg-zinc-800/80 border border-zinc-700/40 flex items-center justify-center text-xs font-semibold text-zinc-300 uppercase shrink-0">
                                        {a.username?.charAt(0) ?? '?'}
                                    </div>
                                    <div className="min-w-0">
                                        <span className="text-sm text-zinc-200 truncate block">{a.username}</span>
                                        {a.email && <span className="text-xs text-zinc-600 truncate block">{a.email}</span>}
                                    </div>
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
                            Remove <span className="text-zinc-200 font-medium">{revokeTarget.username}</span>'s access to <span className="text-zinc-200 font-medium">{deviceName}</span>?
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
