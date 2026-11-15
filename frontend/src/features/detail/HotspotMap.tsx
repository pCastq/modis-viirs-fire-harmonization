import L from 'leaflet';
import { useEffect } from 'react';
import type { HotspotFeature } from '@/api/types';
import { useLeafletMap } from '@/components/map/useLeafletMap';
import { satelliteStyle } from '@/domain/satellites';
import { useI18n } from '@/i18n/I18nProvider';
import type { Translator } from '@/i18n/translate';
import styles from './HotspotMap.module.css';

interface Props {
  features: readonly HotspotFeature[];
}

function popupHtml(f: HotspotFeature, t: Translator): string {
  const p = f.properties;
  const rows: [string, string][] = [
    [t('detail.popup.satellite'), satelliteStyle(p.satellite).label],
    [t('detail.popup.time'), p.timeUtc],
    [t('detail.popup.dayNight'), t(p.dayNight === 'D' ? 'detail.popup.day' : 'detail.popup.night')],
    [t('detail.popup.confidence'), String(p.confidence)],
    [t('detail.popup.frp'), p.frpMw == null ? t('detail.popup.notAvailable') : `${p.frpMw} MW`],
    [t('detail.popup.type'), p.type],
  ];
  // i valori vengono dall'API ma si escludono comunque come testo, mai come HTML
  const div = document.createElement('div');
  rows.forEach(([k, v]) => {
    const line = document.createElement('div');
    const key = document.createElement('strong');
    key.textContent = `${k}: `;
    line.append(key, document.createTextNode(v));
    div.append(line);
  });
  return div.innerHTML;
}

/** I punti di un giorno, colorati per satellite. */
export function HotspotMap({ features }: Props) {
  const { t } = useI18n();
  const { containerRef, map } = useLeafletMap();

  useEffect(() => {
    if (!map) return undefined;
    const group = L.layerGroup(
      features.map((f) => {
        const [lon, lat] = f.geometry.coordinates;
        const color = satelliteStyle(f.properties.satellite).color;
        return L.circleMarker([lat, lon], {
          radius: 5,
          color: '#111',
          weight: 1,
          fillColor: color,
          fillOpacity: 0.85,
        }).bindPopup(popupHtml(f, t));
      }),
    ).addTo(map);
    if (features.length > 0) {
      const bounds = L.latLngBounds(
        features.map(
          (f) => [f.geometry.coordinates[1], f.geometry.coordinates[0]] as L.LatLngTuple,
        ),
      );
      map.fitBounds(bounds, { padding: [30, 30], maxZoom: 10, animate: false });
    }
    return () => {
      group.remove();
    };
  }, [map, features, t]);

  return (
    <div
      ref={containerRef}
      className={styles.map}
      role="application"
      aria-label={t('detail.mapLabel')}
    />
  );
}
