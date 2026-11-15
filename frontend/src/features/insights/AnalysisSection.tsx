import { useMemo } from 'react';
import type { CalendarResponse } from '@/api/types';
import { Card } from '@/components/Card';
import { QueryBoundary } from '@/components/QueryBoundary';
import { buildCells, latestComparableMonth } from '@/domain/heatmap';
import { capitalize } from '@/domain/format';
import { monthKey, parseMonthKey } from '@/domain/months';
import type { Focus, SearchQuery } from '@/domain/query';
import { useI18n } from '@/i18n/I18nProvider';
import { useAnnualTrend, useAnomaly, useSeasonalProfile } from '@/hooks/queries';
import { AnnualTrendChart } from './AnnualTrendChart';
import { AnomalyCard } from './AnomalyCard';
import { SeasonalProfileChart } from './SeasonalProfileChart';
import { SummaryCards } from './SummaryCards';
import styles from './AnalysisSection.module.css';

interface Props {
  query: SearchQuery;
  calendar: CalendarResponse;
  focus: Focus;
}

/** I quattro numeri in alto: la risposta rapida, prima del calendario. */
export function SummaryStrip({ query, calendar }: Pick<Props, 'query' | 'calendar'>) {
  const trend = useAnnualTrend(query);
  return <SummaryCards calendar={calendar} trend={trend.data} />;
}

/** Gli indicatori che spiegano il calendario: anomalia, profilo stagionale, tendenza annuale. */
export function AnalysisGrid({ query, calendar, focus }: Props) {
  const { t, fmt } = useI18n();
  const trend = useAnnualTrend(query);
  const profile = useSeasonalProfile(query);

  // l'anomalia riguarda il mese scelto, o altrimenti l'ultimo mese confrontabile
  const anomalyMonth = useMemo(() => {
    if (focus.month) return parseMonthKey(focus.month);
    return latestComparableMonth(buildCells(calendar));
  }, [focus.month, calendar]);
  const anomaly = useAnomaly(query, anomalyMonth);

  return (
    <div className={styles.grid}>
      <Card
        id="anomaly"
        title={t('insights.anomaly.title')}
        subtitle={
          anomalyMonth
            ? t('insights.anomaly.subtitle', {
                month: capitalize(fmt.monthKey(monthKey(anomalyMonth.year, anomalyMonth.month))),
              })
            : undefined
        }
      >
        <QueryBoundary {...anomaly} onRetry={() => anomaly.refetch()} skeletonHeight={140}>
          {(data) => <AnomalyCard anomaly={data} />}
        </QueryBoundary>
      </Card>
      <Card
        id="profile"
        title={t('insights.profile.title')}
        subtitle={t('insights.profile.subtitle')}
      >
        <QueryBoundary {...profile} onRetry={() => profile.refetch()} skeletonHeight={250}>
          {(data) => (
            <SeasonalProfileChart profile={data} highlightMonth={anomalyMonth?.month ?? null} />
          )}
        </QueryBoundary>
      </Card>
      <Card id="trend" title={t('insights.trend.title')} subtitle={t('insights.trend.subtitle')}>
        <QueryBoundary {...trend} onRetry={() => trend.refetch()} skeletonHeight={250}>
          {(data) => <AnnualTrendChart trend={data} />}
        </QueryBoundary>
      </Card>
    </div>
  );
}
