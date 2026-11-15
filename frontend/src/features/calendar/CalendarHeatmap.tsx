import { useRef, useState, type KeyboardEvent } from 'react';
import { divergingPaint, sequentialPaint, type CellPaint } from '@/domain/colorScale';
import { EM_DASH } from '@/domain/format';
import { moveInGrid } from '@/domain/gridNavigation';
import { yearTotal, type HeatCell } from '@/domain/heatmap';
import { monthKey } from '@/domain/months';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './CalendarHeatmap.module.css';

export type HeatView = 'counts' | 'typical';

interface Props {
  cells: readonly (readonly HeatCell[])[];
  months: readonly number[];
  view: HeatView;
  max: number;
  selectedMonth: string | null;
  onSelect: (year: number, month: number) => void;
  onInspect: (cell: HeatCell | null) => void;
}

function paintFor(cell: HeatCell, view: HeatView, max: number): CellPaint | null {
  if (cell.value === null) return null;
  if (view === 'typical') return cell.ratio === null ? null : divergingPaint(cell.ratio);
  return sequentialPaint(cell.value, max);
}

/**
 * Il calendario: una riga per anno, una colonna per mese. È una griglia accessibile (`role=grid`):
 * si naviga con le frecce e si apre il dettaglio con Invio. Il colore non è mai l'unica
 * informazione: ogni cella riporta il numero e i mesi incompleti o provvisori hanno un segno proprio.
 */
export function CalendarHeatmap({
  cells,
  months,
  view,
  max,
  selectedMonth,
  onSelect,
  onInspect,
}: Props) {
  const { t, fmt } = useI18n();

  const describe = (cell: HeatCell): string => {
    const month = fmt.monthName(cell.month);
    if (cell.value === null) return t('calendar.cellNoData', { month, year: cell.year });
    const flags = [
      cell.incomplete ? t('calendar.flagIncomplete') : null,
      cell.provisional ? t('calendar.flagProvisional') : null,
    ].filter(Boolean);
    const base = t('calendar.cellValue', { month, year: cell.year, value: fmt.count(cell.value) });
    return flags.length ? `${base}, ${flags.join(', ')}` : base;
  };

  const [active, setActive] = useState({ row: Math.max(0, cells.length - 1), col: 0 });
  const buttons = useRef(new Map<string, HTMLButtonElement>());
  const size = { rows: cells.length, cols: months.length };

  const onKeyDown = (event: KeyboardEvent) => {
    const next = moveInGrid(active, event.key, size);
    if (!next) return;
    event.preventDefault();
    setActive(next);
    buttons.current.get(`${next.row}:${next.col}`)?.focus();
  };

  return (
    <div className={styles.scroll}>
      <div
        className={styles.grid}
        role="grid"
        aria-label={t('calendar.gridLabel')}
        aria-rowcount={cells.length + 1}
        onKeyDown={onKeyDown}
        style={{ ['--cols' as string]: months.length }}
      >
        <div role="row" className={styles.row}>
          <span role="columnheader" className={styles.corner}>
            <span className="sr-only">{t('calendar.year')}</span>
          </span>
          {months.map((m) => (
            <span key={m} role="columnheader" className={styles.colHead} title={fmt.monthName(m)}>
              {fmt.monthShort(m)}
            </span>
          ))}
          <span role="columnheader" className={styles.colHead}>
            {t('calendar.total')}
          </span>
        </div>

        {cells.map((row, r) => (
          <div role="row" key={row[0]?.year ?? r} className={styles.row}>
            <span role="rowheader" className={styles.rowHead}>
              {row[0]?.year}
            </span>
            {row.map((cell, c) => {
              const paint = paintFor(cell, view, max);
              const key = monthKey(cell.year, cell.month);
              const classes = [
                styles.cell,
                cell.value === null ? styles.empty : '',
                cell.incomplete ? styles.incomplete : '',
                cell.provisional ? styles.provisional : '',
                key === selectedMonth ? styles.selected : '',
              ].join(' ');
              return (
                <span role="gridcell" key={c} className={styles.cellWrap}>
                  <button
                    type="button"
                    ref={(el) => {
                      if (el) buttons.current.set(`${r}:${c}`, el);
                      else buttons.current.delete(`${r}:${c}`);
                    }}
                    className={classes}
                    style={paint ? { background: paint.background, color: paint.text } : undefined}
                    tabIndex={active.row === r && active.col === c ? 0 : -1}
                    aria-label={describe(cell)}
                    aria-pressed={key === selectedMonth}
                    disabled={cell.value === null}
                    onClick={() => {
                      setActive({ row: r, col: c });
                      onSelect(cell.year, cell.month);
                    }}
                    onMouseEnter={() => onInspect(cell)}
                    onFocus={() => onInspect(cell)}
                    onMouseLeave={() => onInspect(null)}
                    onBlur={() => onInspect(null)}
                  >
                    {cell.value === null ? EM_DASH : fmt.compact(cell.value)}
                    {cell.incomplete ? (
                      <span className={styles.badge} aria-hidden="true">
                        !
                      </span>
                    ) : null}
                  </button>
                </span>
              );
            })}
            <span role="gridcell" className={styles.total}>
              {fmt.compact(yearTotal(row))}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
