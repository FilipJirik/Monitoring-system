import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import { ShieldX } from 'lucide-react';

interface Props {
    /** If true, shows a 403 page instead of redirecting */
    showForbidden?: boolean;
}

export default function AdminRoute({ showForbidden = true }: Props) {
    const user = useAuthStore((s) => s.user);

    if (user?.role !== 'ADMIN') {
        if (showForbidden) {
            return (
                <div className="flex flex-col items-center justify-center py-24 gap-4">
                    <div className="w-16 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center">
                        <ShieldX className="w-8 h-8 text-red-400" />
                    </div>
                    <div className="text-center">
                        <h2 className="text-xl font-semibold text-zinc-100 mb-1">Access Denied</h2>
                        <p className="text-sm text-zinc-500 max-w-xs">
                            You don't have permission to access this page. Contact your administrator.
                        </p>
                    </div>
                    <a
                        href="/"
                        className="mt-2 px-4 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 border border-zinc-800 transition-colors"
                    >
                        Back to Dashboard
                    </a>
                </div>
            );
        }
        return <Navigate to="/" replace />;
    }

    return <Outlet />;
}
