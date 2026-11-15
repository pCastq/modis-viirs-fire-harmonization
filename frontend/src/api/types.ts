/** Contratto dell'API REST (vedi `ApiDtos.java` e `/v3/api-docs`). Solo tipi: nessuna logica. */

export type SatelliteCode = 'TERRA' | 'AQUA' | 'SNPP' | 'NOAA20' | 'NOAA21';
export type Mode = 'HARMONIZED' | 'RAW';
export type DayNightFilter = 'ANY' | 'D' | 'N';
export type ConfidenceFilter = 'ALL' | 'NOMINAL' | 'HIGH';
export type SourceTypeFilter = 'VEGETATION' | 'ALL' | 'OTHER';
export type DayStatus = 'OK' | 'PARTIAL' | 'MISSING';
export type Resolution = 'paese' | '0.01' | '0.1';

/** GeoJSON Polygon in lon/lat WGS84, il solo formato di area libera accettato dall'API. */
export interface PolygonGeometry {
  type: 'Polygon';
  coordinates: number[][][];
}

/** Corpo comune delle richieste di lettura: un paese oppure un poligono, più i filtri. */
export interface AreaRequest {
  boundaryCode?: string;
  geometry?: PolygonGeometry;
  from?: string;
  to?: string;
  mode?: Mode;
  dayNight?: DayNightFilter;
  minConfidence?: ConfidenceFilter;
  sourceType?: SourceTypeFilter;
  year?: number;
  month?: number;
  date?: string;
}

export interface AreaInfo {
  label: string;
  kind: string;
  west: number;
  south: number;
  east: number;
  north: number;
}

export interface MethodInfo {
  id: string;
  version: string;
}

export interface FilterInfo {
  sourceType: SourceTypeFilter;
  dayNight: DayNightFilter;
  minConfidence: ConfidenceFilter;
}

export interface CalendarResponse {
  area: AreaInfo;
  resolution: Resolution;
  method: MethodInfo;
  mode: Mode;
  filters: FilterInfo;
  from: string;
  to: string;
  years: number[];
  months: number[];
  /** Anno x mese; `null` = nessun dato (non zero). */
  values: (number | null)[][];
  provisional: boolean[][];
  incomplete: boolean[][];
  /** Errore relativo atteso di ogni mese (0,14 = sotto il 14% in 8 casi su 10); `null` se non noto. */
  uncertainty: (number | null)[][];
  bySatellite: Partial<Record<SatelliteCode, (number | null)[][]>>;
  typicalUncertainty: number | null;
  notes: string[];
}

export interface AnomalyResponse {
  available: boolean;
  reason: string | null;
  month: string;
  value: number | null;
  median: number | null;
  difference: number | null;
  ratio: number | null;
  percentileRank: number | null;
  robustZScore: number | null;
  referenceCount: number | null;
  referenceYears: number[] | null;
  fewReferenceYears: boolean;
  valueIncomplete: boolean;
  valueProvisional: boolean;
  method: MethodInfo;
  resolution: Resolution;
  notes: string[];
}

export interface MonthProfile {
  month: number;
  years: number;
  median: number | null;
  q1: number | null;
  q3: number | null;
  min: number | null;
  max: number | null;
}

export interface SeasonalProfileResponse {
  months: MonthProfile[];
  peakMonth: number | null;
  method: MethodInfo;
  resolution: Resolution;
  notes: string[];
}

export interface AnnualTotal {
  year: number;
  total: number;
  provisional: boolean;
  relativeUncertainty: number | null;
}

export interface Trend {
  senSlope: number;
  mannKendallS: number;
  zScore: number;
  pValue: number;
  years: number;
  fewYears: boolean;
  significantAt5Percent: boolean;
}

export interface AnnualTrendResponse {
  totals: AnnualTotal[];
  trend: Trend | null;
  worstYear: number | null;
  peakMonth: number | null;
  method: MethodInfo;
  resolution: Resolution;
  notes: string[];
}

export interface DayDetail {
  date: string;
  bySatellite: Partial<Record<SatelliteCode, number>>;
  status: Partial<Record<SatelliteCode, DayStatus>>;
}

export interface MonthDetailResponse {
  area: AreaInfo;
  month: string;
  filters: FilterInfo;
  days: DayDetail[];
  monthTotalsBySatellite: Partial<Record<SatelliteCode, number>>;
  notes: string[];
}

export interface HotspotProperties {
  satellite: SatelliteCode;
  sensor: 'MODIS' | 'VIIRS';
  dateUtc: string;
  timeUtc: string;
  confidence: string;
  confidenceRaw: string | number | null;
  dayNight: 'D' | 'N';
  type: string;
  frpMw: number | null;
  productVersion: string;
}

export interface HotspotFeature {
  type: 'Feature';
  geometry: { type: 'Point'; coordinates: [number, number] };
  properties: HotspotProperties;
}

export interface HotspotCollection {
  type: 'FeatureCollection';
  features: HotspotFeature[];
  date: string;
  count: number;
  truncated: boolean;
  limit: number;
}

export interface SatelliteInfo {
  code: SatelliteCode;
  csvLabel: string;
  sensor: string;
  role: string;
  firstDate: string;
  lastDate: string | null;
  loaded: boolean;
}

export interface BoundaryInfo {
  code: string;
  name: string;
  level: string;
  west: number;
  south: number;
  east: number;
  north: number;
}

export interface Metadata {
  firstMonth: string;
  lastMonth: string;
  activeMethod: MethodInfo;
  activeMethodIsProvisional: boolean;
  availableMethods: MethodInfo[];
  satellites: SatelliteInfo[];
  boundaries: BoundaryInfo[];
  lastIngest: string | null;
  defaultSourceType: SourceTypeFilter;
  limits: {
    maxHotspotsPerDay: number;
    maxExportRows: number;
    maxPolygonVertices: number;
    exactResolutionMaxBoxDegrees2: number;
  };
  notes: string[];
}

/** I tre filtri giorno/notte dell'API di armonizzazione, con i nomi con cui li scrive il server. */
export type FactorDayNight = 'ANY' | 'DAY' | 'NIGHT';

/** Da quale livello viene un k: il paese, tutti i paesi insieme o l'intera calibrazione. */
export type FactorSource = 'COUNTRY' | 'REGION' | 'GLOBAL';

export interface CalibrationInfo {
  /** `AAAA-MM` */
  from: string;
  to: string;
  monthsUsed: number;
}

/** Il moltiplicatore k di un trimestre e di un filtro giorno/notte, con le somme da cui nasce. */
export interface HarmonizationFactor {
  /** 1 = gennaio-marzo */
  quarter: 1 | 2 | 3 | 4;
  dayNight: FactorDayNight;
  /** Celle-giorno VIIRS per ogni cella-giorno MODIS (Terra+Aqua); `null` senza calibrazione. */
  k: number | null;
  source: FactorSource | null;
  viirsCellDays: number;
  modisCellDays: number;
}

export interface UncertaintyBand {
  /** Fascia di celle-giorno MODIS del mese, per esempio `100-299`. */
  level: string;
  relativeUncertainty: number;
}

export interface HarmonizationResponse {
  method: MethodInfo;
  calibration: CalibrationInfo | null;
  area: string;
  satelliteGroup: string;
  sourceType: SourceTypeFilter;
  minConfidence: ConfidenceFilter;
  factors: HarmonizationFactor[];
  uncertaintyBands: UncertaintyBand[];
  notes: string[];
}

/** Problem Details (RFC 9457) restituito dall'API per ogni errore. */
export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
}
