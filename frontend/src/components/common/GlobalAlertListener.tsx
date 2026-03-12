import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { useWebSocketStore } from '../../stores/websocketStore';
import { AlertTriangle, AlertOctagon, ArrowRight } from 'lucide-react';

const METRIC_LABELS: Record<string, string> = {
    CPU_USAGE: 'CPU Usage',
    CPU_TEMP: 'CPU Temp',
    CPU_FREQ: 'CPU Freq',
    RAM_USAGE: 'RAM Usage',
    DISK_USAGE: 'Disk Usage',
    NETWORK_IN: 'Network In',
    NETWORK_OUT: 'Network Out',
    UPTIME: 'Uptime',
    PROCESS_COUNT: 'Process Count',
    TCP_CONNECTIONS_COUNT: 'TCP Connections',
    LISTENING_PORTS_COUNT: 'Listening Ports',
};

export default function GlobalAlertListener() {
    const navigate = useNavigate();
    const lastAlert = useWebSocketStore((s) => s.lastAlert);
    const lastAlertTimestamp = useWebSocketStore((s) => s.lastAlertTimestamp);

    useEffect(() => {
        if (!lastAlert || !lastAlertTimestamp) return;

        // Common toast options
        const description = METRIC_LABELS[lastAlert.metricType] ?? lastAlert.metricType;
        const deviceName = lastAlert.deviceName;
        const deviceId = lastAlert.deviceId;

        if (lastAlert.severity === 'CRITICAL') {
            toast.custom((t) => (
                <div className="w-full relative overflow-hidden rounded-xl bg-red-600 border border-red-500 shadow-2xl shadow-red-900/20 p-4 pr-10">
                    <div className="flex items-start gap-4">
                        <div className="p-2 bg-red-800/50 rounded-lg shrink-0">
                            <AlertOctagon className="w-6 h-6 text-white" />
                        </div>
                        <div className="flex-1 min-w-0">
                            <h3 className="text-base font-bold text-white leading-tight mb-1">
                                Critical Alert: {deviceName}
                            </h3>
                            <p className="text-sm text-red-100/90 font-medium mb-3">
                                {description}
                            </p>
                            <button
                                onClick={() => {
                                    toast.dismiss(t);
                                    navigate(`/devices/${deviceId}`);
                                }}
                                className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white text-xs font-semibold transition-colors cursor-pointer"
                            >
                                View Device <ArrowRight className="w-3.5 h-3.5" />
                            </button>
                        </div>
                    </div>
                    <button
                        onClick={() => toast.dismiss(t)}
                        className="absolute top-2 right-2 p-1.5 rounded-lg text-red-200 hover:text-white hover:bg-red-700/50 transition-colors cursor-pointer"
                    >
                        <span className="sr-only">Close</span>
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>
            ), {
                duration: Infinity, // Persistent
                position: 'top-right',
                id: `critical-${lastAlert.id}`, // Prevent duplicates
            });
        } else {
            // WARNING
            toast.custom((t) => (
                <div className="w-full relative overflow-hidden rounded-xl bg-amber-500 border border-amber-400 shadow-xl shadow-amber-900/10 p-4 pr-10">
                    <div className="flex items-start gap-4">
                        <div className="p-2 bg-amber-600/50 rounded-lg shrink-0">
                            <AlertTriangle className="w-6 h-6 text-white" />
                        </div>
                        <div className="flex-1 min-w-0">
                            <h3 className="text-base font-bold text-white leading-tight mb-1">
                                Warning: {deviceName}
                            </h3>
                            <p className="text-sm text-amber-100/90 font-medium mb-3">
                                {description}
                            </p>
                            <button
                                onClick={() => {
                                    toast.dismiss(t);
                                    navigate(`/devices/${deviceId}`);
                                }}
                                className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-black/10 hover:bg-black/20 text-white text-xs font-semibold transition-colors cursor-pointer"
                            >
                                View Device <ArrowRight className="w-3.5 h-3.5" />
                            </button>
                        </div>
                    </div>
                    <button
                        onClick={() => toast.dismiss(t)}
                        className="absolute top-2 right-2 p-1.5 rounded-lg text-amber-100 hover:text-white hover:bg-amber-600/50 transition-colors cursor-pointer"
                    >
                        <span className="sr-only">Close</span>
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>
            ), {
                duration: 10000,
                position: 'top-right',
                id: `warning-${lastAlert.id}`,
            });
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [lastAlertTimestamp]); // Only trigger when timestamp updates

    return null; // Headless component
}
