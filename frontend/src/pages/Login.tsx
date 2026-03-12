import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { Monitor, Eye, EyeOff, Loader2 } from 'lucide-react';

export default function Login() {
    const navigate = useNavigate();
    const { login, error, clearError } = useAuthStore();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            await login(email, password);
            navigate('/', { replace: true });
        } catch {
            // Error is handled by the store
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="min-h-screen bg-zinc-950 flex items-center justify-center p-4">
            {/* Subtle background gradient */}
            <div className="fixed inset-0 bg-[radial-gradient(ellipse_at_top,_var(--color-zinc-900)_0%,_var(--color-zinc-950)_70%)]" />

            <div className="relative w-full max-w-md">
                {/* Logo / Brand */}
                <div className="flex items-center justify-center gap-3 mb-10">
                    <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-zinc-800/80 border border-zinc-700/50">
                        <Monitor className="w-5 h-5 text-zinc-300" />
                    </div>
                    <span className="text-xl font-semibold text-zinc-100 tracking-tight">
                        Device Monitor
                    </span>
                </div>

                {/* Login card */}
                <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-8 backdrop-blur-sm shadow-2xl shadow-black/20">
                    <div className="mb-6">
                        <h1 className="text-lg font-semibold text-zinc-100">Sign in</h1>
                        <p className="text-sm text-zinc-500 mt-1">
                            Enter your credentials to access the dashboard
                        </p>
                    </div>

                    {error && (
                        <div className="mb-5 px-4 py-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-4">
                        {/* Email */}
                        <div>
                            <label
                                htmlFor="email"
                                className="block text-sm font-medium text-zinc-400 mb-1.5"
                            >
                                Email
                            </label>
                            <input
                                id="email"
                                type="email"
                                required
                                value={email}
                                onChange={(e) => {
                                    setEmail(e.target.value);
                                    clearError();
                                }}
                                placeholder="you@example.com"
                                autoComplete="email"
                                className="w-full px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm
                           outline-none transition-all duration-200
                           focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700"
                            />
                        </div>

                        {/* Password */}
                        <div>
                            <label
                                htmlFor="password"
                                className="block text-sm font-medium text-zinc-400 mb-1.5"
                            >
                                Password
                            </label>
                            <div className="relative">
                                <input
                                    id="password"
                                    type={showPassword ? 'text' : 'password'}
                                    required
                                    value={password}
                                    onChange={(e) => {
                                        setPassword(e.target.value);
                                        clearError();
                                    }}
                                    placeholder="••••••••"
                                    autoComplete="current-password"
                                    className="w-full px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm
                             outline-none transition-all duration-200 pr-10
                             focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-600 hover:text-zinc-400 transition-colors"
                                    tabIndex={-1}
                                >
                                    {showPassword ? (
                                        <EyeOff className="w-4 h-4" />
                                    ) : (
                                        <Eye className="w-4 h-4" />
                                    )}
                                </button>
                            </div>
                        </div>

                        {/* Submit */}
                        <button
                            type="submit"
                            disabled={submitting}
                            className="w-full mt-2 px-4 py-2.5 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium
                         hover:bg-zinc-200 active:bg-zinc-300
                         disabled:opacity-50 disabled:cursor-not-allowed
                         transition-all duration-200 flex items-center justify-center gap-2"
                        >
                            {submitting ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                    Signing in…
                                </>
                            ) : (
                                'Sign in'
                            )}
                        </button>
                    </form>
                    {/* Link to Register */}
                    <p className="mt-5 text-center text-sm text-zinc-500">
                        Don't have an account?{' '}
                        <Link to="/register" className="text-zinc-300 hover:text-zinc-100 transition-colors">
                            Register
                        </Link>
                    </p>
                </div>

                {/* Footer */}
                <p className="text-center text-xs text-zinc-600 mt-6">
                    Device Monitoring System
                </p>
            </div>
        </div>
    );
}
