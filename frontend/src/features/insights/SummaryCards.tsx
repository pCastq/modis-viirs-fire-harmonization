import type { AnnualTrendResponse, CalendarResponse } from '@/api/types';
import { capitalize } from '@/domain/format';
import { trendDirection } from '@/domain/trend';
import { areaName } from '@/i18n/areaName';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './SummaryCards.module.css';

interface Props {
  calendar: CalendarResponse;
  trend: AnnualTrendResponse | undefined;
}

function totalHotspots(calendar: CalendarResponse): number {
  return calendar.values.flat().reduce<number>((sum, v) => sum + (v ?? 0), 0);
}

interface Kpi {
  label: string;
  value: string;
  detail?: string;
}

/** Quattro numeri che rispondono subito a: quanto brucia, quando, quale anno peggiore, dove va. */
export function SummaryCards({ calendar, trend }: Props) {
  const { t, fmt } = useI18n();
  const dash = fmt.count(null);

  const trendKpi = (): Pick<Kpi, 'value' | 'detail'> => {
    if (!trend) return { value: '…', detail: t('insights.kpi.trendPending') };
    if (!trend.trend) return { value: dash, detail: t('insights.kpi.trendNone') };
    const years = trend.trend.years;
    const direction = trendDirection(trend.trend);
    if (direction === 'flat') {
      return {
        value: t('insights.kpi.trendFlat'),
        detail: t('insights.kpi.trendFlatDetail', { count: years }),
      };
    }
    return {
      value: t(direction === 'up' ? 'insights.kpi.trendUp' : 'insights.kpi.trendDown'),
      detail:
        t('insights.kpi.trendDetail', {
          perYear: fmt.compact(Math.abs(trend.trend.senSlope)),
          count: years,
        }) + (trend.trend.fewYears ? t('insights.kpi.fewYears') : ''),
    };
  };

  const kpis: Kpi[] = [
    {
      label: t('insights.kpi.total'),
      value: fmt.count(totalHotspots(calendar)),
      detail: t('insights.kpi.totalDetail', {
        area: areaName(calendar.area, t),
        mode: calendar.mode === 'HARMONIZED' ? t('modes.harmonized') : t('modes.raw'),
      }),
    },
    {
      label: t('insights.kpi.peakMonth'),
      value: trend?.peakMonth ? capitalize(fmt.monthName(trend.peakMonth)) : dash,
      detail: t('insights.kpi.peakMonthDetail'),
    },
    {
      label: t('insights.kpi.worstYear'),
      value: trend?.worstYear ? String(trend.worstYear) : dash,
      detail: t('insights.kpi.worstYearDetail'),
    },
    { label: t('insights.kpi.trend'), ...trendKpi() },
  ];

  return (
    <ul className={styles.list}>
      {kpis.map((kpi) => (
        <li key={kpi.label} className={styles.kpi}>
          <span className={styles.label}>{kpi.label}</span>
          <strong className={styles.value}>{kpi.value}</strong>
          {kpi.detail ? <span className={styles.detail}>{kpi.detail}</span> : null}
        </li>
      ))}
    </ul>
  );
}
