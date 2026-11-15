import { linearScale, niceAxis } from '@/domain/chartScale';
import { maxDayTotal, type DayBar } from '@/domain/dailyChart';
import { satelliteStyle, SATELLITES } from '@/domain/satellites';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './DailyChart.module.css';

const W = 700;
const H = 240;
const M = { top: 10, right: 8, bottom: 40, left: 48 };

interface Props {
  bars: readonly DayBar[];
  selectedDate: string | null;
  onSelect: (date: string) => void;
}

/**
 * Hotspot giorno per giorno, impilati per satellite. Sotto l'asse, un segno per ogni giorno in
 * cui un satellite non ha osservato (o solo in parte): un giorno senza barra non è un giorno senza fuoco.
 */
export function DailyChart({ bars, selectedDate, onSelect }: Props) {
  const { t, fmt } = useI18n();
  const axis = niceAxis(maxDayTotal(bars));
  const y = linearScale([0, axis.top], [H - M.bottom, M.top]);
  const slot = (W - M.left - M.right) / Math.max(bars.length, 1);

  return (
    <div>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        className={styles.svg}
        role="group"
        aria-label={t('detail.chartLabel')}
      >
        {axis.ticks.map((tick) => (
          <g key={tick}>
            <line x1={M.left} x2={W - M.right} y1={y(tick)} y2={y(tick)} className={styles.grid} />
            <text x={M.left - 6} y={y(tick)} className={styles.tickY} dominantBaseline="middle">
              {fmt.compact(tick)}
            </text>
          </g>
        ))}
        {bars.map((bar, i) => {
          const x = M.left + slot * i;
          let top = H - M.bottom;
          const selected = bar.date === selectedDate;
          const label =
            t('detail.dayLabel', { day: bar.day, total: fmt.count(bar.total) }) +
            bar.gaps
              .map(
                (g) =>
                  `, ${satelliteStyle(g.code).label} ${t(g.status === 'MISSING' ? 'detail.gapMissing' : 'detail.gapPartial')}`,
              )
              .join('');
          return (
            <g key={bar.date}>
              {selected ? (
                <rect
                  x={x}
                  width={slot}
                  y={M.top}
                  height={H - M.bottom - M.top}
                  className={styles.selection}
                />
              ) : null}
              {bar.segments.map((seg) => {
                const height = y(0) - y(seg.count);
                top -= height;
                return (
                  <rect
                    key={seg.code}
                    x={x + slot * 0.12}
                    width={slot * 0.76}
                    y={top}
                    height={height}
                    fill={satelliteStyle(seg.code).color}
                  />
                );
              })}
              {bar.gaps.map((gap, g) => (
                <rect
                  key={gap.code}
                  x={x + slot * 0.12}
                  width={slot * 0.76}
                  y={H - M.bottom + 4 + g * 4}
                  height={3}
                  fill={satelliteStyle(gap.code).color}
                  fillOpacity={gap.status === 'MISSING' ? 0.35 : 0.8}
                />
              ))}
              <text x={x + slot / 2} y={H - M.bottom + 4 + 5 * 4 + 6} className={styles.tickX}>
                {bar.day % 2 === 1 || bars.length < 16 ? bar.day : ''}
              </text>
              <rect
                x={x}
                width={slot}
                y={M.top}
                height={H - M.top}
                className={styles.hit}
                role="button"
                tabIndex={0}
                aria-label={label}
                aria-pressed={selected}
                onClick={() => onSelect(bar.date)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    onSelect(bar.date);
                  }
                }}
              >
                <title>{label}</title>
              </rect>
            </g>
          );
        })}
      </svg>
      <ul className={styles.legend}>
        {SATELLITES.map((s) => (
          <li key={s.code}>
            <i style={{ background: s.color }} aria-hidden="true" />
            {s.label}
          </li>
        ))}
        <li className={styles.legendNote}>{t('detail.gapsLegend')}</li>
      </ul>
    </div>
  );
}
