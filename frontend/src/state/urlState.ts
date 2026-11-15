/**
 * Lo stato della ricerca vive nell'URL: ogni vista è condivisibile con un link e il tasto
 * "indietro" del browser funziona. Qui solo conversione pura testo <-> stato; nessun effetto.
 */
import { isMonthKey } from '@/domain/months';
import { isValidLat, isValidLon, roundCoord, type LonLat } from '@/domain/geometry';
import {
  DEFAULT_FILTERS,
  DEFAULT_QUERY,
  NO_FOCUS,
  type Filters,
  type Focus,
  type SearchQuery,
} from '@/domain/query';

export interface AppState {
  query: SearchQuery;
  focus: Focus;
}

const MODES = ['HARMONIZED', 'RAW'] as const;
const DAY_NIGHT = ['ANY', 'D', 'N'] as const;
const CONFIDENCE = ['ALL', 'NOMINAL', 'HIGH'] as const;
const SOURCE = ['VEGETATION', 'ALL', 'OTHER'] as const;
const DATE = /^\d{4}-\d{2}-\d{2}$/;
const COUNTRY_CODE = /^[A-Za-z_]{2,40}$/;

function oneOf<T extends string>(allowed: readonly T[], value: string | null, fallback: T): T {
  return allowed.find((a) => a === value) ?? fallback;
}

function parsePolygon(text: string): LonLat[] | null {
  const ring: LonLat[] = [];
  for (const pair of text.split(';')) {
    const [lon, lat] = pair.split(',').map(Number);
    if (lon === undefined || lat === undefined || !isValidLon(lon) || !isValidLat(lat)) return null;
    ring.push([lon, lat]);
  }
  return ring.length >= 3 ? ring : null;
}

function serializePolygon(ring: readonly LonLat[]): string {
  return ring.map(([lon, lat]) => `${roundCoord(lon)},${roundCoord(lat)}`).join(';');
}

function parseFilters(params: URLSearchParams): Filters {
  return {
    mode: oneOf(MODES, params.get('mode'), DEFAULT_FILTERS.mode),
    dayNight: oneOf(DAY_NIGHT, params.get('dn'), DEFAULT_FILTERS.dayNight),
    minConfidence: oneOf(CONFIDENCE, params.get('conf'), DEFAULT_FILTERS.minConfidence),
    sourceType: oneOf(SOURCE, params.get('src'), DEFAULT_FILTERS.sourceType),
  };
}

export function parseState(search: string): AppState {
  const params = new URLSearchParams(search);
  const poly = params.get('poly');
  const ring = poly ? parsePolygon(poly) : null;
  const country = params.get('area');
  const area = ring
    ? ({ kind: 'polygon', ring } as const)
    : country && COUNTRY_CODE.test(country)
      ? ({ kind: 'country', code: country } as const)
      : DEFAULT_QUERY.area;

  const from = params.get('from');
  const to = params.get('to');
  const month = params.get('month');
  const date = params.get('date');
  const focusMonth = month && isMonthKey(month) ? month : null;
  const focusDate =
    focusMonth && date && DATE.test(date) && date.startsWith(focusMonth) ? date : null;

  return {
    query: {
      area,
      period: {
        from: from && isMonthKey(from) ? from : null,
        to: to && isMonthKey(to) ? to : null,
      },
      filters: parseFilters(params),
    },
    focus: focusMonth ? { month: focusMonth, date: focusDate } : NO_FOCUS,
  };
}

function isDefaultCountry(code: string): boolean {
  return DEFAULT_QUERY.area.kind === 'country' && DEFAULT_QUERY.area.code === code;
}

/** Scrive solo cio' che si discosta dai valori predefiniti: gli URL restano corti e leggibili. */
export function serializeState({ query, focus }: AppState): string {
  const params = new URLSearchParams();
  if (query.area.kind === 'polygon') {
    params.set('poly', serializePolygon(query.area.ring));
  } else if (!isDefaultCountry(query.area.code)) {
    params.set('area', query.area.code);
  }
  if (query.period.from) params.set('from', query.period.from);
  if (query.period.to) params.set('to', query.period.to);
  const f = query.filters;
  if (f.mode !== DEFAULT_FILTERS.mode) params.set('mode', f.mode);
  if (f.dayNight !== DEFAULT_FILTERS.dayNight) params.set('dn', f.dayNight);
  if (f.minConfidence !== DEFAULT_FILTERS.minConfidence) params.set('conf', f.minConfidence);
  if (f.sourceType !== DEFAULT_FILTERS.sourceType) params.set('src', f.sourceType);
  if (focus.month) params.set('month', focus.month);
  if (focus.date) params.set('date', focus.date);
  const text = params.toString();
  return text ? `?${text}` : '';
}
