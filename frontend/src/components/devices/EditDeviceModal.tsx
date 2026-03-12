import { useState, useEffect, useCallback, type FormEvent } from 'react';
import { Loader2 } from 'lucide-react';
import Modal from '../ui/Modal';
import {
    updateDevice,
    type DeviceResponse,
    type UpdateDeviceModel,
} from '../../api/deviceService';
import { toast } from 'sonner';
import LocationPicker from './LocationPicker';

interface Props {
    open: boolean;
    device: DeviceResponse | null;
    onClose: () => void;
    onUpdated: () => void;
}

export default function EditDeviceModal({ open, device, onClose, onUpdated }: Props) {
    const [form, setForm] = useState<UpdateDeviceModel>({});
    const [loading, setLoading] = useState(false);

    const handleLocationChange = useCallback((lat: number, lng: number) => {
        setForm((prev) => ({ ...prev, latitude: lat, longitude: lng }));
    }, []);

    useEffect(() => {
        if (device) {
            setForm({
                name: device.name,
                operatingSystem: device.operatingSystem,
                ipAddress: device.ipAddress,
                macAddress: device.macAddress ?? '',
                description: device.description ?? '',
                model: device.model ?? '',
                sshEnabled: device.sshEnabled ?? false,
                latitude: device.latitude,
                longitude: device.longitude,
            });
        }
    }, [device]);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        if (!device) return;
        setLoading(true);
        try {
            await updateDevice(device.id, form);
            toast.success('Device updated');
            onUpdated();
            onClose();
        } catch (err: unknown) {
            const msg =
                (err as { response?: { data?: { message?: string } } })?.response?.data
                    ?.message || 'Failed to update device';
            toast.error(msg);
        } finally {
            setLoading(false);
        }
    };

    const inputClasses =
        'w-full px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm outline-none transition-all duration-200 focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700';

    return (
        <Modal open={open} onClose={onClose} title="Edit Device" maxWidth="max-w-xl" disableBackdropClose>
            <form onSubmit={handleSubmit} className="space-y-4">
                {/* Row: Name + OS */}
                <div className="grid grid-cols-2 gap-3">
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                            Name <span className="text-red-400">*</span>
                        </label>
                        <input
                            required
                            value={form.name ?? ''}
                            onChange={(e) => setForm({ ...form, name: e.target.value })}
                            className={inputClasses}
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                            Operating System <span className="text-red-400">*</span>
                        </label>
                        <input
                            required
                            value={form.operatingSystem ?? ''}
                            onChange={(e) => setForm({ ...form, operatingSystem: e.target.value })}
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
                            value={form.ipAddress ?? ''}
                            onChange={(e) => setForm({ ...form, ipAddress: e.target.value })}
                            className={inputClasses}
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">
                            MAC Address
                        </label>
                        <input
                            value={form.macAddress ?? ''}
                            onChange={(e) => setForm({ ...form, macAddress: e.target.value })}
                            className={inputClasses}
                        />
                    </div>
                </div>

                {/* Model */}
                <div>
                    <label className="block text-xs font-medium text-zinc-400 mb-1.5">Model</label>
                    <input
                        value={form.model ?? ''}
                        onChange={(e) => setForm({ ...form, model: e.target.value })}
                        className={inputClasses}
                    />
                </div>

                {/* Description */}
                <div>
                    <label className="block text-xs font-medium text-zinc-400 mb-1.5">Description</label>
                    <textarea
                        value={form.description ?? ''}
                        onChange={(e) => setForm({ ...form, description: e.target.value })}
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
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">Latitude</label>
                        <input
                            type="number"
                            step="any"
                            value={form.latitude ?? ''}
                            onChange={(e) =>
                                setForm({
                                    ...form,
                                    latitude: e.target.value ? parseFloat(e.target.value) : undefined,
                                })
                            }
                            placeholder="48.8566"
                            className={inputClasses}
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">Longitude</label>
                        <input
                            type="number"
                            step="any"
                            value={form.longitude ?? ''}
                            onChange={(e) =>
                                setForm({
                                    ...form,
                                    longitude: e.target.value ? parseFloat(e.target.value) : undefined,
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
                            onChange={(e) => setForm({ ...form, sshEnabled: e.target.checked })}
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
                        onClick={onClose}
                        className="px-4 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer"
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        disabled={loading}
                        className="px-4 py-2 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2 cursor-pointer"
                    >
                        {loading ? (
                            <>
                                <Loader2 className="w-4 h-4 animate-spin" />
                                Saving…
                            </>
                        ) : (
                            'Save Changes'
                        )}
                    </button>
                </div>
            </form>
        </Modal>
    );
}
