import { useState, useCallback, type FormEvent } from 'react';
import { Loader2, Copy, Check, Terminal, KeyRound } from 'lucide-react';
import Modal from '../ui/Modal';
import LocationPicker from './LocationPicker';
import {
    createDevice,
    type CreateDeviceModel,
    type DeviceWithApiKeyModel,
} from '../../api/deviceService';

interface Props {
    open: boolean;
    onClose: () => void;
    onCreated: () => void;
}

const initialForm: CreateDeviceModel = {
    name: '',
    operatingSystem: '',
    ipAddress: '',
    macAddress: '',
    description: '',
    model: '',
    sshEnabled: false,
};

export default function CreateDeviceModal({ open, onClose, onCreated }: Props) {
    const [form, setForm] = useState<CreateDeviceModel>({ ...initialForm });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [result, setResult] = useState<DeviceWithApiKeyModel | null>(null);
    const [copiedField, setCopiedField] = useState<string | null>(null);

    const handleLocationChange = useCallback((lat: number, lng: number) => {
        setForm((prev) => ({ ...prev, latitude: lat, longitude: lng }));
    }, []);

    const handleClose = () => {
        setForm({ ...initialForm });
        setError(null);
        setResult(null);
        setLoading(false);
        onClose();
    };

    const handleCopy = async (text: string, field: string) => {
        await navigator.clipboard.writeText(text);
        setCopiedField(field);
        setTimeout(() => setCopiedField(null), 2000);
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            const payload: CreateDeviceModel = {
                ...form,
                macAddress: form.macAddress?.trim() || undefined,
                description: form.description?.trim() || undefined,
                model: form.model?.trim() || undefined,
            };
            const data = await createDevice(payload);
            setResult(data);
            onCreated();
        } catch (err: unknown) {
            const message =
                (err as { response?: { data?: { message?: string } } })?.response?.data
                    ?.message || 'Failed to create device';
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    const inputClasses =
        'w-full px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm outline-none transition-all duration-200 focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700';

    return (
        <Modal
            open={open}
            onClose={handleClose}
            title={result ? 'Device Created' : 'Add Device'}
            maxWidth="max-w-xl"
            disableBackdropClose
        >
            {result ? (
                /* ── Success view with API key ──────────────────────────────── */
                <div className="space-y-5">
                    <div className="px-4 py-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm">
                        Device created successfully. Save the API key — it won't be shown
                        again.
                    </div>

                    {/* API Key */}
                    <div>
                        <label className="flex items-center gap-1.5 text-xs font-medium text-zinc-500 mb-1.5">
                            <KeyRound className="w-3.5 h-3.5" />
                            API Key
                        </label>
                        <div className="flex items-center gap-2">
                            <code className="flex-1 px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-amber-400 text-sm font-mono truncate">
                                {result.apiKey}
                            </code>
                            <button
                                onClick={() => handleCopy(result.apiKey, 'apiKey')}
                                className="shrink-0 p-2.5 rounded-lg border border-zinc-800 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer"
                                title="Copy API key"
                            >
                                {copiedField === 'apiKey' ? (
                                    <Check className="w-4 h-4 text-emerald-400" />
                                ) : (
                                    <Copy className="w-4 h-4" />
                                )}
                            </button>
                        </div>
                    </div>

                    {/* Setup Command */}
                    {result.setupCommand && (
                        <div>
                            <label className="flex items-center gap-1.5 text-xs font-medium text-zinc-500 mb-1.5">
                                <Terminal className="w-3.5 h-3.5" />
                                Setup Command
                            </label>
                            <div className="flex items-start gap-2">
                                <pre className="flex-1 px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-emerald-400 text-xs font-mono whitespace-pre-wrap break-all overflow-auto max-h-32">
                                    {result.setupCommand}
                                </pre>
                                <button
                                    onClick={() =>
                                        handleCopy(result.setupCommand, 'setupCommand')
                                    }
                                    className="shrink-0 p-2.5 rounded-lg border border-zinc-800 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer mt-0.5"
                                    title="Copy setup command"
                                >
                                    {copiedField === 'setupCommand' ? (
                                        <Check className="w-4 h-4 text-emerald-400" />
                                    ) : (
                                        <Copy className="w-4 h-4" />
                                    )}
                                </button>
                            </div>
                        </div>
                    )}

                    <div className="flex justify-end pt-1">
                        <button
                            onClick={handleClose}
                            className="px-4 py-2 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 transition-colors cursor-pointer"
                        >
                            Done
                        </button>
                    </div>
                </div>
            ) : (
                /* ── Create form ───────────────────────────────────────────── */
                <form onSubmit={handleSubmit} className="space-y-4">
                    {error && (
                        <div className="px-4 py-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
                            {error}
                        </div>
                    )}

                    {/* Row: Name + OS */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                                Name <span className="text-red-400">*</span>
                            </label>
                            <input
                                required
                                value={form.name}
                                onChange={(e) => setForm({ ...form, name: e.target.value })}
                                placeholder="web-server-01"
                                className={inputClasses}
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                                Operating System <span className="text-red-400">*</span>
                            </label>
                            <input
                                required
                                value={form.operatingSystem}
                                onChange={(e) =>
                                    setForm({ ...form, operatingSystem: e.target.value })
                                }
                                placeholder="Ubuntu 22.04"
                                className={inputClasses}
                            />
                        </div>
                    </div>

                    {/* Row: IP + MAC */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                                IP Address <span className="text-red-400">*</span>
                            </label>
                            <input
                                required
                                value={form.ipAddress}
                                onChange={(e) =>
                                    setForm({ ...form, ipAddress: e.target.value })
                                }
                                placeholder="192.168.1.100"
                                className={inputClasses}
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                                MAC Address
                            </label>
                            <input
                                value={form.macAddress ?? ''}
                                onChange={(e) =>
                                    setForm({ ...form, macAddress: e.target.value })
                                }
                                placeholder="AA:BB:CC:DD:EE:FF"
                                className={inputClasses}
                            />
                        </div>
                    </div>

                    {/* Model */}
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                            Model
                        </label>
                        <input
                            value={form.model ?? ''}
                            onChange={(e) => setForm({ ...form, model: e.target.value })}
                            placeholder="Dell PowerEdge R740"
                            className={inputClasses}
                        />
                    </div>

                    {/* Description */}
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                            Description
                        </label>
                        <textarea
                            value={form.description ?? ''}
                            onChange={(e) =>
                                setForm({ ...form, description: e.target.value })
                            }
                            placeholder="Optional description…"
                            rows={2}
                            className={`${inputClasses} resize-none`}
                        />
                    </div>

                    {/* Location — map picker + manual lat/lng */}
                    <LocationPicker
                        latitude={form.latitude}
                        longitude={form.longitude}
                        onChange={handleLocationChange}
                    />
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                                Latitude
                            </label>
                            <input
                                type="number"
                                step="any"
                                value={form.latitude ?? ''}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        latitude: e.target.value ? parseFloat(e.target.value) : undefined,
                                        longitude: form.longitude,
                                    })
                                }
                                placeholder="48.8566"
                                className={inputClasses}
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                                Longitude
                            </label>
                            <input
                                type="number"
                                step="any"
                                value={form.longitude ?? ''}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        longitude: e.target.value ? parseFloat(e.target.value) : undefined,
                                        latitude: form.latitude,
                                    })
                                }
                                placeholder="2.3522"
                                className={inputClasses}
                            />
                        </div>
                    </div>

                    {/* SSH toggle */}
                    <label className="flex items-center gap-3 cursor-pointer group">
                        <div className="relative">
                            <input
                                type="checkbox"
                                checked={form.sshEnabled ?? false}
                                onChange={(e) =>
                                    setForm({ ...form, sshEnabled: e.target.checked })
                                }
                                className="sr-only peer"
                            />
                            <div className="w-9 h-5 rounded-full bg-zinc-800 border border-zinc-700 peer-checked:bg-emerald-500/20 peer-checked:border-emerald-500/40 transition-colors" />
                            <div className="absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-zinc-500 peer-checked:bg-emerald-400 peer-checked:translate-x-4 transition-all" />
                        </div>
                        <span className="text-sm text-zinc-400 group-hover:text-zinc-300 transition-colors">
                            SSH Enabled
                        </span>
                    </label>

                    {/* Actions */}
                    <div className="flex justify-end gap-2 pt-2">
                        <button
                            type="button"
                            onClick={handleClose}
                            className="px-4 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={loading}
                            className="px-4 py-2 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
                        >
                            {loading ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                    Creating…
                                </>
                            ) : (
                                'Create Device'
                            )}
                        </button>
                    </div>
                </form>
            )}
        </Modal>
    );
}
