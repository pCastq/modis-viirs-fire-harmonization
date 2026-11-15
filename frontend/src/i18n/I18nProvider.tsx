import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { createFormatters, type Formatters } from '@/domain/format';
import { DEFAULT_LOCALE, detectLocale, saveLocale, type Locale } from './locales';
import { createTranslator, type Translator } from './translate';

interface I18n {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: Translator;
  fmt: Formatters;
}

const I18nContext = createContext<I18n | null>(null);

function browserStorage(): Storage | null {
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

interface Props {
  children: ReactNode;
  /** Solo per i test: lingua iniziale al posto di quella rilevata. */
  initialLocale?: Locale;
}

export function I18nProvider({ children, initialLocale }: Props) {
  const [locale, setLocaleState] = useState<Locale>(
    () => initialLocale ?? detectLocale(window.location.search, browserStorage()),
  );

  const value = useMemo<I18n>(
    () => ({
      locale,
      setLocale: (next) => {
        saveLocale(next, browserStorage());
        setLocaleState(next);
      },
      t: createTranslator(locale),
      fmt: createFormatters(locale),
    }),
    [locale],
  );

  useEffect(() => {
    document.documentElement.lang = locale;
    document.title = value.t('app.documentTitle');
  }, [locale, value]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18n {
  const context = useContext(I18nContext);
  if (!context) throw new Error('useI18n va usato dentro <I18nProvider>');
  return context;
}

export { DEFAULT_LOCALE };
