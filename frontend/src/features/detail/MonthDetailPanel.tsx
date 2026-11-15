import { useMemo } from 'react';
import { Card } from '@/components/Card';
import { errorText } from '@/components/errorText';
import { Notice } from '@/components/Notice';
import { QueryBoundary } from '@/components/QueryBoundary';
import { Skeleton } from '@/components/Skeleton';
import { toDayBars } from '@/domain/dailyChart';
import { parseMonthKey } from '@/domain/months';
import { useI18n } from '@/i18n/I18nProvider';
import { satelliteStyle } from '@/domain/satellites';
import type { Focus, SearchQuery } from '@/domain/query';
import { useHotspots, useMonthDetail } from '@/hooks/queries';
import { Button } from '@/components/Button';
import { DailyChart } from './DailyChart';
import { HotspotMap } from './HotspotMap';
import styles from './MonthDetailPanel.module.css';
import type { SatelliteCode } from '@/api/types';

interface Props {
  query: SearchQuery;
  focus: Focus & { month: string };
  onFocus: (focus: Focus) => void;
}

/** Cosa è successo giorno per giorno nel mese scelto; clic su un giorno per vederne i punti. */
export function MonthDetailPanel({ query, focus, onFocus }: Props) {
  const { t, fmt } = useI18n();
  const month = parseMonthKey(focus.month);
  const detail = useMonthDetail(query, month);
  const hotspots = useHotspots(query, focus);
  const bars = useMemo(() => (detail.data ? toDayBars(detail.data.days) : []), [detail.data]);

  return (
    <Card
      id="detail"
      title={t('detail.title', { month: fmt.monthKey(focus.month) })}
      subtitle={t('detail.subtitle')}
      actions={
        <Button variant="ghost" onClick={() => onFocus({ month: null, date: null })}>
          {t('common.close')}
        </Button>
      }
    >
      <QueryBoundary {...detail} onRetry={() => detail.refetch()} skeletonHeight={240}>
        {(data) => (
          <div className={styles.body}>
            <DailyChart
              bars={bars}
              selectedDate={focus.date}
              onSelect={(date) => onFocus({ month: focus.month, date })}
            />
            <ul className={styles.totals}>
              {(Object.entries(data.monthTotalsBySatellite) as [SatelliteCode, number][]).map(
                ([code, total]) => (
                  <li key={code}>
                    <i style={{ background: satelliteStyle(code).color }} aria-hidden="true" />
                    {satelliteStyle(code).label}: <strong>{fmt.count(total)}</strong>
                  </li>
                ),
              )}
            </ul>
            {data.notes.map((note) => (
              <Notice key={note}>{note}</Notice>
            ))}
          </div>
        )}
      </QueryBoundary>

      {focus.date ? (
        <div className={styles.map}>
          <h3 className={styles.mapTitle}>{t('detail.pointsOf', { date: focus.date })}</h3>
          {hotspots.data ? (
            <>
              {hotspots.data.truncated ? (
                <Notice tone="warn">
                  {t('detail.truncated', { limit: fmt.count(hotspots.data.limit) })}
                </Notice>
              ) : null}
              {hotspots.data.count === 0 ? (
                <Notice>{t('detail.none')}</Notice>
              ) : (
                <HotspotMap features={hotspots.data.features} />
              )}
            </>
          ) : hotspots.error ? (
            <Notice tone="error">{errorText(hotspots.error, t).detail}</Notice>
          ) : (
            <Skeleton height={340} />
          )}
        </div>
      ) : (
        <p className={styles.tip}>{t('detail.tip')}</p>
      )}
    </Card>
  );
}
