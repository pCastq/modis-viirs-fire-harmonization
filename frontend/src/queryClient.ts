import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '@/api/client';

const MAX_RETRIES = 2;

/** Un errore 4xx dipende dalla richiesta: riprovare darebbe la stessa risposta. */
export function shouldRetry(failureCount: number, error: unknown): boolean {
  const clientError = error instanceof ApiError && error.status >= 400 && error.status < 500;
  return !clientError && failureCount < MAX_RETRIES;
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: { queries: { retry: shouldRetry, refetchOnWindowFocus: false } },
  });
}
