import { divergingGradient, DIVERGING_LIMIT, sequentialGradient } from '@/domain/colorScale';
import { useI18n } from '@/i18n/I18nProvider';
import type { HeatView } from './CalendarHeatmap';
import styles from './HeatmapLegend.module.css';

interface Props {
  view: HeatView;
  max: number;
}

/** Spiega i colori e i segni speciali del calendario. */
export function HeatmapLegend({ view, max }: Props) {
  const { t, fmt } = useI18n();
  const counts = view === 'counts';
  return (
    <div className={styles.legend}>
      <div className={styles.ramp}>
        <div
          className={styles.bar}
          style={{ background: counts ? sequentialGradient() : divergingGradient() }}
          role="img"
          aria-label={counts ? t('calendar.legend.counts') : t('calendar.legend.typical')}
        />
        <div className={styles.labels}>
          {counts ? (
            <>
              <span>0</span>
              <span>{t('calendar.legend.perMonth', { max: fmt.compact(max) })}</span>
            </>
          ) : (
            <>
              <span>{t('calendar.legend.muchLess', { limit: DIVERGING_LIMIT })}</span>
              <span>{t('calendar.legend.typical0')}</span>
              <span>{t('calendar.legend.muchMore', { limit: DIVERGING_LIMIT })}</span>
            </>
          )}
        </div>
      </div>
      <ul className={styles.keys}>
        <li>
          <span className={`${styles.swatch} ${styles.swEmpty}`} aria-hidden="true" />{' '}
          {t('calendar.legend.noData')}
        </li>
        <li>
          <span className={`${styles.swatch} ${styles.swIncomplete}`} aria-hidden="true" />{' '}
          {t('calendar.legend.incomplete')}
        </li>
        <li>
          <span className={`${styles.swatch} ${styles.swProvisional}`} aria-hidden="true" />{' '}
          {t('calendar.legend.provisional')}
        </li>
      </ul>
    </div>
  );
}
