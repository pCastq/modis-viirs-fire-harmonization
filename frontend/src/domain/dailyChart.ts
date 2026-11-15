import type { DayDetail, DayStatus, SatelliteCode } from '@/api/types';
import { SATELLITES } from './satellites';

export interface DayBar {
  date: string;
  day: number;
  total: number;
  /** Segmenti impilati, dal basso, con il satellite e il conteggio. */
  segments: { code: SatelliteCode; count: number }[];
  /** Satelliti che quel giorno non hanno osservato (tutto o in parte). */
  gaps: { code: SatelliteCode; status: Exclude<DayStatus, 'OK'> }[];
}

/** Prepara i dati del grafico dei giorni: conteggi impilati e satelliti mancanti/parziali. */
export function toDayBars(days: readonly DayDetail[]): DayBar[] {
  return days.map((d) => {
    const segments = SATELLITES.flatMap((s) => {
      const count = d.bySatellite[s.code] ?? 0;
      return count > 0 ? [{ code: s.code, count }] : [];
    });
    const gaps = SATELLITES.flatMap((s) => {
      const status = d.status[s.code];
      return status && status !== 'OK' ? [{ code: s.code, status }] : [];
    });
    return {
      date: d.date,
      day: Number(d.date.slice(8, 10)),
      total: segments.reduce((sum, s) => sum + s.count, 0),
      segments,
      gaps,
    };
  });
}

export function maxDayTotal(bars: readonly DayBar[]): number {
  return Math.max(0, ...bars.map((b) => b.total));
}
