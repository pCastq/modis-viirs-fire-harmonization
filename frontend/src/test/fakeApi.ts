import { vi } from 'vitest';
import * as fixtures from './fixtures';

export interface RecordedCall {
  path: string;
  body: Record<string, unknown> | null;
  /** Valore dell'header `Accept-Language` inviato. */
  language: string | null;
}

type Override = (call: RecordedCall) => Response | undefined;

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

export function problem(status: number, title: string, detail: string): Response {
  return json({ status, title, detail }, status);
}

/** Sostituisce `fetch` con un finto server che risponde con i dati di prova e registra le richieste. */
export function installFakeApi(override: Override = () => undefined): RecordedCall[] {
  const calls: RecordedCall[] = [];
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input);
      const body = init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : null;
      const language = new Headers(init?.headers).get('Accept-Language');
      const call = { path, body, language };
      calls.push(call);
      const custom = override(call);
      if (custom) return custom;
      switch (path) {
        case '/api/v1/metadata':
          return json(fixtures.metadata);
        case '/api/v1/calendar':
          return json(fixtures.calendar);
        case '/api/v1/anomaly':
          return json(fixtures.anomaly);
        case '/api/v1/seasonal-profile':
          return json(fixtures.profile);
        case '/api/v1/annual-trend':
          return json(fixtures.trend);
        case '/api/v1/month-detail':
          return json(fixtures.monthDetail);
        case '/api/v1/hotspots':
          return json({
            type: 'FeatureCollection',
            features: [],
            date: '2019-01-01',
            count: 0,
            truncated: false,
            limit: 5000,
          });
        default:
          return problem(404, 'Non trovato', path);
      }
    }),
  );
  return calls;
}
