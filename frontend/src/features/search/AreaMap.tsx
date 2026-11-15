import L from 'leaflet';
import { useEffect } from 'react';
import { AREA_STYLE, DRAFT_STYLE } from '@/components/map/leafletSetup';
import { useLeafletMap } from '@/components/map/useLeafletMap';
import type { DrawMode } from '@/domain/drawing';
import type { Bounds, LonLat } from '@/domain/geometry';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './AreaMap.module.css';

interface Props {
  /** Area scelta: un poligono, oppure solo il rettangolo limite di un paese. */
  ring: readonly LonLat[] | null;
  bounds: Bounds | null;
  drawMode: DrawMode | null;
  draftPoints: readonly LonLat[];
  onPoint: (point: LonLat) => void;
}

const toLatLng = ([lon, lat]: LonLat): L.LatLngTuple => [lat, lon];

export function AreaMap({ ring, bounds, drawMode, draftPoints, onPoint }: Props) {
  const { t } = useI18n();
  const { containerRef, map } = useLeafletMap();

  // area scelta: il poligono se c'e', altrimenti il rettangolo limite del paese
  useEffect(() => {
    if (!map) return undefined;
    const layer = ring
      ? L.polygon(ring.map(toLatLng), AREA_STYLE)
      : bounds
        ? L.rectangle(
            [
              [bounds.south, bounds.west],
              [bounds.north, bounds.east],
            ],
            AREA_STYLE,
          )
        : null;
    if (!layer) return undefined;
    layer.addTo(map);
    map.fitBounds(layer.getBounds(), { padding: [24, 24], maxZoom: 9, animate: false });
    return () => {
      layer.remove();
    };
  }, [map, ring, bounds]);

  // clic sulla mappa solo mentre si disegna
  useEffect(() => {
    if (!map || !drawMode) return undefined;
    const handler = (event: L.LeafletMouseEvent) => onPoint([event.latlng.lng, event.latlng.lat]);
    map.on('click', handler);
    map.getContainer().style.cursor = 'crosshair';
    return () => {
      map.off('click', handler);
      map.getContainer().style.cursor = '';
    };
  }, [map, drawMode, onPoint]);

  // bozza del disegno in corso
  useEffect(() => {
    if (!map || draftPoints.length === 0) return undefined;
    const latlngs = draftPoints.map(toLatLng);
    const group = L.layerGroup([
      drawMode === 'polygon' && latlngs.length > 2
        ? L.polygon(latlngs, DRAFT_STYLE)
        : L.polyline(latlngs, DRAFT_STYLE),
      ...latlngs.map((p) => L.circleMarker(p, { radius: 4, color: '#2563eb', fillOpacity: 1 })),
    ]).addTo(map);
    return () => {
      group.remove();
    };
  }, [map, draftPoints, drawMode]);

  return (
    <div
      ref={containerRef}
      className={styles.map}
      role="application"
      aria-label={t('search.area.mapLabel')}
    />
  );
}
