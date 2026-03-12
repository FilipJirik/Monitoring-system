import { useState, useEffect, useCallback, useRef, type FormEvent } from 'react';
import { createPortal } from 'react-dom';
import {
    Search,
    Plus,
    ChevronLeft,
    ChevronRight,
    Users as UsersIcon,
    Loader2,
    Trash2,
    Pencil,
    MoreHorizontal,
    Mail,
    Server,
} from 'lucide-react';
import {
    getUsers,
    createUser,
    updateUser,
    deleteUser,
    type UserResponse,
    type CreateUserModel,
    type UpdateUserModel,
} from '../api/userService';
import type { PageResponse } from '../api/deviceService';
import { useDebouncedCallback } from '../hooks/useDebouncedCallback';
import Modal from '../components/ui/Modal';
import UserDevicesModal from '../components/users/UserDevicesModal';
import { toast } from 'sonner';

/* ── Portal action menu ────────────────────────────────────────────────── */
function UserActionMenu({
    onEdit,
    onDelete,
    onManageDevices,
    onClose,
    anchorRef,
}: {
    onEdit: () => void;
    onDelete: () => void;
    onManageDevices: () => void;
    onClose: () => void;
    anchorRef: React.RefObject<HTMLButtonElement | null>;
}) {
    const [pos, setPos] = useState({ top: 0, left: 0 });

    useEffect(() => {
        if (!anchorRef.current) return;
        const rect = anchorRef.current.getBoundingClientRect();
        setPos({ top: rect.bottom + 4, left: Math.max(8, rect.right - 176) });
    }, [anchorRef]);

    return createPortal(
        <>
            <div className="fixed inset-0 z-[100]" onMouseDown={onClose} />
            <div
                className="fixed z-[101] w-44 bg-zinc-900 border border-zinc-800 rounded-xl shadow-xl shadow-black/30 py-1"
                style={{ top: pos.top, left: pos.left }}
            >
                <button
                    onClick={() => { onClose(); onEdit(); }}
                    className="flex items-center gap-2.5 w-full px-3 py-2 text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                >
                    <Pencil className="w-3.5 h-3.5" />
                    Edit
                </button>
                <button
                    onClick={() => { onClose(); onManageDevices(); }}
                    className="flex items-center gap-2.5 w-full px-3 py-2 text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                >
                    <Server className="w-3.5 h-3.5" />
                    Manage Devices
                </button>
                <button
                    onClick={() => { onClose(); onDelete(); }}
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

/* ── Users Page ────────────────────────────────────────────────────────── */
export default function Users() {
    const [page, setPage] = useState(0);
    const [keyword, setKeyword] = useState('');
    const [searchInput, setSearchInput] = useState('');
    const [data, setData] = useState<PageResponse<UserResponse> | null>(null);
    const [loading, setLoading] = useState(true);
    const [openMenu, setOpenMenu] = useState<string | null>(null);
    const menuBtnRefs = useRef<Map<string, HTMLButtonElement | null>>(new Map());

    // Create modal
    const [createOpen, setCreateOpen] = useState(false);
    const [createForm, setCreateForm] = useState<CreateUserModel>({ username: '', password: '', email: '' });
    const [creating, setCreating] = useState(false);

    // Edit modal
    const [editTarget, setEditTarget] = useState<UserResponse | null>(null);
    const [editForm, setEditForm] = useState<UpdateUserModel>({});
    const [editing, setEditing] = useState(false);

    // Delete
    const [deleteTarget, setDeleteTarget] = useState<UserResponse | null>(null);
    const [deleting, setDeleting] = useState(false);

    // Manage devices modal
    const [devicesTarget, setDevicesTarget] = useState<UserResponse | null>(null);

    const pageSize = 10;

    const fetchUsers = useCallback(async () => {
        setLoading(true);
        try {
            const res = await getUsers(page, pageSize, keyword);
            setData(res);
        } catch { /* interceptors */ }
        finally { setLoading(false); }
    }, [page, keyword]);

    useEffect(() => { fetchUsers(); }, [fetchUsers]);

    const debouncedSearch = useDebouncedCallback((value: string) => {
        setKeyword(value);
        setPage(0);
    }, 400);

    const handleSearchChange = (value: string) => {
        setSearchInput(value);
        debouncedSearch(value);
    };

    const handleCreate = async (e: FormEvent) => {
        e.preventDefault();
        setCreating(true);
        try {
            await createUser(createForm);
            toast.success('User created');
            setCreateOpen(false);
            setCreateForm({ username: '', password: '', email: '' });
            fetchUsers();
        } catch (err: unknown) {
            toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Failed to create user');
        } finally { setCreating(false); }
    };

    const handleEdit = async (e: FormEvent) => {
        e.preventDefault();
        if (!editTarget) return;
        setEditing(true);
        try {
            const payload: UpdateUserModel = { ...editForm };
            if (!payload.password) delete payload.password;
            await updateUser(editTarget.id, payload);
            toast.success('User updated');
            setEditTarget(null);
            fetchUsers();
        } catch (err: unknown) {
            toast.error((err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Failed to update user');
        } finally { setEditing(false); }
    };

    const handleDelete = async () => {
        if (!deleteTarget) return;
        setDeleting(true);
        try {
            await deleteUser(deleteTarget.id);
            toast.success(`${deleteTarget.username} deleted`);
            setDeleteTarget(null);
            fetchUsers();
        } catch { toast.error('Failed to delete user'); }
        finally { setDeleting(false); }
    };

    const users = data?.content ?? [];
    const totalPages = data?.totalPages ?? 0;
    const totalElements = data?.totalElements ?? 0;

    const inputClasses = 'w-full px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm outline-none transition-all duration-200 focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700';

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-100 tracking-tight">Users</h1>
                    <p className="text-sm text-zinc-500 mt-0.5">{totalElements} user{totalElements !== 1 ? 's' : ''}</p>
                </div>
                <button onClick={() => setCreateOpen(true)} className="flex items-center gap-2 px-4 py-2.5 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 active:bg-zinc-300 transition-colors cursor-pointer">
                    <Plus className="w-4 h-4" />
                    Add User
                </button>
            </div>

            {/* Search */}
            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-600" />
                <input
                    type="text"
                    value={searchInput}
                    onChange={(e) => handleSearchChange(e.target.value)}
                    placeholder="Search users…"
                    className="w-full pl-9 pr-4 py-2.5 rounded-lg bg-zinc-900/60 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm outline-none transition-all duration-200 focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700"
                />
            </div>

            {/* Table */}
            <div className="bg-zinc-900/40 border border-zinc-800/60 rounded-xl">
                <div className="grid grid-cols-[1fr_1fr_48px] gap-4 px-5 py-3 border-b border-zinc-800/60 text-xs font-medium text-zinc-500 uppercase tracking-wider">
                    <span>Username</span><span>Email</span><span />
                </div>

                {loading && (
                    <div className="flex items-center justify-center py-16">
                        <Loader2 className="w-5 h-5 text-zinc-600 animate-spin" />
                    </div>
                )}

                {!loading && users.length === 0 && (
                    <div className="flex flex-col items-center justify-center py-16 gap-3">
                        <div className="w-12 h-12 rounded-xl bg-zinc-800/60 flex items-center justify-center">
                            <UsersIcon className="w-6 h-6 text-zinc-600" />
                        </div>
                        <p className="text-sm text-zinc-600">{keyword ? 'No users match your search' : 'No users yet'}</p>
                    </div>
                )}

                {!loading && users.map((user) => (
                    <div key={user.id} className="grid grid-cols-[1fr_1fr_48px] gap-4 px-5 py-3.5 border-b border-zinc-800/30 last:border-b-0 items-center hover:bg-zinc-800/20 transition-colors group">
                        <div className="flex items-center gap-3 min-w-0">
                            <div className="w-8 h-8 rounded-full bg-zinc-800/80 border border-zinc-700/40 flex items-center justify-center shrink-0 text-xs font-semibold text-zinc-300 uppercase">
                                {user.username?.charAt(0) ?? '?'}
                            </div>
                            <span className="text-sm font-medium text-zinc-200 truncate">{user.username}</span>
                        </div>
                        <div className="flex items-center gap-2 min-w-0">
                            <Mail className="w-3.5 h-3.5 text-zinc-600 shrink-0" />
                            <span className="text-sm text-zinc-400 truncate">{user.email || '—'}</span>
                        </div>
                        <div>
                            <button
                                ref={(el) => { menuBtnRefs.current.set(user.id, el); }}
                                onClick={() => setOpenMenu(openMenu === user.id ? null : user.id)}
                                className="p-1.5 rounded-lg text-zinc-600 hover:text-zinc-300 hover:bg-zinc-800 transition-colors opacity-0 group-hover:opacity-100 cursor-pointer"
                            >
                                <MoreHorizontal className="w-4 h-4" />
                            </button>
                            {openMenu === user.id && (
                                <UserActionMenu
                                    anchorRef={{ current: menuBtnRefs.current.get(user.id) ?? null }}
                                    onClose={() => setOpenMenu(null)}
                                    onEdit={() => {
                                        setEditTarget(user);
                                        setEditForm({ username: user.username, email: user.email, password: '' });
                                    }}
                                    onDelete={() => setDeleteTarget(user)}
                                    onManageDevices={() => setDevicesTarget(user)}
                                />
                            )}
                        </div>
                    </div>
                ))}
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
                                <button key={pn} onClick={() => setPage(pn)} className={`w-8 h-8 rounded-lg text-sm font-medium transition-colors cursor-pointer ${pn === page ? 'bg-zinc-800 text-zinc-100' : 'text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800/60'}`}>{pn + 1}</button>
                            );
                        })}
                        <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="p-2 rounded-lg text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer">
                            <ChevronRight className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            )}

            {/* Create User Modal */}
            <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Add User" maxWidth="max-w-md" disableBackdropClose>
                <form onSubmit={handleCreate} className="space-y-4">
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">Username <span className="text-red-400">*</span></label>
                        <input required value={createForm.username} onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })} placeholder="john_doe" className={inputClasses} />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">Email</label>
                        <input type="email" value={createForm.email ?? ''} onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })} placeholder="john@example.com" className={inputClasses} />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">Password <span className="text-red-400">*</span></label>
                        <input required type="password" minLength={8} value={createForm.password} onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })} placeholder="Min 8 characters" className={inputClasses} />
                    </div>
                    <div className="flex justify-end gap-2 pt-2">
                        <button type="button" onClick={() => setCreateOpen(false)} className="px-4 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer">Cancel</button>
                        <button type="submit" disabled={creating} className="px-4 py-2 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 disabled:opacity-50 transition-colors flex items-center gap-2 cursor-pointer">
                            {creating ? <><Loader2 className="w-4 h-4 animate-spin" />Creating…</> : 'Create User'}
                        </button>
                    </div>
                </form>
            </Modal>

            {/* Edit User Modal */}
            <Modal open={!!editTarget} onClose={() => setEditTarget(null)} title="Edit User" maxWidth="max-w-md" disableBackdropClose>
                <form onSubmit={handleEdit} className="space-y-4">
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">Username</label>
                        <input value={editForm.username ?? ''} onChange={(e) => setEditForm({ ...editForm, username: e.target.value })} className={inputClasses} />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">Email</label>
                        <input type="email" value={editForm.email ?? ''} onChange={(e) => setEditForm({ ...editForm, email: e.target.value })} className={inputClasses} />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">New Password <span className="text-zinc-600">(leave blank to keep)</span></label>
                        <input type="password" value={editForm.password ?? ''} onChange={(e) => setEditForm({ ...editForm, password: e.target.value })} placeholder="••••••••" className={inputClasses} />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">Role</label>
                        <select value={editForm.role ?? 'USER'} onChange={(e) => setEditForm({ ...editForm, role: e.target.value as 'USER' | 'ADMIN' })} className={`${inputClasses} cursor-pointer`}>
                            <option value="USER">User</option>
                            <option value="ADMIN">Admin</option>
                        </select>
                    </div>
                    <div className="flex justify-end gap-2 pt-2">
                        <button type="button" onClick={() => setEditTarget(null)} className="px-4 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer">Cancel</button>
                        <button type="submit" disabled={editing} className="px-4 py-2 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 disabled:opacity-50 transition-colors flex items-center gap-2 cursor-pointer">
                            {editing ? <><Loader2 className="w-4 h-4 animate-spin" />Saving…</> : 'Save Changes'}
                        </button>
                    </div>
                </form>
            </Modal>

            {/* Delete Confirmation */}
            {deleteTarget && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => !deleting && setDeleteTarget(null)} />
                    <div className="relative w-full max-w-sm bg-zinc-900 border border-zinc-800 rounded-2xl shadow-2xl shadow-black/40 p-6">
                        <h3 className="text-base font-semibold text-zinc-100 mb-2">Delete User</h3>
                        <p className="text-sm text-zinc-400 mb-5">
                            Are you sure you want to delete <span className="text-zinc-200 font-medium">{deleteTarget.username}</span>? This action cannot be undone.
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

            {/* User Devices Modal */}
            <UserDevicesModal
                open={!!devicesTarget}
                userId={devicesTarget?.id ?? null}
                username={devicesTarget?.username ?? ''}
                onClose={() => setDevicesTarget(null)}
            />
        </div>
    );
}
