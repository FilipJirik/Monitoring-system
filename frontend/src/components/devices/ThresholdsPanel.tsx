import { useState, useEffect, type FormEvent } from 'react';
import { Loader2, Trash2, Plus, Pencil, Check, X, AlertTriangle, AlertOctagon } from 'lucide-react';
import {
    getThresholds,
    createThreshold,
    deleteThreshold,
    updateThreshold,
    type ThresholdResponseModel,
    type CreateThresholdModel,
    type MetricType,
    type Operator,
    type Severity,
} from '../../api/thresholdService';
import { toast } from 'sonner';

interface Props {
    deviceId: string;
    readOnly?: boolean;
}

const METRIC_OPTIONS: { value: MetricType; label: string }[] = [
    { value: 'CPU_USAGE', label: 'CPU Usage' },
    { value: 'CPU_TEMP', label: 'CPU Temp' },
    { value: 'CPU_FREQ', label: 'CPU Freq' },
    { value: 'RAM_USAGE', label: 'RAM Usage' },
    { value: 'DISK_USAGE', label: 'Disk Usage' },
    { value: 'NETWORK_IN', label: 'Network In' },
    { value: 'NETWORK_OUT', label: 'Network Out' },
    { value: 'UPTIME', label: 'Uptime' },
    { value: 'PROCESS_COUNT', label: 'Process Count' },
    { value: 'TCP_CONNECTIONS_COUNT', label: 'TCP Connections' },
    { value: 'LISTENING_PORTS_COUNT', label: 'Listening Ports' },
    { value: 'DEVICE_OFFLINE', label: 'Device Offline' },
];

const OPERATOR_OPTIONS: { value: Operator; label: string }[] = [
    { value: 'GREATER_THAN', label: '>' },
    { value: 'LESS_THAN', label: '<' },
    { value: 'EQUAL', label: '=' },
    { value: 'IS_NULL', label: 'is null' },
    { value: 'IS_NOT_NULL', label: 'is not null' },
];

const isNullOperator = (op: Operator) => op === 'IS_NULL' || op === 'IS_NOT_NULL';
const isDeviceOffline = (mt: MetricType) => mt === 'DEVICE_OFFLINE';

/** For DEVICE_OFFLINE, force safe defaults before submit */
const withOfflineDefaults = (f: CreateThresholdModel): CreateThresholdModel =>
    isDeviceOffline(f.metricType) ? { ...f, operator: 'EQUAL', thresholdValue: 0 } : f;

const initialForm: CreateThresholdModel = {
    metricType: 'CPU_USAGE',
    operator: 'GREATER_THAN',
    thresholdValue: 90,
    severity: 'WARNING',
};

