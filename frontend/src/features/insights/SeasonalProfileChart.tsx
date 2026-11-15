import type { SeasonalProfileResponse } from '@/api/types';
import { linearScale, niceAxis } from '@/domain/chartScale';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './Charts.module.css';

const W = 620;
const H = 250;
const M = { top: 12, right: 12, bottom: 28, left: 52 };

interface Props {
  profile: SeasonalProfileResponse;
  highlightMonth: number | null;
}

/**
 * Profilo stagionale: per ogni mese, come variano i conteggi fra gli anni.
 * Riquadro = metà centrale degli anni (25-75%), tacca = mediana, baffi = minimo e massimo.
 */
export function SeasonalProfileChart({ profile, highlightMonth }: Props) {
  const { t, fmt } = useI18n();
  const months = profile.months;
  const axis = niceAxis(Math.max(0, ...months.map((m) => m.max ?? 0)));
  const y = linearScale([0, axis.top], [H - M.bottom, M.top]);
  const slot = (W - M.left - M.right) / 12;

  const summary = months
    .filter((m) => m.median !== null)
    .map((m) =>
      t('insights.profile.median', { month: fmt.monthName(m.month), value: fmt.count(m.median) }),
    )
    .join('; ');

  return (
    <figure className={styles.figure}>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        className={styles.svg}
        role="img"
        aria-label={t('insights.profile.aria', { summary })}
      >
        {axis.ticks.map((tick) => (
          <g key={tick}>
            <line x1={M.left} x2={W - M.right} y1={y(tick)} y2={y(tick)} className={styles.grid} />
            <text x={M.left - 6} y={y(tick)} className={styles.tickY} dominantBaseline="middle">
              {fmt.compact(tick)}
            </text>
          </g>
        ))}
        {months.map((m, i) => {
          const cx = M.left + slot * i + slot / 2;
          const hot = m.month === profile.peakMonth;
          const label = m.month === highlightMonth;
          return (
            <g key={m.month}>
              <text x={cx} y={H - 8} className={`${styles.tickX} ${label ? styles.strong : ''}`}>
                {fmt.monthShort(m.month)}
              </text>
              {m.median !== null &&
              m.q1 !== null &&
              m.q3 !== null &&
              m.min !== null &&
              m.max !== null ? (
                <g>
                  <title>
                    {t('insights.profile.tooltip', {
                      count: m.years,
                      month: fmt.monthName(m.month),
                      median: fmt.count(m.median),
                      q1: fmt.count(m.q1),
                      q3: fmt.count(m.q3),
                      min: fmt.count(m.min),
                      max: fmt.count(m.max),
                    })}
                  </title>
                  <line x1={cx} x2={cx} y1={y(m.max)} y2={y(m.min)} className={styles.whisker} />
                  <rect
                    x={cx - slot * 0.28}
                    width={slot * 0.56}
                    y={y(m.q3)}
                    height={Math.max(2, y(m.q1) - y(m.q3))}
                    rx={3}
                    className={hot ? styles.boxPeak : styles.box}
                  />
                  <line
                    x1={cx - slot * 0.28}
                    x2={cx + slot * 0.28}
                    y1={y(m.median)}
                    y2={y(m.median)}
                    className={styles.median}
                  />
                </g>
              ) : null}
            </g>
          );
        })}
      </svg>
      <figcaption className={styles.caption}>{t('insights.profile.caption')}</figcaption>
    </figure>
  );
}
