import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { I18nProvider } from '@/i18n/I18nProvider';
import type { Locale } from '@/i18n/locales';

interface Options {
  /** `'detect'`: come nell'app (URL, scelta salvata, poi inglese). */
  locale?: Locale | 'detect';
}

/** Render dentro il provider di lingua (inglese per impostazione predefinita, come l'app). */
export function renderWithI18n(ui: ReactElement, { locale = 'en' }: Options = {}) {
  return render(
    <I18nProvider initialLocale={locale === 'detect' ? undefined : locale}>{ui}</I18nProvider>,
  );
}

/** Render con lingua e un QueryClient nuovo (senza ripetizioni) per ogni test. */
export function renderWithClient(ui: ReactElement, options: Options = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return renderWithI18n(<QueryClientProvider client={client}>{ui}</QueryClientProvider>, options);
}
