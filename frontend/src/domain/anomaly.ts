export type AnomalyLevel = 'veryHigh' | 'high' | 'typical' | 'low' | 'veryLow';

/** Soglie sullo z-score robusto: oltre 2 e' raro (circa il 5% dei casi), oltre 1 e' notevole. */
export function classifyAnomaly(robustZ: number | null): AnomalyLevel | null {
  if (robustZ === null) return null;
  if (robustZ >= 2) return 'veryHigh';
  if (robustZ >= 1) return 'high';
  if (robustZ <= -2) return 'veryLow';
  if (robustZ <= -1) return 'low';
  return 'typical';
}
