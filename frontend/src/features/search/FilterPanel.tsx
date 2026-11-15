import type { ConfidenceFilter, DayNightFilter, Mode, SourceTypeFilter } from '@/api/types';
import { Segmented, type SegmentedOption } from '@/components/Segmented';
import type { Filters } from '@/domain/query';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './FilterPanel.module.css';

interface Props {
  filters: Filters;
  onChange: (filters: Filters) => void;
}

export function FilterPanel({ filters, onChange }: Props) {
  const { t } = useI18n();
  const set =
    <K extends keyof Filters>(key: K) =>
    (value: Filters[K]) =>
      onChange({ ...filters, [key]: value });

  const mode: SegmentedOption<Mode>[] = [
    {
      value: 'HARMONIZED',
      label: t('search.filters.harmonized'),
      hint: t('search.filters.harmonizedHint'),
    },
    { value: 'RAW', label: t('search.filters.raw'), hint: t('search.filters.rawHint') },
  ];
  const source: SegmentedOption<SourceTypeFilter>[] = [
    {
      value: 'VEGETATION',
      label: t('search.filters.vegetation'),
      hint: t('search.filters.vegetationHint'),
    },
    {
      value: 'ALL',
      label: t('search.filters.allSources'),
      hint: t('search.filters.allSourcesHint'),
    },
    {
      value: 'OTHER',
      label: t('search.filters.otherSources'),
      hint: t('search.filters.otherSourcesHint'),
    },
  ];
  const dayNight: SegmentedOption<DayNightFilter>[] = [
    { value: 'ANY', label: t('search.filters.any') },
    { value: 'D', label: t('search.filters.day') },
    { value: 'N', label: t('search.filters.night') },
  ];
  const confidence: SegmentedOption<ConfidenceFilter>[] = [
    { value: 'ALL', label: t('search.filters.confAll'), hint: t('search.filters.confAllHint') },
    {
      value: 'NOMINAL',
      label: t('search.filters.confNominal'),
      hint: t('search.filters.confNominalHint'),
    },
    { value: 'HIGH', label: t('search.filters.confHigh') },
  ];

  return (
    <div className={styles.panel}>
      <Segmented
        legend={t('search.filters.counts')}
        value={filters.mode}
        options={mode}
        onChange={set('mode')}
      />
      <Segmented
        legend={t('search.filters.source')}
        value={filters.sourceType}
        options={source}
        onChange={set('sourceType')}
      />
      <Segmented
        legend={t('search.filters.dayNight')}
        value={filters.dayNight}
        options={dayNight}
        onChange={set('dayNight')}
      />
      <Segmented
        legend={t('search.filters.confidence')}
        value={filters.minConfidence}
        options={confidence}
        onChange={set('minConfidence')}
      />
    </div>
  );
}
