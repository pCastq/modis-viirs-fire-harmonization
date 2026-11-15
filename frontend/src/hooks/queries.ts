import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { createApi, type Api } from '@/api/endpoints';
import type { AreaRequest } from '@/api/types';
import { toRequest, type Focus, type SearchQuery } from '@/domain/query';
import { useI18n } from '@/i18n/I18nProvider';
import type { Locale } from '@/i18n/locales';

const HOUR = 60 * 60 * 1000;

/** Il client dell'API legato alla lingua scelta: note ed errori del server arrivano in quella lingua. */
function useApi(): { api: Api; language: Locale } {
  const { locale } = useI18n();
  const api = useMemo(() => createApi(locale), [locale]);
  return { api, language: locale };
}

export function useMetadata() {
  const { api, language } = useApi();
  return useQuery({
    queryKey: ['metadata', language],
    queryFn: ({ signal }) => api.metadata(signal),
    staleTime: HOUR,
  });
}

type Fetcher<T> = (body: AreaRequest, signal: AbortSignal) => Promise<T>;

/**
 * I dati cambiano solo quando si ricaricano i file: le risposte restano valide a lungo e, mentre
 * si cambia filtro, quella precedente resta a video (niente sfarfallio di schermate vuote).
 * La lingua e' nella chiave: cambiandola si rileggono note e testi del server nella lingua nuova.
 */
function useRead<T>(name: string, request: AreaRequest | null, pick: (api: Api) => Fetcher<T>) {
  const { api, language } = useApi();
  return useQuery({
    queryKey: [name, language, request],
    queryFn: ({ signal }) => pick(api)(request as AreaRequest, signal),
    enabled: request !== null,
    staleTime: HOUR,
    placeholderData: keepPreviousData,
  });
}

/**
 * I moltiplicatori k di un paese. Un'area disegnata non ha un k unico (il metodo usa k locali per blocchi di 2 gradi):
 * per lei non si chiede nulla.
 */
export function useHarmonization(query: SearchQuery) {
  const { api, language } = useApi();
  const { area, filters } = query;
  const country = area.kind === 'country' ? area.code : null;
  return useQuery({
    queryKey: ['harmonization', language, country, filters.sourceType, filters.minConfidence],
    queryFn: ({ signal }) =>
      api.harmonization(
        country as string,
        { sourceType: filters.sourceType, minConfidence: filters.minConfidence },
        signal,
      ),
    enabled: country !== null,
    staleTime: HOUR,
    placeholderData: keepPreviousData,
  });
}

export function useCalendar(query: SearchQuery) {
  return useRead('calendar', toRequest(query), (api) => api.calendar);
}

export function useSeasonalProfile(query: SearchQuery) {
  return useRead('seasonal-profile', toRequest(query), (api) => api.seasonalProfile);
}

export function useAnnualTrend(query: SearchQuery) {
  return useRead('annual-trend', toRequest(query), (api) => api.annualTrend);
}

/** L'anomalia del mese a fuoco; senza mese non c'è nulla da chiedere. */
export function useAnomaly(query: SearchQuery, month: { year: number; month: number } | null) {
  return useRead('anomaly', month ? toRequest(query, month) : null, (api) => api.anomaly);
}

export function useMonthDetail(query: SearchQuery, month: { year: number; month: number } | null) {
  return useRead('month-detail', month ? toRequest(query, month) : null, (api) => api.monthDetail);
}

export function useHotspots(query: SearchQuery, focus: Focus) {
  return useRead(
    'hotspots',
    focus.date ? toRequest(query, { date: focus.date }) : null,
    (api) => api.hotspots,
  );
}

export type ExportKind = 'monthly' | 'hotspots';

export interface ExportRequest {
  kind: ExportKind;
  /** `AAAA-MM`: limita l'export a questo mese invece che a tutto il periodo cercato. */
  month?: string;
}

/** Scarica un CSV e lo salva con il nome scelto dal server. */
export function useCsvExport(query: SearchQuery) {
  const { api } = useApi();
  return useMutation({
    mutationFn: async ({ kind, month }: ExportRequest) => {
      const body = toRequest(query, month ? { from: month, to: month } : {});
      const file = await (kind === 'monthly'
        ? api.exportMonthlyCsv(body)
        : api.exportHotspotsCsv(body));
      saveBlob(file.blob, file.filename);
    },
  });
}

function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
