import { useState, useEffect } from 'react';
import { Loader2, Plus, Trash2, Mail, Bell, Crown, ShieldCheck } from 'lucide-react';
import {
    getRecipients,
    addRecipient,
    updateRecipient,
    deleteRecipient,
    type RecipientStatusModel,
} from '../../api/recipientService';
import { toast } from 'sonner';

interface Props {
    deviceId: string;
    readOnly?: boolean;
}

export default function RecipientsPanel({ deviceId, readOnly = false }: Props) {
    const [recipients, setRecipients] = useState<RecipientStatusModel[]>([]);
    const [loading, setLoading] = useState(true);
    const [togglingId, setTogglingId] = useState<string | null>(null);
    const [addingId, setAddingId] = useState<string | null>(null);
    const [removingId, setRemovingId] = useState<string | null>(null);

    const fetchRecipients = async () => {
        setLoading(true);
        try {
            const res = await getRecipients(deviceId, true, 0, 100);
            setRecipients(res.content);
        } catch {
            toast.error('Failed to load recipients');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchRecipients();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [deviceId]);

    const handleAdd = async (r: RecipientStatusModel) => {
        setAddingId(r.userId);
        try {
            await addRecipient(deviceId, { userId: r.userId, notifyEmail: true, notifyFrontend: true });
            toast.success(`${r.username} added as recipient`);
            fetchRecipients();
        } catch {
            toast.error('Failed to add recipient');
        } finally {
            setAddingId(null);
        }
    };

    const handleToggle = async (r: RecipientStatusModel, field: 'notifyEmail' | 'notifyFrontend') => {
        setTogglingId(`${r.userId}-${field}`);
        try {
            await updateRecipient(deviceId, r.userId, {
                notifyEmail: field === 'notifyEmail' ? !r.notifyEmail : r.notifyEmail,
                notifyFrontend: field === 'notifyFrontend' ? !r.notifyFrontend : r.notifyFrontend,
            });
            fetchRecipients();
        } catch {
            toast.error('Failed to update recipient');
        } finally {
            setTogglingId(null);
        }
    };

    const handleRemove = async (r: RecipientStatusModel) => {
        setRemovingId(r.userId);
        try {
            await deleteRecipient(deviceId, r.userId);
            toast.success(`${r.username} removed`);
            fetchRecipients();
        } catch {
            toast.error('Failed to remove recipient');
        } finally {
            setRemovingId(null);
        }
    };

    const activeRecipients = recipients.filter((r) => r.recipient);
    const availableUsers = recipients.filter((r) => !r.recipient);

    if (loading) {
        return (
            <div className="flex items-center justify-center py-12">
                <Loader2 className="w-5 h-5 text-zinc-600 animate-spin" />
            </div>
        );
    }

    return (
        <div className="space-y-5">
            {/* Active Recipients */}
            <div>
                <h3 className="text-xs font-medium text-zinc-500 uppercase tracking-wider mb-2">
                    Active Recipients ({activeRecipients.length})
                </h3>
                {activeRecipients.length === 0 && (
                    <p className="text-sm text-zinc-600 py-4 text-center">No recipients configured</p>
                )}
                <div className="space-y-1">
                    {activeRecipients.map((r) => (
                        <div
                            key={r.userId}
                            className="flex items-center justify-between gap-3 px-4 py-3 rounded-lg bg-zinc-900/60 border border-zinc-800/40"
                        >
                            <div className="flex items-center gap-3 min-w-0">
                                <div className="w-8 h-8 rounded-full bg-zinc-800/80 border border-zinc-700/40 flex items-center justify-center shrink-0 text-xs font-semibold text-zinc-300 uppercase">
                                    {r.username?.charAt(0) ?? '?'}
                                </div>
                                <div className="min-w-0">
                                    <div className="flex items-center gap-2">
                                        <span className="text-sm font-medium text-zinc-200 truncate">{r.username}</span>
                                        {r.owner && (
                                            <span className="flex items-center gap-1 text-[10px] text-amber-400 bg-amber-500/10 px-1.5 py-0.5 rounded-md">
                                                <Crown className="w-2.5 h-2.5" /> Owner
                                            </span>
                                        )}
                                        {r.admin && (
                                            <span className="flex items-center gap-1 text-[10px] text-cyan-400 bg-cyan-500/10 px-1.5 py-0.5 rounded-md">
                                                <ShieldCheck className="w-2.5 h-2.5" /> Admin
                                            </span>
                                        )}
                                    </div>
                                    <span className="text-xs text-zinc-600 truncate block">{r.email}</span>
                                </div>
                            </div>
                            {!readOnly && (
                                <div className="flex items-center gap-2 shrink-0">
                                    {/* Email toggle */}
                                    <button
                                        onClick={() => handleToggle(r, 'notifyEmail')}
                                        disabled={togglingId === `${r.userId}-notifyEmail`}
                                        className={`p-1.5 rounded-lg border transition-colors cursor-pointer ${r.notifyEmail
                                            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                                            : 'bg-zinc-900 border-zinc-800 text-zinc-600 hover:text-zinc-400'
                                            }`}
                                        title="Email notifications"
                                    >
                                        {togglingId === `${r.userId}-notifyEmail` ? (
                                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                        ) : (
                                            <Mail className="w-3.5 h-3.5" />
                                        )}
                                    </button>
                                    {/* Frontend toggle */}
                                    <button
                                        onClick={() => handleToggle(r, 'notifyFrontend')}
                                        disabled={togglingId === `${r.userId}-notifyFrontend`}
                                        className={`p-1.5 rounded-lg border transition-colors cursor-pointer ${r.notifyFrontend
                                            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                                            : 'bg-zinc-900 border-zinc-800 text-zinc-600 hover:text-zinc-400'
                                            }`}
                                        title="Frontend notifications"
                                    >
                                        {togglingId === `${r.userId}-notifyFrontend` ? (
                                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                        ) : (
                                            <Bell className="w-3.5 h-3.5" />
                                        )}
                                    </button>
                                    {/* Remove */}
                                    <button
                                        onClick={() => handleRemove(r)}
                                        disabled={removingId === r.userId}
                                        className="p-1.5 rounded-lg text-zinc-600 hover:text-red-400 hover:bg-red-500/10 transition-colors cursor-pointer"
                                        title="Remove recipient"
                                    >
                                        {removingId === r.userId ? (
                                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                        ) : (
                                            <Trash2 className="w-3.5 h-3.5" />
                                        )}
                                    </button>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            </div>

            {/* Available Users */}
            {!readOnly && availableUsers.length > 0 && (
                <div>
                    <h3 className="text-xs font-medium text-zinc-500 uppercase tracking-wider mb-2">
                        Available Users ({availableUsers.length})
                    </h3>
                    <div className="space-y-1">
                        {availableUsers.map((r) => (
                            <div
                                key={r.userId}
                                className="flex items-center justify-between gap-3 px-4 py-3 rounded-lg bg-zinc-900/30 border border-zinc-800/20 hover:border-zinc-800/40 transition-colors"
                            >
                                <div className="flex items-center gap-3 min-w-0">
                                    <div className="w-8 h-8 rounded-full bg-zinc-800/60 border border-zinc-700/30 flex items-center justify-center shrink-0 text-xs font-semibold text-zinc-500 uppercase">
                                        {r.username?.charAt(0) ?? '?'}
                                    </div>
                                    <div className="min-w-0">
                                        <span className="text-sm text-zinc-400 truncate block">{r.username}</span>
                                        <span className="text-xs text-zinc-600 truncate block">{r.email}</span>
                                    </div>
                                </div>
                                <button
                                    onClick={() => handleAdd(r)}
                                    disabled={addingId === r.userId}
                                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 border border-zinc-800 transition-colors cursor-pointer disabled:opacity-50"
                                >
                                    {addingId === r.userId ? (
                                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                    ) : (
                                        <Plus className="w-3.5 h-3.5" />
                                    )}
                                    Add
                                </button>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}
