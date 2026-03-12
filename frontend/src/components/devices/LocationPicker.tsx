import { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { MapContainer, TileLayer, Marker, useMapEvents, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapPin, Maximize, Minimize } from 'lucide-react';

// Leaflet default marker icon fix for bundlers
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
    onChange: (lat: number, lng: number) => void;
}

/** Handles map click → set marker position */
function ClickHandler({ onChange }: { onChange: (lat: number, lng: number) => void }) {
    useMapEvents({
        click(e) {
            onChange(e.latlng.lat, e.latlng.lng);
        },
    });
    return null;
}

/** Re-centers the map when coordinates change externally (e.g. manual typing) */
function MapSync({ lat, lng }: { lat: number; lng: number }) {
    const map = useMap();
    useEffect(() => {
        map.setView([lat, lng], map.getZoom(), { animate: true });
    }, [lat, lng, map]);
    return null;
}


/** Invalidates map size when fullscreen changes (Leaflet needs this) */
function MapResizer({ isFullscreen }: { isFullscreen: boolean }) {
    const map = useMap();
    useEffect(() => {
        // Small delay to let the DOM settle before invalidating
        const timeout = setTimeout(() => map.invalidateSize(), 100);
        return () => clearTimeout(timeout);
    }, [isFullscreen, map]);
    return null;
}

export default function LocationPicker({ latitude, longitude, onChange }: Props) {
    const containerRef = useRef<HTMLDivElement>(null);
    const markerRef = useRef<L.Marker>(null);
    const hasCoords = latitude != null && longitude != null;
    const [isFullscreen, setIsFullscreen] = useState(false);

    const center: L.LatLngExpression = hasCoords
        ? [latitude, longitude]
        : [48.8566, 2.3522]; // Default: Paris

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

    const eventHandlers = useMemo(
        () => ({
            dragend() {
                const marker = markerRef.current;
                if (marker) {
                    const pos = marker.getLatLng();
                    onChange(pos.lat, pos.lng);
                }
            },
        }),
        [onChange],
    );

    return (
        <div className="space-y-2">
            <div className="flex items-center gap-1.5">
                <MapPin className="w-3.5 h-3.5 text-zinc-500" />
                <span className="text-xs font-medium text-zinc-400">Location</span>
                {!hasCoords && (
                    <span className="text-[10px] text-zinc-600 ml-1">Click map to set</span>
                )}
            </div>
            <div
                ref={containerRef}
                className={`relative overflow-hidden border border-zinc-800/40 group/map ${isFullscreen
                    ? 'h-screen rounded-none bg-zinc-950'
                    : 'h-[180px] rounded-lg'
                    }`}
            >
                <MapContainer
                    center={center}
                    zoom={hasCoords ? 13 : 4}
                    scrollWheelZoom={true}
                    zoomControl={false}
                    attributionControl={false}
                    style={{ height: '100%', width: '100%' }}
                >
                    <TileLayer url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png" />
                    <ClickHandler onChange={onChange} />
                    <MapResizer isFullscreen={isFullscreen} />
                    {hasCoords && (
                        <>
                            <MapSync lat={latitude} lng={longitude} />
                            <Marker
                                position={[latitude, longitude]}
                                icon={markerIcon}
                                draggable
                                ref={markerRef}
                                eventHandlers={eventHandlers}
                            />
                        </>
                    )}
                </MapContainer>

                {/* Fullscreen toggle button */}
                <button
                    type="button"
                    onClick={toggleFullscreen}
                    className={`absolute top-2 right-2 z-[1000] p-1.5 rounded-lg bg-zinc-900/80 border border-zinc-700/50 text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800 transition-all duration-200 cursor-pointer ${isFullscreen
                        ? 'opacity-100'
                        : 'opacity-0 group-hover/map:opacity-100'
                        }`}
                    title={isFullscreen ? 'Exit fullscreen' : 'View fullscreen'}
                >
                    {isFullscreen ? (
                        <Minimize className="w-3.5 h-3.5" />
                    ) : (
                        <Maximize className="w-3.5 h-3.5" />
                    )}
                </button>
            </div>
        </div>
    );
}
