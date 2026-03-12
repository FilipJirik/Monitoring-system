import { useRef, useState, useEffect, useCallback } from 'react';
import { Camera, Monitor, Loader2, Trash2, Maximize, Minimize } from 'lucide-react';
import { uploadDeviceImage, deleteDeviceImage } from '../../api/deviceService';
import { toast } from 'sonner';

interface Props {
    deviceId: string;
    imageFilename?: string;
    onUploaded: () => void;
    readOnly?: boolean;
}

export default function DeviceImage({ deviceId, imageFilename, onUploaded, readOnly = false }: Props) {
    const fileRef = useRef<HTMLInputElement>(null);
    const containerRef = useRef<HTMLDivElement>(null);
    const [uploading, setUploading] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [isFullscreen, setIsFullscreen] = useState(false);

    const imageUrl = imageFilename ? `/api/images/${imageFilename}` : null;

    // Listen for fullscreenchange so ESC also updates state
    useEffect(() => {
        const handler = () => {
            setIsFullscreen(!!document.fullscreenElement);
        };
        document.addEventListener('fullscreenchange', handler);
        return () => document.removeEventListener('fullscreenchange', handler);
    }, []);

    const toggleFullscreen = useCallback(() => {
        if (document.fullscreenElement) {
            document.exitFullscreen();
        } else {
            containerRef.current?.requestFullscreen?.();
        }
    }, []);

    const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setUploading(true);
        try {
            await uploadDeviceImage(deviceId, file);
            toast.success('Image updated');
            onUploaded();
        } catch {
            toast.error('Failed to upload image');
        } finally {
            setUploading(false);
            if (fileRef.current) fileRef.current.value = '';
        }
    };

    const handleDelete = async () => {
        setDeleting(true);
        try {
            await deleteDeviceImage(deviceId);
            toast.success('Image removed');
            onUploaded();
        } catch {
            toast.error('Failed to remove image');
        } finally {
            setDeleting(false);
        }
    };

    return (
        <div
            ref={containerRef}
            className={`relative group ${isFullscreen ? 'bg-zinc-950 flex items-center justify-center h-screen w-screen' : ''}`}
        >
            <div
                className={`overflow-hidden bg-zinc-900/60 border border-zinc-800/40 flex justify-center items-center ${isFullscreen ? 'w-full h-full rounded-none border-0' : 'aspect-[4/3] rounded-xl'}`}
            >
                {imageUrl ? (
                    <img
                        src={imageUrl}
                        alt="Device"
                        className={`object-contain mx-auto ${isFullscreen ? 'max-w-full max-h-full' : 'max-w-full max-h-full'}`}
                    />
                ) : (
                    <Monitor className="w-12 h-12 text-zinc-800" />
                )}
            </div>

            {/* Fullscreen toggle button */}
            {imageUrl && (
                <button
                    onClick={toggleFullscreen}
                    className={`absolute top-2 right-2 z-[1000] p-1.5 rounded-lg bg-zinc-900/80 border border-zinc-700/50 text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800 transition-all duration-200 cursor-pointer ${isFullscreen ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}
                    title={isFullscreen ? 'Exit fullscreen' : 'View fullscreen'}
                >
                    {isFullscreen ? (
                        <Minimize className="w-3.5 h-3.5" />
                    ) : (
                        <Maximize className="w-3.5 h-3.5" />
                    )}
                </button>
            )}

            {/* Hover overlay with action buttons */}
            {!isFullscreen && !readOnly && (
                <div className="absolute inset-0 rounded-xl flex items-center justify-center gap-2 bg-black/0 group-hover:bg-black/50 transition-all duration-200 pointer-events-none">
                    {/* Change Image */}
                    <button
                        onClick={() => fileRef.current?.click()}
                        disabled={uploading || deleting}
                        className="opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-zinc-900/90 border border-zinc-700/50 text-zinc-300 text-xs font-medium pointer-events-auto cursor-pointer hover:bg-zinc-800"
                    >
                        {uploading ? (
                            <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Uploading…</>
                        ) : (
                            <><Camera className="w-3.5 h-3.5" /> Change Image</>
                        )}
                    </button>

                    {/* Delete Image (only when image exists) */}
                    {imageFilename && (
                        <button
                            onClick={handleDelete}
                            disabled={uploading || deleting}
                            className="opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-red-500/15 border border-red-500/30 text-red-400 text-xs font-medium pointer-events-auto cursor-pointer hover:bg-red-500/25 disabled:opacity-50"
                            title="Remove image"
                        >
                            {deleting ? (
                                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            ) : (
                                <Trash2 className="w-3.5 h-3.5" />
                            )}
                        </button>
                    )}
                </div>
            )}

            <input
                ref={fileRef}
                type="file"
                accept="image/*"
                onChange={handleUpload}
                className="hidden"
            />
        </div>
    );
}
