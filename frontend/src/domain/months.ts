/** Mesi nel formato `AAAA-MM` (lo stesso dell'API). I nomi dei mesi dipendono dalla lingua: vedi `format.ts`. */

const MONTH_KEY = /^(\d{4})-(0[1-9]|1[0-2])$/;

export function monthKey(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, '0')}`;
}

export function isMonthKey(text: string): boolean {
  return MONTH_KEY.test(text);
}

export function parseMonthKey(key: string): { year: number; month: number } | null {
  const match = MONTH_KEY.exec(key);
  return match ? { year: Number(match[1]), month: Number(match[2]) } : null;
}

/** Il primo giorno del mese successivo non serve: basta il numero di giorni. */
export function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

/** Limita `key` all'intervallo [min, max]; l'ordine lessicografico coincide con quello cronologico. */
export function clampMonthKey(key: string, min: string, max: string): string {
  if (key < min) return min;
  if (key > max) return max;
  return key;
}
