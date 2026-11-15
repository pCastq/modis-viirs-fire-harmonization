import type L from 'leaflet';
import { useEffect, useRef, useState } from 'react';
import { createBaseMap } from './leafletSetup';

/**
 * Crea la mappa Leaflet nel contenitore e la distrugge allo smontaggio.
 * Restituisce la mappa solo quando esiste, così gli effetti dei layer non devono controllare i `null`.
 */
export function useLeafletMap() {
  const containerRef = useRef<HTMLDivElement>(null);
  const [map, setMap] = useState<L.Map | null>(null);

  useEffect(() => {
    const element = containerRef.current;
    if (!element) return undefined;
    const created = createBaseMap(element);
    setMap(created);
    // il contenitore puo' cambiare dimensione dopo la creazione (layout, caricamenti): Leaflet va avvisato
    const observer = new ResizeObserver(() => created.invalidateSize());
    observer.observe(element);
    return () => {
      observer.disconnect();
      created.remove();
      setMap(null);
    };
  }, []);

  return { containerRef, map };
}
