import type { AnnualTrendResponse } from '@/api/types';
import { linearScale, niceAxis } from '@/domain/chartScale';
import { senLine } from '@/domain/trend';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './Charts.module.css';

const W = 620;
const H = 250;
const M = { top: 12, right: 12, bottom: 28, left: 52 };

interface Props {
  trend: AnnualTrendResponse;
}

/** Totale annuale (solo anni completi), con l'incertezza del metodo e la retta di tendenza di Sen. */
export function AnnualTrendChart({ trend }: Props) {
  const { t, fmt } = useI18n();
  const totals = trend.totals;
  const tops = totals.map((entry) => entry.total * (1 + (entry.relativeUncertainty ?? 0)));
  const axis = niceAxis(Math.max(0, ...tops));
  const y = linearScale([0, axis.top], [H - M.bottom, M.top]);
  const slot = (W - M.left - M.right) / Math.max(totals.length, 1);
  const line = trend.trend ? senLine(totals, trend.trend.senSlope) : null;
  const first = totals[0]?.year ?? 0;
  const last = totals.at(-1)?.year ?? 0;
  const xOf = (year: number) => M.left + slot * (year - first) + slot / 2;
  const labelEvery = totals.length > 16 ? 3 : totals.length > 8 ? 2 : 1;

  return (
    <figure className={styles.figure}>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        className={styles.svg}
        role="img"
        aria-label={t('insights.trend.aria', { first, last })}
      >
        {axis.ticks.map((tick) => (
          <g key={tick}>
            <line x1={M.left} x2={W - M.right} y1={y(tick)} y2={y(tick)} className={styles.grid} />
            <text x={M.left - 6} y={y(tick)} className={styles.tickY} dominantBaseline="middle">
              {fmt.compact(tick)}
            </text>
          </g>
        ))}
        {totals.map((entry, i) => {
          const x = M.left + slot * i + slot * 0.15;
          const u = entry.relativeUncertainty ?? 0;
          const cx = x + slot * 0.35;
          return (
            <g key={entry.year}>
              <title>
                {t('insights.trend.tooltip', { year: entry.year, total: fmt.count(entry.total) }) +
                  (entry.relativeUncertainty != null
                    ? t('insights.trend.tooltipUncertainty', { uncertainty: fmt.percent(u) })
                    : '') +
                  (entry.provisional ? t('insights.trend.tooltipProvisional') : '')}
              </title>
              <rect
                x={x}
                width={slot * 0.7}
                y={y(entry.total)}
                height={Math.max(0, y(0) - y(entry.total))}
                rx={2}
                className={entry.provisional ? styles.barProvisional : styles.bar}
              />
              {u > 0 ? (
                <line
                  x1={cx}
                  x2={cx}
                  y1={y(entry.total * (1 + u))}
                  y2={y(entry.total * (1 - u))}
                  className={styles.whisker}
                />
              ) : null}
              {i % labelEvery === 0 ? (
                <text x={cx} y={H - 8} className={styles.tickX}>
                  {entry.year}
                </text>
              ) : null}
            </g>
          );
        })}
        {line ? (
          <line
            x1={xOf(first)}
            x2={xOf(last)}
            y1={y(Math.max(0, line(first)))}
            y2={y(Math.max(0, line(last)))}
            className={styles.trendLine}
          />
        ) : null}
      </svg>
      <figcaption className={styles.caption}>
        {t('insights.trend.caption')}
        {line ? t('insights.trend.captionLine') : ''}
      </figcaption>
    </figure>
  );
}
