export const LOCALES = ['en', 'it'] as const;
export type Locale = (typeof LOCALES)[number];

/** L'interfaccia parte in inglese, indipendentemente dalla lingua del browser. */
export const DEFAULT_LOCALE: Locale = 'en';

/** Nome di ogni lingua scritto nella lingua stessa: si riconosce anche senza leggere l'interfaccia. */
export const LOCALE_NAMES: Record<Locale, string> = { en: 'English', it: 'Italiano' };

/** Etichetta BCP 47 per `Intl` (numeri, mesi). */
export const INTL_TAG: Record<Locale, string> = { en: 'en-US', it: 'it-IT' };

export function isLocale(value: string | null | undefined): value is Locale {
  return LOCALES.some((l) => l === value);
}

const STORAGE_KEY = 'firecal.locale';

interface StorageLike {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
}

/** Ordine di precedenza: `?lang=` nell'URL (link condiviso), poi la scelta salvata, poi l'inglese. */
export function detectLocale(search: string, storage: StorageLike | null): Locale {
  const fromUrl = new URLSearchParams(search).get('lang');
  if (isLocale(fromUrl)) return fromUrl;
  try {
    const saved = storage?.getItem(STORAGE_KEY);
    if (isLocale(saved)) return saved;
  } catch {
    // storage non disponibile (modalita' privata, politiche del browser): si usa il predefinito
  }
  return DEFAULT_LOCALE;
}

export function saveLocale(locale: Locale, storage: StorageLike | null): void {
  try {
    storage?.setItem(STORAGE_KEY, locale);
  } catch {
    // la scelta vale comunque per questa sessione
  }
}
