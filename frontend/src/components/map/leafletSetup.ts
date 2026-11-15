import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

/** Mappa di base: tiles OpenStreetMap (con attribuzione obbligatoria). */
export function createBaseMap(element: HTMLElement): L.Map {
  const map = L.map(element, { worldCopyJump: true, zoomSnap: 0.25 }).setView([-20, -60], 3);
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  }).addTo(map);
  return map;
}

export const AREA_STYLE: L.PathOptions = {
  color: '#c2410c',
  weight: 2,
  fillColor: '#fb923c',
  fillOpacity: 0.18,
};

export const DRAFT_STYLE: L.PathOptions = {
  color: '#2563eb',
  weight: 2,
  dashArray: '6 4',
  fillOpacity: 0.1,
};
