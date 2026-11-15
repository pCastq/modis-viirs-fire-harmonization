import type { Metadata } from '@/api/types';
import { useI18n } from '@/i18n/I18nProvider';
import { LanguageSwitcher } from '@/i18n/LanguageSwitcher';
import styles from './Header.module.css';

interface Props {
  metadata: Metadata | undefined;
}

export function Header({ metadata }: Props) {
  const { t, fmt } = useI18n();
  return (
    <header className={styles.header}>
      <div className={styles.brand}>
        <span className={styles.logo} aria-hidden="true">
          🔥
        </span>
        <div>
          <h1 className={styles.title}>{t('header.title')}</h1>
          <p className={styles.subtitle}>{t('header.subtitle')}</p>
        </div>
      </div>
      <ul className={styles.chips}>
        {metadata ? (
          <>
            <li>
              {t('header.dataRange', {
                from: fmt.monthKey(metadata.firstMonth),
                to: fmt.monthKey(metadata.lastMonth),
              })}
            </li>
            <li className={metadata.activeMethodIsProvisional ? styles.warn : undefined}>
              {t('header.method', {
                id: metadata.activeMethod.id,
                version: metadata.activeMethod.version,
              })}
              {metadata.activeMethodIsProvisional ? ` ${t('header.provisional')}` : ''}
            </li>
            <li>
              <a href="/swagger-ui.html" target="_blank" rel="noreferrer">
                {t('header.api')}
              </a>
            </li>
          </>
        ) : null}
        <li>
          <LanguageSwitcher />
        </li>
      </ul>
    </header>
  );
}
