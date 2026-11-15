/**
 * Scale di colore accessibili (ColorBrewer, sicure per il daltonismo):
 * - sequenziale YlOrRd per i conteggi (poco = chiaro, molto = scuro e caldo);
 * - divergente PuOr per le anomalie (viola = sotto il tipico, arancio = sopra).
 * Il valore non è mai codificato solo dal colore: le celle riportano anche il numero.
 */

type Rgb = readonly [number, number, number];

const SEQUENTIAL: readonly Rgb[] = [
  [255, 255, 204],
  [255, 237, 160],
  [254, 217, 118],
  [254, 178, 76],
  [253, 141, 60],
  [252, 78, 42],
  [227, 26, 28],
  [189, 0, 38],
  [128, 0, 38],
];

const DIVERGING: readonly Rgb[] = [
  [84, 39, 136],
  [128, 115, 172],
  [178, 171, 210],
  [216, 218, 235],
  [247, 247, 247],
  [254, 224, 182],
  [253, 184, 99],
  [224, 130, 20],
  [179, 88, 6],
];

function clamp01(t: number): number {
  return Math.min(1, Math.max(0, t));
}

function interpolate(stops: readonly Rgb[], t: number): Rgb {
  const x = clamp01(t) * (stops.length - 1);
  const i = Math.min(Math.floor(x), stops.length - 2);
  const f = x - i;
  const a = stops[i]!;
  const b = stops[i + 1]!;
  return [a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f];
}

export function toCss([r, g, b]: Rgb): string {
  return `rgb(${Math.round(r)} ${Math.round(g)} ${Math.round(b)})`;
}

/** Luminanza relativa WCAG, 0 (nero) - 1 (bianco). */
export function luminance([r, g, b]: Rgb): number {
  const lin = (c: number) => {
    const s = c / 255;
    return s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
}

const DARK_TEXT: Rgb = [0, 0, 0];

function contrastRatio(a: number, b: number): number {
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
}

/** Bianco o nero: quello che ha più contrasto (WCAG) sullo sfondo dato. */
export function readableTextOn(background: Rgb): string {
  const bg = luminance(background);
  return contrastRatio(bg, 1) >= contrastRatio(bg, luminance(DARK_TEXT)) ? '#ffffff' : '#000000';
}

export interface CellPaint {
  background: string;
  text: string;
}

function paint(rgb: Rgb): CellPaint {
  return { background: toCss(rgb), text: readableTextOn(rgb) };
}

/**
 * Conteggi: radice quadrata, perché i conteggi di fuoco hanno code lunghissime e una scala
 * lineare lascerebbe quasi tutti i mesi dello stesso chiaro.
 */
export function sequentialPaint(value: number, max: number): CellPaint {
  if (max <= 0) return paint(SEQUENTIAL[0]!);
  return paint(interpolate(SEQUENTIAL, Math.sqrt(clamp01(value / max))));
}

/** Gradiente CSS per la legenda: stesse tappe della scala. */
export function sequentialGradient(): string {
  return `linear-gradient(to right, ${SEQUENTIAL.map(toCss).join(', ')})`;
}

export function divergingGradient(): string {
  return `linear-gradient(to right, ${DIVERGING.map(toCss).join(', ')})`;
}

/** Limite del rapporto mostrato dalla scala divergente: oltre 4x (o sotto 1/4) il colore satura. */
export const DIVERGING_LIMIT = 4;

/** Rapporto con il tipico: 1 = bianco, 4x = arancio pieno, 1/4 = viola pieno (scala logaritmica). */
export function divergingPaint(ratio: number): CellPaint {
  const safe = Math.max(ratio, 1 / (DIVERGING_LIMIT * 4));
  const t = 0.5 + Math.log(safe) / (2 * Math.log(DIVERGING_LIMIT));
  return paint(interpolate(DIVERGING, t));
}
