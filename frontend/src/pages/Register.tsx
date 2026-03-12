import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Monitor, Eye, EyeOff, Loader2 } from 'lucide-react';
import api from '../api/axios';
import { toast } from 'sonner';

export default function Register() {
    const navigate = useNavigate();

    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            await api.post('/auth/register', { username, email, password });
            toast.success('Account created — please sign in');
            navigate('/login', { replace: true });
        } catch (err: unknown) {
            const message =
                (err as { response?: { data?: { message?: string } } })?.response?.data
                    ?.message || 'Registration failed';
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    const inputClasses =
        'w-full px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm outline-none transition-all duration-200 focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700';

    return (
        <div className="min-h-screen bg-zinc-950 flex items-center justify-center p-4">
            <div className="fixed inset-0 bg-[radial-gradient(ellipse_at_top,_var(--color-zinc-900)_0%,_var(--color-zinc-950)_70%)]" />

            <div className="relative w-full max-w-md">
                {/* Logo */}
                <div className="flex items-center justify-center gap-3 mb-10">
                    <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-zinc-800/80 border border-zinc-700/50">
                        <Monitor className="w-5 h-5 text-zinc-300" />
                    </div>
                    <span className="text-xl font-semibold text-zinc-100 tracking-tight">
                        Device Monitor
                    </span>
                </div>

                {/* Card */}
                <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-8 backdrop-blur-sm shadow-2xl shadow-black/20">
                    <div className="mb-6">
                        <h1 className="text-lg font-semibold text-zinc-100">Create account</h1>
                        <p className="text-sm text-zinc-500 mt-1">
                            Register to start monitoring your devices
                        </p>
                    </div>

                    {error && (
                        <div className="mb-5 px-4 py-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-4">
                        {/* Username */}
                        <div>
                            <label htmlFor="username" className="block text-sm font-medium text-zinc-400 mb-1.5">
                                Username
                            </label>
                            <input
                                id="username"
                                type="text"
                                required
                                maxLength={50}
                                value={username}
                                onChange={(e) => { setUsername(e.target.value); setError(null); }}
                                placeholder="john_doe"
                                autoComplete="username"
                                className={inputClasses}
                            />
                        </div>

                        {/* Email */}
                        <div>
                            <label htmlFor="email" className="block text-sm font-medium text-zinc-400 mb-1.5">
                                Email
                            </label>
                            <input
                                id="email"
                                type="email"
                                required
                                value={email}
                                onChange={(e) => { setEmail(e.target.value); setError(null); }}
                                placeholder="you@example.com"
                                autoComplete="email"
                                className={inputClasses}
                            />
                        </div>

                        {/* Password */}
                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-zinc-400 mb-1.5">
                                Password
                            </label>
                            <div className="relative">
                                <input
                                    id="password"
                                    type={showPassword ? 'text' : 'password'}
                                    required
                                    minLength={8}
                                    value={password}
                                    onChange={(e) => { setPassword(e.target.value); setError(null); }}
                                    placeholder="Min 8 characters"
                                    autoComplete="new-password"
                                    className={`${inputClasses} pr-10`}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-600 hover:text-zinc-400 transition-colors"
                                    tabIndex={-1}
                                >
                                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                </button>
                            </div>
                        </div>

                        {/* Submit */}
                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full mt-2 px-4 py-2.5 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 active:bg-zinc-300 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 flex items-center justify-center gap-2 cursor-pointer"
                        >
                            {loading ? (
                                <><Loader2 className="w-4 h-4 animate-spin" />Creating account…</>
                            ) : (
                                'Create account'
                            )}
                        </button>
                    </form>

                    {/* Link to Login */}
                    <p className="mt-5 text-center text-sm text-zinc-500">
                        Already have an account?{' '}
                        <Link to="/login" className="text-zinc-300 hover:text-zinc-100 transition-colors">
                            Sign in
                        </Link>
                    </p>
                </div>

                <p className="text-center text-xs text-zinc-600 mt-6">Device Monitoring System</p>
            </div>
        </div>
    );
}
