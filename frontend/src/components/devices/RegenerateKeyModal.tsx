import { useState } from 'react';
import { Loader2, Copy, Check, KeyRound, Terminal } from 'lucide-react';
import Modal from '../ui/Modal';
import { regenerateApiKey, type DeviceWithApiKeyModel } from '../../api/deviceService';
import { toast } from 'sonner';

interface Props {
    open: boolean;
    deviceId: string | null;
    deviceName: string;
    onClose: () => void;
}

export default function RegenerateKeyModal({ open, deviceId, deviceName, onClose }: Props) {
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<DeviceWithApiKeyModel | null>(null);
    const [copiedKey, setCopiedKey] = useState(false);
    const [copiedCmd, setCopiedCmd] = useState(false);

    const handleClose = () => {
        setResult(null);
        setLoading(false);
        setCopiedKey(false);
        setCopiedCmd(false);
        onClose();
    };

    const handleRegenerate = async () => {
        if (!deviceId) return;
        setLoading(true);
        try {
            const data = await regenerateApiKey(deviceId);
            setResult(data);
            toast.success('API key regenerated');
        } catch {
            toast.error('Failed to regenerate API key');
            handleClose();
        } finally {
            setLoading(false);
        }
    };

    const handleCopyKey = async () => {
        if (!result) return;
        await navigator.clipboard.writeText(result.apiKey);
        setCopiedKey(true);
        setTimeout(() => setCopiedKey(false), 2000);
    };

    const handleCopyCmd = async () => {
        if (!result?.setupCommand) return;
        await navigator.clipboard.writeText(result.setupCommand);
        setCopiedCmd(true);
        setTimeout(() => setCopiedCmd(false), 2000);
    };

    return (
        <Modal open={open} onClose={handleClose} title="Regenerate API Key" maxWidth="max-w-lg">
            {result ? (
                <div className="space-y-5">
                    <div className="px-4 py-3 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-400 text-sm">
                        Save this new API key — the old key is now invalid and this key won't be shown again.
                    </div>

                    {/* API Key */}
                    <div>
                        <label className="flex items-center gap-1.5 text-xs font-medium text-zinc-500 mb-1.5">
                            <KeyRound className="w-3.5 h-3.5" />
                            New API Key
                        </label>
                        <div className="flex items-center gap-2">
                            <code className="flex-1 px-3.5 py-2.5 rounded-lg bg-zinc-950/80 border border-zinc-800 text-amber-400 text-sm font-mono truncate">
                                {result.apiKey}
                            </code>
                            <button
                                onClick={handleCopyKey}
                                className="shrink-0 p-2.5 rounded-lg border border-zinc-800 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer"
                                title="Copy API key"
                            >
                                {copiedKey ? (
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
                                    onClick={handleCopyCmd}
                                    className="shrink-0 p-2.5 rounded-lg border border-zinc-800 text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer mt-0.5"
                                    title="Copy setup command"
                                >
                                    {copiedCmd ? (
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
                <div className="space-y-4">
                    <p className="text-sm text-zinc-400">
                        Are you sure you want to regenerate the API key for{' '}
                        <span className="text-zinc-200 font-medium">{deviceName}</span>?
                        The current key will be immediately invalidated.
                    </p>
                    <div className="flex justify-end gap-2 pt-2">
                        <button
                            onClick={handleClose}
                            className="px-4 py-2 rounded-lg text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800 transition-colors cursor-pointer"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={handleRegenerate}
                            disabled={loading}
                            className="px-4 py-2 rounded-lg bg-amber-500/20 border border-amber-500/30 text-amber-400 text-sm font-medium hover:bg-amber-500/30 disabled:opacity-50 transition-colors flex items-center gap-2 cursor-pointer"
                        >
                            {loading ? (
                                <>
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                    Regenerating…
                                </>
                            ) : (
                                <>
                                    <KeyRound className="w-4 h-4" />
                                    Regenerate
                                </>
                            )}
                        </button>
                    </div>
                </div>
            )}
        </Modal>
    );
}
