/** Utilita' per i grafici SVG: nessuna libreria, solo la matematica che serve. */

export interface LinearScale {
  (value: number): number;
  domain: readonly [number, number];
  range: readonly [number, number];
}

export function linearScale(
  domain: readonly [number, number],
  range: readonly [number, number],
): LinearScale {
  const [d0, d1] = domain;
  const [r0, r1] = range;
  const span = d1 - d0;
  const scale = ((value: number) =>
    span === 0 ? r0 : r0 + ((value - d0) / span) * (r1 - r0)) as LinearScale;
  scale.domain = domain;
  scale.range = range;
  return scale;
}

const NICE_STEPS = [1, 2, 2.5, 5] as const;

/**
 * Asse con estremo "tondo" sopra `max`: il passo più piccolo fra 1, 2, 2.5, 5 x 10^n che
 * richiede al massimo `maxTicks` intervalli.
 */
export function niceAxis(max: number, maxTicks = 5): { top: number; ticks: number[] } {
  if (!(max > 0)) return { top: 1, ticks: [0, 1] };
  const start = Math.floor(Math.log10(max / maxTicks)) - 1;
  for (let exp = start; ; exp++) {
    for (const base of NICE_STEPS) {
      const step = base * 10 ** exp;
      const intervals = Math.ceil(max / step - 1e-9);
      if (intervals <= maxTicks) {
        const ticks = Array.from({ length: intervals + 1 }, (_, i) =>
          Number((i * step).toFixed(10)),
        );
        return { top: ticks[intervals]!, ticks };
      }
    }
  }
}
