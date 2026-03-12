import { useState, useEffect, type FormEvent } from 'react';
import { Loader2, Save, Activity, Clock, Calendar, AlertTriangle } from 'lucide-react';
import {
    getSettings,
    updateSettings,
    type SettingsUpdateModel,
} from '../api/settingsService';
import { toast } from 'sonner';

export default function Settings() {
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [form, setForm] = useState<SettingsUpdateModel>({
        rawDataRetentionDays: 7,
        hourlyDataRetentionDays: 30,
        dailyDataRetentionDays: 365,
        deviceOfflineThresholdSeconds: 300,
    });

    useEffect(() => {
        (async () => {
            try {
                const data = await getSettings();
                setForm(data);
            } catch {
                toast.error('Failed to load settings');
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setSaving(true);
        try {
            await updateSettings(form);
            toast.success('Settings saved');
        } catch (err: unknown) {
            toast.error(
                (err as { response?: { data?: { message?: string } } })?.response
                    ?.data?.message || 'Failed to save settings',
            );
        } finally {
            setSaving(false);
        }
    };

    const inputClasses =
        'w-full px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 text-sm outline-none transition-all duration-200 focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700';

    if (loading) {
        return (
            <div className="flex items-center justify-center py-24">
                <Loader2 className="w-6 h-6 text-zinc-600 animate-spin" />
            </div>
        );
    }

    const fields: {
        key: keyof SettingsUpdateModel;
        label: string;
        hint: string;
        icon: typeof Activity;
        min: number;
        max?: number;
        unit: string;
    }[] = [
            {
                key: 'rawDataRetentionDays',
                label: 'High-Resolution Data Retention',
                hint: 'How long to keep the exact, second-by-second metrics before permanent deletion. (Data is automatically summarized into hourly averages in the background).',
                icon: Activity,
                min: 1,
                max: 365,
                unit: 'days',
            },
            {
                key: 'hourlyDataRetentionDays',
                label: 'Hourly Averages Retention',
                hint: 'How long to keep the 1-hour aggregated blocks before permanent deletion. Used for rendering weekly and monthly charts.',
                icon: Clock,
                min: 7,
                max: 1825,
                unit: 'days',
            },
            {
                key: 'dailyDataRetentionDays',
                label: 'Daily Averages Retention',
                hint: 'How long to keep the 1-day aggregated blocks before permanent deletion. Used for long-term historical statistics.',
                icon: Calendar,
                min: 30,
                unit: 'days',
            },
            {
                key: 'deviceOfflineThresholdSeconds',
                label: 'Offline Detection Time',
                hint: "The amount of time without receiving any metrics before the system marks a device as 'Offline'.",
                icon: AlertTriangle,
                min: 30,
                max: 86400,
                unit: 'seconds',
            },
        ];

    return (
        <div className="space-y-6 max-w-2xl">
            <div>
                <h1 className="text-2xl font-semibold text-zinc-100 tracking-tight">
                    Settings
                </h1>
                <p className="text-sm text-zinc-500 mt-0.5">
                    Manage data retention and system thresholds
                </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-1">
                {fields.map((f) => (
                    <div
                        key={f.key}
                        className="flex items-start gap-4 px-5 py-4 rounded-xl bg-zinc-900/40 border border-zinc-800/40 hover:border-zinc-800/80 transition-colors"
                    >
                        <div className="w-9 h-9 rounded-lg bg-zinc-800/60 flex items-center justify-center shrink-0 mt-0.5">
                            <f.icon className="w-4 h-4 text-zinc-400" />
                        </div>
                        <div className="flex-1 min-w-0">
                            <label className="block text-sm font-medium text-zinc-200 mb-0.5">
                                {f.label}
                            </label>
                            <p className="text-xs text-zinc-500 mb-2.5">{f.hint}</p>
                            <div className="flex items-center gap-2 max-w-[200px]">
                                <input
                                    type="number"
                                    min={f.min}
                                    max={f.max}
                                    required
                                    value={form[f.key]}
                                    onChange={(e) =>
                                        setForm({ ...form, [f.key]: parseInt(e.target.value) || 0 })
                                    }
                                    className={inputClasses}
                                />
                                <span className="text-xs text-zinc-600 shrink-0">{f.unit}</span>
                            </div>
                        </div>
                    </div>
                ))}

                <div className="pt-4">
                    <button
                        type="submit"
                        disabled={saving}
                        className="flex items-center gap-2 px-5 py-2.5 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 disabled:opacity-50 transition-colors cursor-pointer"
                    >
                        {saving ? (
                            <>
                                <Loader2 className="w-4 h-4 animate-spin" />
                                Saving…
                            </>
                        ) : (
                            <>
                                <Save className="w-4 h-4" />
                                Save Settings
                            </>
                        )}
                    </button>
                </div>
            </form>
        </div>
    );
}
