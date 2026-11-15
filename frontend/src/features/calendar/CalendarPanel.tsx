import { useMemo, useState } from 'react';
import type { CalendarResponse, SatelliteCode } from '@/api/types';
import { Card } from '@/components/Card';
import { Segmented, type SegmentedOption } from '@/components/Segmented';
import { buildCells, maxValue, type HeatCell } from '@/domain/heatmap';
import { monthKey } from '@/domain/months';
import { areaName } from '@/i18n/areaName';
import { useI18n } from '@/i18n/I18nProvider';
import { CalendarHeatmap, type HeatView } from './CalendarHeatmap';
import { CellInspector } from './CellInspector';
import { HeatmapLegend } from './HeatmapLegend';
import styles from './CalendarPanel.module.css';

interface Props {
  calendar: CalendarResponse;
  selectedMonth: string | null;
  onSelectMonth: (year: number, month: number) => void;
}

function satelliteCounts(calendar: CalendarResponse, cell: HeatCell | null) {
  const counts: Partial<Record<SatelliteCode, number | null>> = {};
  if (!cell) return counts;
  const y = calendar.years.indexOf(cell.year);
  const m = calendar.months.indexOf(cell.month);
  for (const [code, matrix] of Object.entries(calendar.bySatellite) as [
    SatelliteCode,
    (number | null)[][],
  ][]) {
    counts[code] = matrix[y]?.[m] ?? null;
  }
  return counts;
}

export function CalendarPanel({ calendar, selectedMonth, onSelectMonth }: Props) {
  const { t } = useI18n();
  const views: SegmentedOption<HeatView>[] = [
    { value: 'counts', label: t('calendar.viewCounts'), hint: t('calendar.viewCountsHint') },
    { value: 'typical', label: t('calendar.viewTypical'), hint: t('calendar.viewTypicalHint') },
  ];
  const [view, setView] = useState<HeatView>('counts');
  const [inspected, setInspected] = useState<HeatCell | null>(null);
  const cells = useMemo(() => buildCells(calendar), [calendar]);
  const max = useMemo(() => maxValue(cells), [cells]);
  const modeLabel = calendar.mode === 'HARMONIZED' ? t('modes.harmonized') : t('modes.raw');
  const inspectTarget = inspected ?? (selectedMonth ? findCell(cells, selectedMonth) : null);

  return (
    <Card
      id="calendar"
      title={t('calendar.title')}
      subtitle={t('calendar.subtitle', {
        area: areaName(calendar.area, t),
        mode: modeLabel,
        resolution: calendar.resolution,
      })}
      actions={
        <Segmented
          legend={t('calendar.colorBy')}
          hideLegend
          value={view}
          options={views}
          onChange={setView}
        />
      }
    >
      <CalendarHeatmap
        cells={cells}
        months={calendar.months}
        view={view}
        max={max}
        selectedMonth={selectedMonth}
        onSelect={onSelectMonth}
        onInspect={setInspected}
      />
      <div className={styles.footer}>
        <CellInspector
          cell={inspectTarget}
          bySatellite={satelliteCounts(calendar, inspectTarget)}
          modeLabel={modeLabel}
        />
        <HeatmapLegend view={view} max={max} />
      </div>
    </Card>
  );
}

function findCell(cells: readonly (readonly HeatCell[])[], key: string): HeatCell | null {
  for (const row of cells) {
    for (const cell of row) {
      if (monthKey(cell.year, cell.month) === key) return cell;
    }
  }
  return null;
}
