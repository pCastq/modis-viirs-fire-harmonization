import type { SatelliteCode } from '@/api/types';

export interface SatelliteStyle {
  code: SatelliteCode;
  label: string;
  sensor: 'MODIS' | 'VIIRS';
  /** Okabe-Ito: distinguibili anche con daltonismo. */
  color: string;
}

export const SATELLITES: readonly SatelliteStyle[] = [
  { code: 'TERRA', label: 'Terra', sensor: 'MODIS', color: '#0072B2' },
  { code: 'AQUA', label: 'Aqua', sensor: 'MODIS', color: '#56B4E9' },
  { code: 'SNPP', label: 'S-NPP', sensor: 'VIIRS', color: '#D55E00' },
  { code: 'NOAA20', label: 'NOAA-20', sensor: 'VIIRS', color: '#E69F00' },
  { code: 'NOAA21', label: 'NOAA-21', sensor: 'VIIRS', color: '#CC79A7' },
];

const BY_CODE = new Map(SATELLITES.map((s) => [s.code, s]));

export function satelliteStyle(code: SatelliteCode): SatelliteStyle {
  return BY_CODE.get(code) ?? { code, label: code, sensor: 'VIIRS', color: '#666666' };
}
