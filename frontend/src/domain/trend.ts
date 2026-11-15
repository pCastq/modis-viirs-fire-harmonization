import type { AnnualTotal, Trend } from '@/api/types';
import { median } from './heatmap';

/** Retta di Sen: pendenza data dal backend, intercetta = mediana di (totale - pendenza x anno). */
export function senLine(
  totals: readonly AnnualTotal[],
  slope: number,
): ((year: number) => number) | null {
  if (totals.length < 2) return null;
  const intercept = median(totals.map((t) => t.total - slope * t.year));
  return intercept === null ? null : (year) => intercept + slope * year;
}

export type TrendDirection = 'up' | 'down' | 'flat';

/** Direzione a parole, ma solo se la tendenza è statisticamente significativa (p < 0,05). */
export function trendDirection(trend: Trend): TrendDirection {
  if (!trend.significantAt5Percent) return 'flat';
  return trend.senSlope > 0 ? 'up' : 'down';
}
