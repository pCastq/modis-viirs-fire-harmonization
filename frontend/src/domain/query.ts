import type {
  AreaRequest,
  ConfidenceFilter,
  DayNightFilter,
  Mode,
  SourceTypeFilter,
} from '@/api/types';
import { ringToGeometry, type LonLat } from './geometry';

export type AreaSelection =
  { kind: 'country'; code: string } | { kind: 'polygon'; ring: readonly LonLat[] };

export interface Filters {
  mode: Mode;
  dayNight: DayNightFilter;
  minConfidence: ConfidenceFilter;
  sourceType: SourceTypeFilter;
}

/** `null` = tutto il periodo coperto dai dati. */
export interface Period {
  from: string | null;
  to: string | null;
}

/** Cio' che l'utente sta cercando: area + periodo + filtri. */
export interface SearchQuery {
  area: AreaSelection;
  period: Period;
  filters: Filters;
}

/** Il punto del calendario su cui si sta approfondendo. */
export interface Focus {
  /** `AAAA-MM` */
  month: string | null;
  /** `AAAA-MM-GG`, dentro `month` */
  date: string | null;
}

export const DEFAULT_FILTERS: Filters = {
  mode: 'HARMONIZED',
  dayNight: 'ANY',
  minConfidence: 'NOMINAL',
  sourceType: 'VEGETATION',
};

export const DEFAULT_COUNTRY = 'Argentina';

export const DEFAULT_QUERY: SearchQuery = {
  area: { kind: 'country', code: DEFAULT_COUNTRY },
  period: { from: null, to: null },
  filters: DEFAULT_FILTERS,
};

export const NO_FOCUS: Focus = { month: null, date: null };

/** Traduce la ricerca nel corpo delle richieste; i campi di dettaglio si aggiungono con `extra`. */
export function toRequest(query: SearchQuery, extra: Partial<AreaRequest> = {}): AreaRequest {
  const { area, period, filters } = query;
  return {
    ...(area.kind === 'country'
      ? { boundaryCode: area.code }
      : { geometry: ringToGeometry(area.ring) }),
    ...(period.from ? { from: period.from } : {}),
    ...(period.to ? { to: period.to } : {}),
    ...filters,
    ...extra,
  };
}

export function areaLabel(area: AreaSelection): string {
  return area.kind === 'country' ? area.code.replaceAll('_', ' ') : 'Area disegnata';
}
