import type { AnomalyResponse } from '@/api/types';
import { Notice } from '@/components/Notice';
import { classifyAnomaly } from '@/domain/anomaly';
import { capitalize } from '@/domain/format';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './AnomalyCard.module.css';

interface Props {
  anomaly: AnomalyResponse;
}

/** Il mese scelto confrontato con lo stesso mese degli altri anni: "è normale o no?". */
export function AnomalyCard({ anomaly }: Props) {
  const { t, fmt } = useI18n();
  const month = capitalize(fmt.monthKey(anomaly.month));

  if (!anomaly.available) {
    // il motivo arriva dal server (testo gia' scritto): non lo si puo' tradurre qui
    return (
      <Notice tone="info" title={month}>
        {anomaly.reason}
      </Notice>
    );
  }
  const level = classifyAnomaly(anomaly.robustZScore);
  return (
    <div className={styles.anomaly}>
      <p className={styles.headline}>
        <strong>{month}</strong>:{' '}
        <span className={level ? styles[level] : undefined}>
          {level ? t(`insights.anomaly.${level}`) : fmt.count(null)}
        </span>
      </p>
      <dl className={styles.facts}>
        <div>
          <dt>{t('insights.anomaly.thisMonth')}</dt>
          <dd>{fmt.count(anomaly.value)}</dd>
        </div>
        <div>
          <dt>{t('insights.anomaly.median')}</dt>
          <dd>{fmt.count(anomaly.median)}</dd>
        </div>
        <div>
          <dt>{t('insights.anomaly.difference')}</dt>
          <dd>{fmt.signedPercent(anomaly.ratio)}</dd>
        </div>
      </dl>
      {anomaly.percentileRank !== null ? (
        <p className={styles.note}>
          {t('insights.anomaly.percentile', { rank: Math.round(anomaly.percentileRank) })}.
        </p>
      ) : null}
      <p className={styles.note}>
        {t('insights.anomaly.reference', { count: anomaly.referenceCount ?? 0 })}
        {anomaly.fewReferenceYears ? t('insights.anomaly.fewReference') : ''}.
      </p>
      {anomaly.valueIncomplete ? (
        <Notice tone="warn">{t('insights.anomaly.incomplete')}</Notice>
      ) : null}
      {anomaly.valueProvisional ? (
        <Notice tone="warn">{t('insights.anomaly.provisional')}</Notice>
      ) : null}
    </div>
  );
}
