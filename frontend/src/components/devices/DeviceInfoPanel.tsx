import {
    Terminal,
    Globe,
    Network,
    Server,
    User,
    Calendar,
    FileText,
    Laptop,
} from 'lucide-react';
import type { DeviceResponse } from '../../api/deviceService';

interface Props {
    device: DeviceResponse;
}

function InfoRow({ icon: Icon, label, value }: { icon: typeof Server; label: string; value?: string | null }) {
    if (!value) return null;
    return (
        <div className="flex items-start gap-2.5 py-1.5">
            <Icon className="w-3.5 h-3.5 text-zinc-600 mt-0.5 shrink-0" />
            <div className="min-w-0">
                <p className="text-[10px] uppercase tracking-wider text-zinc-600 font-medium">{label}</p>
                <p className="text-sm text-zinc-300 break-all">{value}</p>
            </div>
        </div>
    );
}

export default function DeviceInfoPanel({ device }: Props) {
    return (
        <div className="rounded-xl bg-zinc-900/60 border border-zinc-800/40 p-4 space-y-1">
            {/* SSH Badge */}
            <div className="flex items-center gap-2 mb-3">
                <Terminal className="w-4 h-4 text-zinc-500" />
                {device.sshEnabled ? (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                        SSH Available
                    </span>
                ) : (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-zinc-800/60 text-zinc-500 border border-zinc-700/40">
                        <span className="w-1.5 h-1.5 rounded-full bg-zinc-600" />
                        SSH Disabled
                    </span>
                )}
            </div>

            <InfoRow icon={Laptop} label="Model" value={device.model} />
            <InfoRow icon={Server} label="Operating System" value={device.operatingSystem} />
            <InfoRow icon={Globe} label="IP Address" value={device.ipAddress} />
            <InfoRow icon={Network} label="MAC Address" value={device.macAddress} />
            <InfoRow icon={FileText} label="Description" value={device.description} />
            <InfoRow icon={User} label="Owner" value={device.ownerUsername} />
            <InfoRow
                icon={Calendar}
                label="Created"
                value={device.createdAt ? new Date(device.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }) : undefined}
            />
        </div>
    );
}
