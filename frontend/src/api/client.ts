import type { ProblemDetails } from './types';

/** Errore dell'API, con il titolo e il dettaglio di Problem Details pronti da mostrare. */
export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly detail: string;

  constructor(status: number, title: string, detail: string) {
    super(detail || title);
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
  }

  /** Errore di rete o server irraggiungibile (nessuna risposta HTTP). */
  get isNetwork(): boolean {
    return this.status === 0;
  }
}

const JSON_HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json' };

async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemDetails = {};
  try {
    problem = (await response.json()) as ProblemDetails;
  } catch {
    // corpo assente o non JSON: restano i valori predefiniti
  }
  return new ApiError(
    response.status,
    problem.title ?? `HTTP ${response.status}`,
    problem.detail ?? response.statusText,
  );
}

async function send(path: string, init: RequestInit): Promise<Response> {
  let response: Response;
  try {
    response = await fetch(path, init);
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause;
    }
    // il testo per l'utente lo sceglie l'interfaccia, nella lingua scelta (vedi errorText)
    throw new ApiError(0, 'Network error', cause instanceof Error ? cause.message : '');
  }
  if (!response.ok) {
    throw await toApiError(response);
  }
  return response;
}

/**
 * Il server scrive in questa lingua note ed errori (header `Accept-Language`); numeri e dati
 * non cambiano. Senza header, o con una lingua che non conosce, risponde in inglese.
 */
export async function getJson<T>(path: string, language: string, signal?: AbortSignal): Promise<T> {
  const response = await send(path, {
    headers: { Accept: 'application/json', 'Accept-Language': language },
    signal,
  });
  return (await response.json()) as T;
}

export async function postJson<T>(
  path: string,
  body: unknown,
  language: string,
  signal?: AbortSignal,
): Promise<T> {
  const response = await send(path, {
    method: 'POST',
    headers: { ...JSON_HEADERS, 'Accept-Language': language },
    body: JSON.stringify(body),
    signal,
  });
  return (await response.json()) as T;
}

export interface DownloadedFile {
  blob: Blob;
  filename: string;
}

const FILENAME = /filename="?([^";]+)"?/i;

/** Scarica un file POST (CSV): il blob è pronto per il salvataggio, gli errori restano leggibili. */
export async function postForFile(
  path: string,
  body: unknown,
  fallbackName: string,
  language: string,
): Promise<DownloadedFile> {
  const response = await send(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/csv',
      'Accept-Language': language,
    },
    body: JSON.stringify(body),
  });
  const disposition = response.headers.get('Content-Disposition') ?? '';
  return { blob: await response.blob(), filename: FILENAME.exec(disposition)?.[1] ?? fallbackName };
}
