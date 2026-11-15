import { ApiError } from '@/api/client';
import type { Translator } from '@/i18n/translate';

export interface ErrorText {
  title: string;
  detail: string;
}

/**
 * Titolo e dettaglio da mostrare per un errore. Quelli generati qui (rete assente, errore
 * imprevisto) sono nella lingua scelta; quelli del server arrivano gia' scritti dal server.
 */
export function errorText(error: Error, t: Translator): ErrorText {
  if (error instanceof ApiError) {
    return error.isNetwork
      ? { title: t('errors.networkTitle'), detail: t('errors.networkDetail') }
      : { title: error.title, detail: error.detail };
  }
  return { title: t('common.unknownError'), detail: error.message };
}
