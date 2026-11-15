/** Geometria dell'area di interesse: tutto in lon/lat WGS84 (gradi decimali). */
import type { PolygonGeometry } from '@/api/types';

export type LonLat = readonly [lon: number, lat: number];

export interface Bounds {
  west: number;
  south: number;
  east: number;
  north: number;
}

export const MIN_POLYGON_VERTICES = 3;

export function boundsToRing(b: Bounds): LonLat[] {
  return [
    [b.west, b.south],
    [b.east, b.south],
    [b.east, b.north],
    [b.west, b.north],
  ];
}

export function ringBounds(ring: readonly LonLat[]): Bounds {
  const lons = ring.map((p) => p[0]);
  const lats = ring.map((p) => p[1]);
  return {
    west: Math.min(...lons),
    east: Math.max(...lons),
    south: Math.min(...lats),
    north: Math.max(...lats),
  };
}

/** L'anello GeoJSON è chiuso: primo e ultimo punto coincidono. */
export function ringToGeometry(ring: readonly LonLat[]): PolygonGeometry {
  const closed = [...ring, ring[0]!];
  return { type: 'Polygon', coordinates: [closed.map((p) => [p[0], p[1]])] };
}

export function geometryToRing(geometry: PolygonGeometry): LonLat[] {
  const outer = geometry.coordinates[0] ?? [];
  const points = outer.map((p): LonLat => [p[0]!, p[1]!]);
  const first = points[0];
  const last = points[points.length - 1];
  return first && last && first[0] === last[0] && first[1] === last[1]
    ? points.slice(0, -1)
    : points;
}

export function isValidLon(lon: number): boolean {
  return Number.isFinite(lon) && lon >= -180 && lon <= 180;
}

export function isValidLat(lat: number): boolean {
  return Number.isFinite(lat) && lat >= -90 && lat <= 90;
}

export type BoundsProblem = 'range' | 'order' | 'empty';

/** Controlla un rettangolo inserito a mano; `null` se è valido. */
export function boundsProblem(b: Bounds): BoundsProblem | null {
  if (![b.west, b.east].every(isValidLon) || ![b.south, b.north].every(isValidLat)) return 'range';
  if (b.west >= b.east || b.south >= b.north) return 'order';
  return null;
}

/** Area approssimata in km2 (rettangolo sulla sfera): serve solo a dare un ordine di grandezza. */
export function approxAreaKm2(b: Bounds): number {
  const R = 6371;
  const toRad = Math.PI / 180;
  const dLon = (b.east - b.west) * toRad;
  return Math.abs(R * R * dLon * (Math.sin(b.north * toRad) - Math.sin(b.south * toRad)));
}

const COORD_DECIMALS = 5; // ~1 m: oltre non ha senso per celle di 1 km

export function roundCoord(value: number): number {
  const f = 10 ** COORD_DECIMALS;
  return Math.round(value * f) / f;
}
