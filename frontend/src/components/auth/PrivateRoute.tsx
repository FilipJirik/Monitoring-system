import { useEffect } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import { useWebSocketStore } from '../../stores/websocketStore';
import { Loader2 } from 'lucide-react';

export default function PrivateRoute() {
    const { isAuthenticated, isLoading, initialize } = useAuthStore();
    const connectWs = useWebSocketStore((s) => s.connect);
    const disconnectWs = useWebSocketStore((s) => s.disconnect);

    useEffect(() => {
        initialize();
    }, [initialize]);

    // Open / close WebSocket when auth state changes
    useEffect(() => {
        if (isAuthenticated) {
            connectWs();
        }
        return () => {
            disconnectWs();
        };
    }, [isAuthenticated, connectWs, disconnectWs]);

    if (isLoading) {
        return (
            <div className="min-h-screen bg-zinc-950 flex items-center justify-center">
                <Loader2 className="w-6 h-6 text-zinc-500 animate-spin" />
            </div>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    return <Outlet />;
}
