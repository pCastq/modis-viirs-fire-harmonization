import { useI18n } from '@/i18n/I18nProvider';
import styles from './Skeleton.module.css';

interface Props {
  height?: number;
  /** Testo per gli screen reader; predefinito: "Caricamento…" nella lingua scelta. */
  label?: string;
}

/** Segnaposto durante il caricamento: occupa lo spazio finale e evita che la pagina "salti". */
export function Skeleton({ height = 160, label }: Props) {
  const { t } = useI18n();
  return (
    <div className={styles.skeleton} style={{ height }} role="status" aria-live="polite">
      <span className="sr-only">{label ?? t('common.loading')}</span>
    </div>
  );
}
