import type { CalendarResponse } from '@/api/types';

export interface HeatCell {
  year: number;
  month: number;
  /** `null` = nessun dato osservato (diverso da zero). */
  value: number | null;
  provisional: boolean;
  incomplete: boolean;
  /** Errore relativo atteso del valore (8 casi su 10), `null` se non noto. */
  uncertainty: number | null;
  /** Rapporto con la mediana tipica dello stesso mese, `null` se non calcolabile. */
  ratio: number | null;
}

/** Minimo di anni comparabili per dire cosa e' "tipico" di un mese. */
export const MIN_YEARS_FOR_TYPICAL = 3;

export function median(values: readonly number[]): number | null {
  if (values.length === 0) return null;
  const sorted = [...values].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 === 1 ? sorted[mid]! : (sorted[mid - 1]! + sorted[mid]!) / 2;
}

/**
 * Mediana per mese del calendario, fra i soli mesi osservati e completi (come fa il backend per
 * l'anomalia). È un aiuto di VISUALIZZAZIONE per colorare "rispetto al tipico"; il confronto
 * formale con gli anni di riferimento resta quello di `/anomaly`.
 */
export function typicalByMonth(calendar: CalendarResponse): (number | null)[] {
  return calendar.months.map((_, m) => {
    const comparable: number[] = [];
    calendar.years.forEach((_, y) => {
      const v = calendar.values[y]?.[m];
      if (v != null && !calendar.incomplete[y]?.[m]) comparable.push(v);
    });
    return comparable.length >= MIN_YEARS_FOR_TYPICAL ? median(comparable) : null;
  });
}

export function buildCells(calendar: CalendarResponse): HeatCell[][] {
  const typical = typicalByMonth(calendar);
  return calendar.years.map((year, y) =>
    calendar.months.map((month, m) => {
      const value = calendar.values[y]?.[m] ?? null;
      const reference = typical[m] ?? null;
      return {
        year,
        month,
        value,
        provisional: calendar.provisional[y]?.[m] ?? false,
        incomplete: calendar.incomplete[y]?.[m] ?? false,
        uncertainty: calendar.uncertainty?.[y]?.[m] ?? null,
        ratio: value != null && reference != null && reference > 0 ? value / reference : null,
      };
    }),
  );
}

export function maxValue(cells: readonly (readonly HeatCell[])[]): number {
  let max = 0;
  for (const row of cells) {
    for (const cell of row) {
      if (cell.value != null && !cell.incomplete && cell.value > max) max = cell.value;
    }
  }
  return max;
}

export function yearTotal(row: readonly HeatCell[]): number | null {
  const observed = row.filter((c) => c.value != null);
  return observed.length === 0 ? null : observed.reduce((sum, c) => sum + (c.value ?? 0), 0);
}

/** L'ultimo mese osservato e completo: il più recente confrontabile con gli anni passati. */
export function latestComparableMonth(
  cells: readonly (readonly HeatCell[])[],
): { year: number; month: number } | null {
  for (let y = cells.length - 1; y >= 0; y--) {
    const row = cells[y]!;
    for (let m = row.length - 1; m >= 0; m--) {
      const cell = row[m]!;
      if (cell.value !== null && !cell.incomplete) return { year: cell.year, month: cell.month };
    }
  }
  return null;
}
