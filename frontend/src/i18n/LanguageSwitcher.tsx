import { LOCALES, LOCALE_NAMES, isLocale } from './locales';
import { useI18n } from './I18nProvider';
import styles from './LanguageSwitcher.module.css';

export function LanguageSwitcher() {
  const { locale, setLocale, t } = useI18n();
  return (
    <label className={styles.label}>
      <span className="sr-only">{t('header.language')}</span>
      <select
        className={styles.select}
        value={locale}
        onChange={(event) => {
          if (isLocale(event.target.value)) setLocale(event.target.value);
        }}
      >
        {LOCALES.map((code) => (
          <option key={code} value={code} lang={code}>
            {LOCALE_NAMES[code]}
          </option>
        ))}
      </select>
    </label>
  );
}
