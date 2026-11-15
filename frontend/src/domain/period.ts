import { monthKey, parseMonthKey } from './months';
import type { Period } from './query';

/** Gli ultimi `years` anni fino all'ultimo mese con dati, in mesi interi. */
export function lastYearsPeriod(lastMonth: string, firstMonth: string, years: number): Period {
  const last = parseMonthKey(lastMonth);
  if (!last) return { from: null, to: null };
  const from = monthKey(last.year - years + 1, 1);
  return { from: from < firstMonth ? null : from, to: null };
}

export type PeriodPresetId = 'all' | 'last10' | 'last5';

export const PERIOD_PRESETS: readonly {
  id: PeriodPresetId;
  years: number | null;
}[] = [
  { id: 'all', years: null },
  { id: 'last10', years: 10 },
  { id: 'last5', years: 5 },
];

export function periodForPreset(
  years: number | null,
  firstMonth: string,
  lastMonth: string,
): Period {
  return years === null ? { from: null, to: null } : lastYearsPeriod(lastMonth, firstMonth, years);
}

/** `from` <= `to`, quando entrambi ci sono: altrimenti l'API risponde 400. */
export function isOrderedPeriod(period: Period): boolean {
  return !period.from || !period.to || period.from <= period.to;
}
