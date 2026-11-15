import { INTL_TAG, type Locale } from '@/i18n/locales';
import { parseMonthKey } from './months';

export const EM_DASH = '—';

export function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export interface Formatters {
  /** 12345 -> "12,345" / "12.345"; `null` -> trattino (mai zero). */
  count(value: number | null | undefined): string;
  /** 61234 -> "61.2K" / "61,2K"; per le etichette dentro le celle. */
  compact(value: number | null | undefined): string;
  decimal(value: number | null | undefined): string;
  /** Moltiplicatore k con due decimali: 1.933 -> "1.93" / "1,93". */
  factor(value: number | null | undefined): string;
  /** 0.35 -> "35%". */
  percent(fraction: number | null | undefined): string;
  /** Rapporto con segno: 1.8 -> "+80%", 0.6 -> "-40%". */
  signedPercent(ratio: number | null | undefined): string;
  /** "August" / "agosto" */
  monthName(month: number): string;
  /** "Aug" / "ago" */
  monthShort(month: number): string;
  /** "2019-08" -> "August 2019" / "agosto 2019" */
  monthKey(key: string): string;
}

/**
 * Formati numerici e nomi dei mesi per una lingua. Il formato compatto e' scritto a mano (non
 * `Intl` compatto): quello di `Intl` cambia da browser a browser e fra una versione e l'altra.
 */
export function createFormatters(locale: Locale): Formatters {
  const tag = INTL_TAG[locale];
  const integer = new Intl.NumberFormat(tag, { maximumFractionDigits: 0, useGrouping: 'always' });
  const oneDecimal = new Intl.NumberFormat(tag, { maximumFractionDigits: 1 });
  const twoDecimals = new Intl.NumberFormat(tag, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  const longMonth = new Intl.DateTimeFormat(tag, { month: 'long', timeZone: 'UTC' });
  const shortMonth = new Intl.DateTimeFormat(tag, { month: 'short', timeZone: 'UTC' });
  const firstOfMonth = (month: number) => new Date(Date.UTC(2000, month - 1, 1));

  const formatters: Formatters = {
    count: (value) => (value == null ? EM_DASH : integer.format(Math.round(value))),

    compact(value) {
      if (value == null) return EM_DASH;
      const abs = Math.abs(value);
      if (abs < 1000) return integer.format(Math.round(value));
      const sign = value < 0 ? '-' : '';
      const thousands = Math.round(abs / 100) / 10;
      if (thousands < 1000) return `${sign}${oneDecimal.format(thousands)}K`;
      return `${sign}${oneDecimal.format(Math.round(abs / 100_000) / 10)}M`;
    },

    decimal: (value) => (value == null ? EM_DASH : oneDecimal.format(value)),

    factor: (value) => (value == null ? EM_DASH : twoDecimals.format(value)),

    percent: (fraction) =>
      fraction == null ? EM_DASH : `${integer.format(Math.round(fraction * 100))}%`,

    signedPercent(ratio) {
      if (ratio == null) return EM_DASH;
      const pct = Math.round((ratio - 1) * 100);
      const sign = pct > 0 ? '+' : pct < 0 ? '−' : '';
      return `${sign}${integer.format(Math.abs(pct))}%`;
    },

    monthName: (month) => longMonth.format(firstOfMonth(month)),
    monthShort: (month) => shortMonth.format(firstOfMonth(month)),

    monthKey(key) {
      const parsed = parseMonthKey(key);
      return parsed ? `${formatters.monthName(parsed.month)} ${parsed.year}` : key;
    },
  };
  return formatters;
}
