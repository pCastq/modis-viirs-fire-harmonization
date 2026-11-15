import { getJson, postForFile, postJson } from './client';
import type {
  AnnualTrendResponse,
  AnomalyResponse,
  AreaRequest,
  BoundaryInfo,
  CalendarResponse,
  HotspotCollection,
  Metadata,
  MonthDetailResponse,
  SeasonalProfileResponse,
} from './types';

const BASE = '/api/v1';

/**
 * Un metodo per endpoint: è l'unico punto del front-end che conosce gli URL dell'API.
 * `language` è la lingua in cui il server scrive note ed errori.
 */
export function createApi(language: string) {
  return {
    metadata: (signal?: AbortSignal) => getJson<Metadata>(`${BASE}/metadata`, language, signal),
    boundaries: (signal?: AbortSignal) =>
      getJson<BoundaryInfo[]>(`${BASE}/boundaries`, language, signal),
    calendar: (body: AreaRequest, signal?: AbortSignal) =>
      postJson<CalendarResponse>(`${BASE}/calendar`, body, language, signal),
    anomaly: (body: AreaRequest, signal?: AbortSignal) =>
      postJson<AnomalyResponse>(`${BASE}/anomaly`, body, language, signal),
    seasonalProfile: (body: AreaRequest, signal?: AbortSignal) =>
      postJson<SeasonalProfileResponse>(`${BASE}/seasonal-profile`, body, language, signal),
    annualTrend: (body: AreaRequest, signal?: AbortSignal) =>
      postJson<AnnualTrendResponse>(`${BASE}/annual-trend`, body, language, signal),
    monthDetail: (body: AreaRequest, signal?: AbortSignal) =>
      postJson<MonthDetailResponse>(`${BASE}/month-detail`, body, language, signal),
    hotspots: (body: AreaRequest, signal?: AbortSignal) =>
      postJson<HotspotCollection>(`${BASE}/hotspots`, body, language, signal),
    exportHotspotsCsv: (body: AreaRequest) =>
      postForFile(`${BASE}/export/hotspots.csv`, body, 'hotspots.csv', language),
    exportMonthlyCsv: (body: AreaRequest) =>
      postForFile(`${BASE}/export/monthly.csv`, body, 'monthly.csv', language),
  };
}

export type Api = ReturnType<typeof createApi>;