export default function ThresholdsPanel({ deviceId, readOnly = false }: Props) {
    const [thresholds, setThresholds] = useState<ThresholdResponseModel[]>([]);
    const [loading, setLoading] = useState(true);
    const [form, setForm] = useState<CreateThresholdModel>({ ...initialForm });
    const [creating, setCreating] = useState(false);
    const [showForm, setShowForm] = useState(false);
    const [deletingId, setDeletingId] = useState<string | null>(null);

    // Edit state
    const [editingId, setEditingId] = useState<string | null>(null);
    const [editForm, setEditForm] = useState<CreateThresholdModel>({ ...initialForm });
    const [updating, setUpdating] = useState(false);

    const fetchThresholds = async () => {
        setLoading(true);
        try {
            const data = await getThresholds(deviceId);
            setThresholds(data);
        } catch {
            toast.error('Failed to load thresholds');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchThresholds();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [deviceId]);

    const handleCreate = async (e: FormEvent) => {
        e.preventDefault();
        setCreating(true);
        try {
            await createThreshold(deviceId, withOfflineDefaults(form));
            toast.success('Threshold created');
            setShowForm(false);
            setForm({ ...initialForm });
            fetchThresholds();
        } catch (err: unknown) {
            const msg =
                (err as { response?: { data?: { message?: string } } })?.response?.data
                    ?.message || 'Failed to create threshold';
            toast.error(msg);
        } finally {
            setCreating(false);
        }
    };

    const handleDelete = async (id: string) => {
        setDeletingId(id);
        try {
            await deleteThreshold(deviceId, id);
            toast.success('Threshold deleted');
            fetchThresholds();
        } catch {
            toast.error('Failed to delete threshold');
        } finally {
            setDeletingId(null);
        }
    };

    const startEditing = (t: ThresholdResponseModel) => {
        setEditingId(t.id);
        setEditForm({
            metricType: t.metricType,
            operator: t.operator,
            thresholdValue: t.thresholdValue,
            severity: t.severity,
        });
    };

    const cancelEditing = () => {
        setEditingId(null);
    };

    const handleUpdate = async (e: FormEvent) => {
        e.preventDefault();
        if (!editingId) return;
        setUpdating(true);
        try {
            await updateThreshold(deviceId, editingId, withOfflineDefaults(editForm));
            toast.success('Threshold updated');
            setEditingId(null);
            fetchThresholds();
        } catch (err: unknown) {
            const msg =
                (err as { response?: { data?: { message?: string } } })?.response?.data
                    ?.message || 'Failed to update threshold';
            toast.error(msg);
        } finally {
            setUpdating(false);
        }
    };

    const selectClasses =
        'px-3 py-2 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 text-sm outline-none focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700 cursor-pointer';

    return (
        <div className="space-y-4">
            {/* Loading */}
            {loading && (
                <div className="flex items-center justify-center py-8">
                    <Loader2 className="w-5 h-5 text-zinc-600 animate-spin" />
                </div>
            )}

            {/* Empty state */}
            {!loading && thresholds.length === 0 && !showForm && (
                <div className="text-center py-8">
                    <AlertTriangle className="w-8 h-8 text-zinc-800 mx-auto mb-2" />
                    <p className="text-sm text-zinc-600">No thresholds configured</p>
                    <p className="text-xs text-zinc-700 mt-1">Add thresholds to get alert notifications</p>
                </div>
            )}

            {/* List */}
            {!loading && thresholds.length > 0 && (
                <div className="space-y-1">
                    {thresholds.map((t) =>
                        editingId === t.id ? (
                            /* ── Inline edit form ─────────────────────────── */
                            <form
                                key={t.id}
                                onSubmit={handleUpdate}
                                className="p-4 rounded-lg bg-zinc-900/60 border border-zinc-700/60 space-y-3"
                            >
                                <div className="grid grid-cols-2 gap-3">
                                    <div>
                                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">Metric</label>
                                        <select
                                            value={editForm.metricType}
                                            onChange={(e) => setEditForm({ ...editForm, metricType: e.target.value as MetricType })}
                                            className={`w-full ${selectClasses}`}
                                        >
                                            {METRIC_OPTIONS.map((m) => (
                                                <option key={m.value} value={m.value}>
                                                    {m.label}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                    {isDeviceOffline(editForm.metricType) ? (
                                        <div className="col-span-2">
                                            <p className="text-xs text-zinc-500 italic px-1 py-2">
                                                Opt-in for offline alerts. The timeout duration is configured globally in System Settings.
                                            </p>
                                        </div>
                                    ) : (
                                        <div>
                                            <label className="block text-xs font-medium text-zinc-400 mb-1.5">Operator</label>
                                            <select
                                                value={editForm.operator}
                                                onChange={(e) => setEditForm({ ...editForm, operator: e.target.value as Operator })}
                                                className={`w-full ${selectClasses}`}
                                            >
                                                {OPERATOR_OPTIONS.map((o) => (
                                                    <option key={o.value} value={o.value}>
                                                        {o.label} ({o.value.replace('_', ' ').toLowerCase()})
                                                    </option>
                                                ))}
                                            </select>
                                        </div>
                                    )}
                                </div>
                                <div className="grid grid-cols-2 gap-3">
                                    {!isDeviceOffline(editForm.metricType) && !isNullOperator(editForm.operator) && (
                                        <div>
                                            <label className="block text-xs font-medium text-zinc-400 mb-1.5">Value</label>
                                            <input
                                                type="number"
                                                step="any"
                                                required
                                                value={editForm.thresholdValue}
                                                onChange={(e) => setEditForm({ ...editForm, thresholdValue: parseFloat(e.target.value) })}
                                                className="w-full px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 text-sm outline-none focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700"
                                            />
                                        </div>
                                    )}
                                    <div>
                                        <label className="block text-xs font-medium text-zinc-400 mb-1.5">Severity</label>
                                        <select
                                            value={editForm.severity}
                                            onChange={(e) => setEditForm({ ...editForm, severity: e.target.value as Severity })}
                                            className={`w-full ${selectClasses}`}
                                        >
                                            <option value="WARNING">Warning</option>
                                            <option value="CRITICAL">Critical</option>
                                        </select>
                                    </div>
                                </div>
                                <div className="flex justify-end gap-2 pt-1">
                                    <button
                                        type="button"
                                        onClick={cancelEditing}
                                        className="px-3 py-1.5 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer flex items-center gap-1.5"
                                    >
                                        <X className="w-3.5 h-3.5" />
                                        Cancel
                                    </button>
                                    <button
                                        type="submit"
                                        disabled={updating}
                                        className="px-3 py-1.5 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 disabled:opacity-50 transition-colors flex items-center gap-1.5 cursor-pointer"
                                    >
                                        {updating ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                                        Save
                                    </button>
                                </div>
                            </form>
                        ) : (
                            /* ── Read-only row ────────────────────────────── */
                            <div
                                key={t.id}
                                className="flex items-center justify-between gap-3 px-4 py-3 rounded-lg bg-zinc-900/60 border border-zinc-800/40"
                            >
                                <div className="flex items-center gap-3">
                                    {t.severity === 'CRITICAL' ? (
                                        <AlertOctagon className="w-4 h-4 text-red-400 shrink-0" />
                                    ) : (
                                        <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
                                    )}
                                    <div>
                                        <p className="text-sm text-zinc-200">
                                            {METRIC_OPTIONS.find((m) => m.value === t.metricType)?.label}{' '}
                                            {isDeviceOffline(t.metricType) ? (
                                                <span className="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">Enabled</span>
                                            ) : (
                                                <>
                                                    <span className="text-zinc-500">
                                                        {OPERATOR_OPTIONS.find((o) => o.value === t.operator)?.label}
                                                    </span>
                                                    {!isNullOperator(t.operator) && (
                                                        <>{' '}<span className="font-mono text-zinc-300">{t.thresholdValue}</span></>
                                                    )}
                                                </>
                                            )}
                                        </p>
                                        <p className="text-xs text-zinc-600">{t.severity}</p>
                                    </div>
                                </div>
                                {!readOnly && (
                                    <div className="flex items-center gap-1">
                                        <button
                                            onClick={() => startEditing(t)}
                                            className="p-1.5 rounded-lg text-zinc-600 hover:text-zinc-300 hover:bg-zinc-800 transition-colors cursor-pointer"
                                            title="Edit threshold"
                                        >
                                            <Pencil className="w-4 h-4" />
                                        </button>
                                        <button
                                            onClick={() => handleDelete(t.id)}
                                            disabled={deletingId === t.id}
                                            className="p-1.5 rounded-lg text-zinc-600 hover:text-red-400 hover:bg-red-500/10 disabled:opacity-50 transition-colors cursor-pointer"
                                            title="Delete threshold"
                                        >
                                            {deletingId === t.id ? (
                                                <Loader2 className="w-4 h-4 animate-spin" />
                                            ) : (
                                                <Trash2 className="w-4 h-4" />
                                            )}
                                        </button>
                                    </div>
                                )}
                            </div>
                        ),
                    )}
                </div>
            )}

            {/* Create form */}
            {showForm && (
                <form
                    onSubmit={handleCreate}
                    className="p-4 rounded-lg bg-zinc-900/60 border border-zinc-800/40 space-y-3"
                >
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-medium text-zinc-400 mb-1.5">Metric</label>
                            <select
                                value={form.metricType}
                                onChange={(e) => setForm({ ...form, metricType: e.target.value as MetricType })}
                                className={`w-full ${selectClasses}`}
                            >
                                {METRIC_OPTIONS.map((m) => (
                                    <option key={m.value} value={m.value}>
                                        {m.label}
                                    </option>
                                ))}
                            </select>
                        </div>
                        {isDeviceOffline(form.metricType) ? (
                            <div className="col-span-2">
                                <p className="text-xs text-zinc-500 italic px-1 py-2">
                                    Opt-in for offline alerts. The timeout duration is configured globally in System Settings.
                                </p>
                            </div>
                        ) : (
                            <div>
                                <label className="block text-xs font-medium text-zinc-400 mb-1.5">Operator</label>
                                <select
                                    value={form.operator}
                                    onChange={(e) => setForm({ ...form, operator: e.target.value as Operator })}
                                    className={`w-full ${selectClasses}`}
                                >
                                    {OPERATOR_OPTIONS.map((o) => (
                                        <option key={o.value} value={o.value}>
                                            {o.label} ({o.value.replace('_', ' ').toLowerCase()})
                                        </option>
                                    ))}
                                </select>
                            </div>
                        )}
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                        {!isDeviceOffline(form.metricType) && !isNullOperator(form.operator) && (
                            <div>
                                <label className="block text-xs font-medium text-zinc-400 mb-1.5">Value</label>
                                <input
                                    type="number"
                                    step="any"
                                    required
                                    value={form.thresholdValue}
                                    onChange={(e) => setForm({ ...form, thresholdValue: parseFloat(e.target.value) })}
                                    className="w-full px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-zinc-100 text-sm outline-none focus:border-zinc-600 focus:ring-1 focus:ring-zinc-700"
                                />
                            </div>
                        )}
                        <div>
                            <label className="block text-xs font-medium text-zinc-400 mb-1.5">Severity</label>
                            <select
                                value={form.severity}
                                onChange={(e) => setForm({ ...form, severity: e.target.value as Severity })}
                                className={`w-full ${selectClasses}`}
                            >
                                <option value="WARNING">Warning</option>
                                <option value="CRITICAL">Critical</option>
                            </select>
                        </div>
                    </div>
                    <div className="flex justify-end gap-2 pt-1">
                        <button
                            type="button"
                            onClick={() => setShowForm(false)}
                            className="px-3 py-1.5 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={creating}
                            className="px-3 py-1.5 rounded-lg bg-zinc-100 text-zinc-950 text-sm font-medium hover:bg-zinc-200 disabled:opacity-50 transition-colors flex items-center gap-1.5 cursor-pointer"
                        >
                            {creating ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Plus className="w-3.5 h-3.5" />}
                            Add
                        </button>
                    </div>
                </form>
            )}

            {/* Add button */}
            {!loading && !showForm && !readOnly && (
                <button
                    onClick={() => setShowForm(true)}
                    className="flex items-center gap-2 px-3.5 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 border border-zinc-800 transition-colors cursor-pointer"
                >
                    <Plus className="w-4 h-4" />
                    Add Threshold
                </button>
            )}
        </div>
    );
}
