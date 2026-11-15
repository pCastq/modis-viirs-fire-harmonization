import type { FactorDayNight, FactorSource, HarmonizationFactor } from '@/api/types';

/** Le colonne della tabella dei k, nell'ordine in cui si mostrano. */
export const DAY_NIGHT_COLUMNS: readonly FactorDayNight[] = ['ANY', 'DAY', 'NIGHT'];

export const QUARTERS = [1, 2, 3, 4] as const;

/** Mesi (1-12) di un trimestre: servono per scrivere "gen-mar" nella lingua scelta. */
export function quarterMonths(quarter: number): readonly [number, number] {
  const first = (quarter - 1) * 3 + 1;
  return [first, first + 2];
}

export interface FactorRow {
  quarter: number;
  cells: Record<FactorDayNight, HarmonizationFactor | null>;
}

/** Una riga per trimestre, una cella per filtro giorno/notte: `null` se l'API non l'ha mandata. */
export function factorRows(factors: readonly HarmonizationFactor[]): FactorRow[] {
  return QUARTERS.map((quarter) => {
    const cells = { ANY: null, DAY: null, NIGHT: null } as FactorRow['cells'];
    for (const factor of factors) {
      if (factor.quarter === quarter) cells[factor.dayNight] = factor;
    }
    return { quarter, cells };
  });
}

/** Vero se il k e' preso da un livello piu' grosso del paese (poche osservazioni nel trimestre). */
export function isBorrowed(source: FactorSource | null): boolean {
  return source === 'REGION' || source === 'GLOBAL';
}

/** Vero se almeno un k e' stato calcolato: altrimenti la calibrazione manca. */
export function hasAnyFactor(factors: readonly HarmonizationFactor[]): boolean {
  return factors.some((f) => f.k !== null);
}
