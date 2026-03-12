import { useRef, useState, useEffect, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapPin, Maximize, Minimize } from 'lucide-react';

// Fix default marker icon issue with bundlers
const markerIcon = new L.Icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41],
});

interface Props {
    latitude?: number;
    longitude?: number;
}

/** Enable/disable scroll-wheel zoom based on fullscreen state */
function ScrollZoomController({ enabled }: { enabled: boolean }) {
    const map = useMap();
    useEffect(() => {
        if (enabled) {
            map.scrollWheelZoom.enable();
        } else {
            map.scrollWheelZoom.disable();
        }
    }, [enabled, map]);
    return null;
}

export default function DeviceMap({ latitude, longitude }: Props) {
    const containerRef = useRef<HTMLDivElement>(null);
    const [isFullscreen, setIsFullscreen] = useState(false);

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

    if (latitude == null || longitude == null) {
        return (
            <div className="h-[200px] rounded-xl bg-zinc-900/60 border border-zinc-800/40 flex flex-col items-center justify-center gap-2">
                <MapPin className="w-6 h-6 text-zinc-700" />
                <span className="text-xs text-zinc-600">No location data</span>
            </div>
        );
    }

    return (
        <div>
            <div
                ref={containerRef}
                className={`relative overflow-hidden border border-zinc-800/40 group/map ${isFullscreen ? 'h-screen rounded-none bg-zinc-950' : 'h-[200px] rounded-xl'}`}
            >
                <MapContainer
                    center={[latitude, longitude]}
                    zoom={13}
                    scrollWheelZoom={false}
                    zoomControl={false}
                    attributionControl={false}
                    style={{ height: '100%', width: '100%' }}
                >
                    <TileLayer url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png" />
                    <Marker position={[latitude, longitude]} icon={markerIcon} />
                    <ScrollZoomController enabled={isFullscreen} />
                </MapContainer>

                {/* Fullscreen toggle button */}
                <button
                    onClick={toggleFullscreen}
                    className={`absolute top-2 right-2 z-[1000] p-1.5 rounded-lg bg-zinc-900/80 border border-zinc-700/50 text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800 transition-all duration-200 cursor-pointer ${isFullscreen ? 'opacity-100' : 'opacity-0 group-hover/map:opacity-100'}`}
                    title={isFullscreen ? 'Exit fullscreen' : 'View fullscreen'}
                >
                    {isFullscreen ? (
                        <Minimize className="w-3.5 h-3.5" />
                    ) : (
                        <Maximize className="w-3.5 h-3.5" />
                    )}
                </button>
            </div>

            {/* Subtle coordinates */}
            <p className="text-xs text-zinc-600 text-center mt-1.5">
                Lat: {latitude.toFixed(5)}, Lng: {longitude.toFixed(5)}
            </p>
        </div>
    );
}
