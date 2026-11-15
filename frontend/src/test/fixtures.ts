import type {
  AnnualTrendResponse,
  AnomalyResponse,
  CalendarResponse,
  Metadata,
  MonthDetailResponse,
  SeasonalProfileResponse,
} from '@/api/types';

const method = { id: 'fixed-factor', version: '1' };

export const metadata: Metadata = {
  firstMonth: '2000-11',
  lastMonth: '2025-12',
  activeMethod: method,
  activeMethodIsProvisional: true,
  availableMethods: [method],
  satellites: [],
  boundaries: [
    {
      code: 'Argentina',
      name: 'Argentina',
      level: 'country',
      west: -73,
      south: -55,
      east: -53,
      north: -21,
    },
    { code: 'Italy', name: 'Italy', level: 'country', west: 6, south: 36, east: 19, north: 47 },
  ],
  lastIngest: null,
  defaultSourceType: 'VEGETATION',
  limits: {
    maxHotspotsPerDay: 5000,
    maxExportRows: 1000000,
    maxPolygonVertices: 500,
    exactResolutionMaxBoxDegrees2: 4,
  },
  notes: [],
};

const falses = [[false, false, false]];

/** Tre mesi del 2019 e due anni vuoti sopra: basta a esercitare griglia, scale e selezione. */
export const calendar: CalendarResponse = {
  area: { label: 'Argentina', kind: 'country', west: -73, south: -55, east: -53, north: -21 },
  resolution: 'paese',
  method,
  mode: 'HARMONIZED',
  filters: { sourceType: 'VEGETATION', dayNight: 'ANY', minConfidence: 'NOMINAL' },
  from: '2019-01',
  to: '2019-03',
  years: [2019],
  months: [1, 2, 3],
  values: [[1200, null, 300]],
  provisional: falses,
  incomplete: [[false, false, true]],
  bySatellite: { SNPP: [[1000, null, 250]], AQUA: [[200, null, 50]] },
  typicalUncertainty: 0.35,
  notes: ['Metodo di armonizzazione PROVVISORIO'],
};

export const anomaly: AnomalyResponse = {
  available: true,
  reason: null,
  month: '2019-01',
  value: 1200,
  median: 800,
  difference: 400,
  ratio: 1.5,
  percentileRank: 85,
  robustZScore: 1.4,
  referenceCount: 20,
  referenceYears: [],
  fewReferenceYears: false,
  valueIncomplete: false,
  valueProvisional: false,
  method,
  resolution: 'paese',
  notes: [],
};

export const profile: SeasonalProfileResponse = {
  months: Array.from({ length: 12 }, (_, i) => ({
    month: i + 1,
    years: 10,
    median: 100 * (i + 1),
    q1: 50 * (i + 1),
    q3: 150 * (i + 1),
    min: 10,
    max: 200 * (i + 1),
  })),
  peakMonth: 12,
  method,
  resolution: 'paese',
  notes: [],
};

export const trend: AnnualTrendResponse = {
  totals: [
    { year: 2018, total: 1000, provisional: false, relativeUncertainty: 0.35 },
    { year: 2019, total: 1500, provisional: false, relativeUncertainty: 0.35 },
  ],
  trend: {
    senSlope: 500,
    mannKendallS: 1,
    zScore: 1,
    pValue: 0.3,
    years: 2,
    fewYears: true,
    significantAt5Percent: false,
  },
  worstYear: 2019,
  peakMonth: 8,
  method,
  resolution: 'paese',
  notes: [],
};

export const monthDetail: MonthDetailResponse = {
  area: calendar.area,
  month: '2019-01',
  filters: calendar.filters,
  days: [
    { date: '2019-01-01', bySatellite: { SNPP: 30, AQUA: 5 }, status: { SNPP: 'OK', AQUA: 'OK' } },
    { date: '2019-01-02', bySatellite: { SNPP: 20 }, status: { SNPP: 'OK', AQUA: 'MISSING' } },
  ],
  monthTotalsBySatellite: { SNPP: 50, AQUA: 5 },
  notes: [],
};
