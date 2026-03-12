import { useState, type FormEvent } from 'react';
import { Loader2, User, Mail, Shield, Lock, Eye, EyeOff } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import api from '../api/axios';
import { toast } from 'sonner';

export default function Profile() {
    const user = useAuthStore((s) => s.user);
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirm, setConfirm] = useState('');
    const [saving, setSaving] = useState(false);
    const [showCurrent, setShowCurrent] = useState(false);
    const [showNew, setShowNew] = useState(false);

    const handleChangePassword = async (e: FormEvent) => {
        e.preventDefault();
        if (newPassword !== confirm) {
            toast.error('Passwords do not match');
            return;
        }
        if (newPassword.length < 8) {
            toast.error('New password must be at least 8 characters');
            return;
        }
        setSaving(true);
        try {
            await api.post('/auth/change-password', { currentPassword, newPassword });
            toast.success('Password changed successfully');
            setCurrentPassword('');
            setNewPassword('');
            setConfirm('');
        } catch (err: unknown) {
            toast.error(
                (err as { response?: { data?: { message?: string } } })?.response
                    ?.data?.message || 'Failed to change password',
            );
        } finally {
            setSaving(false);
        }
    };

    const inputClasses =
        'w-full px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm outline-none transition-all duration-200 focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700';

    return (
        <div className="space-y-8 max-w-2xl">
            <div>
                <h1 className="text-2xl font-semibold text-zinc-100 tracking-tight">Profile</h1>
                <p className="text-sm text-zinc-500 mt-0.5">Your account details and security</p>
            </div>

            {/* ── Account Info ─────────────────────────────────────────────────── */}
            <div className="space-y-1">
                <h2 className="text-sm font-medium text-zinc-400 uppercase tracking-wider mb-3">Account</h2>
                {[
                    { icon: User, label: 'Username', value: user?.username },
                    { icon: Mail, label: 'Email', value: user?.email },
                    { icon: Shield, label: 'Role', value: user?.role },
                ].map((item) => (
                    <div
                        key={item.label}
                        className="flex items-center gap-4 px-5 py-4 rounded-xl bg-zinc-900/40 border border-zinc-800/40"
                    >
                        <div className="w-9 h-9 rounded-lg bg-zinc-800/60 flex items-center justify-center shrink-0">
                            <item.icon className="w-4 h-4 text-zinc-400" />
                        </div>
                        <div>
                            <p className="text-xs text-zinc-500">{item.label}</p>
                            <p className="text-sm font-medium text-zinc-200">{item.value ?? '—'}</p>
                        </div>
                    </div>
                ))}
            </div>

            {/* ── Change Password ──────────────────────────────────────────────── */}
            <div>
                <h2 className="text-sm font-medium text-zinc-400 uppercase tracking-wider mb-3">Security</h2>
                <form
                    onSubmit={handleChangePassword}
                    className="p-5 rounded-xl bg-zinc-900/40 border border-zinc-800/40 space-y-4"
                >
                    <div className="flex items-center gap-3 mb-2">
                        <div className="w-9 h-9 rounded-lg bg-zinc-800/60 flex items-center justify-center">
                            <Lock className="w-4 h-4 text-zinc-400" />
                        </div>
                        <div>
                            <p className="text-sm font-medium text-zinc-200">Change Password</p>
                            <p className="text-xs text-zinc-500">Update your account password</p>
                        </div>
                    </div>

                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                            Current Password <span className="text-red-400">*</span>
                        </label>
                        <div className="relative">
                            <input
                                required
                                type={showCurrent ? 'text' : 'password'}
                                value={currentPassword}
                                onChange={(e) => setCurrentPassword(e.target.value)}
                                placeholder="••••••••"
                                className={inputClasses}
                            />
                            <button
                                type="button"
                                onClick={() => setShowCurrent(!showCurrent)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-600 hover:text-zinc-400 transition-colors cursor-pointer"
                            >
                                {showCurrent ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                            </button>
                        </div>
                    </div>

                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                            New Password <span className="text-red-400">*</span>
                        </label>
                        <div className="relative">
                            <input
                                required
                                minLength={8}
                                type={showNew ? 'text' : 'password'}
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                placeholder="Min 8 characters"
                                className={inputClasses}
                            />
                            <button
                                type="button"
                                onClick={() => setShowNew(!showNew)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-600 hover:text-zinc-400 transition-colors cursor-pointer"
                            >
                                {showNew ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                            </button>
                        </div>
                    </div>

                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                            Confirm New Password <span className="text-red-400">*</span>
                        </label>
                        <input
                            required
                            minLength={8}
                            type="password"
                            value={confirm}
                            onChange={(e) => setConfirm(e.target.value)}
                            placeholder="••••••••"
                            className={inputClasses}
                        />
                    </div>

                    <div className="pt-1">
                        <button
                            type="submit"
                            disabled={saving}
                            className="flex items-center gap-2 px-4 py-2.5 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 disabled:opacity-50 transition-colors cursor-pointer"
                        >
                            {saving ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                    Updating…
                                </>
                            ) : (
                                'Update Password'
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
