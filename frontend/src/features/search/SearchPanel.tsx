import type { Metadata } from '@/api/types';
import { Card } from '@/components/Card';
import { useI18n } from '@/i18n/I18nProvider';
import type { AppActions } from '@/state/useAppState';
import type { SearchQuery } from '@/domain/query';
import { AreaPicker } from './AreaPicker';
import { FilterPanel } from './FilterPanel';
import { PeriodPicker } from './PeriodPicker';
import styles from './SearchPanel.module.css';

interface Props {
  query: SearchQuery;
  metadata: Metadata;
  actions: Pick<AppActions, 'setArea' | 'setPeriod' | 'setFilters'>;
}

/** Colonna di sinistra: dove, quando, cosa contare. */
export function SearchPanel({ query, metadata, actions }: Props) {
  const { t } = useI18n();
  return (
    <div className={styles.panel}>
      <Card title={t('search.where')} id="area">
        <AreaPicker
          area={query.area}
          boundaries={metadata.boundaries}
          maxVertices={metadata.limits.maxPolygonVertices}
          onChange={actions.setArea}
        />
      </Card>
      <Card title={t('search.when')} id="period">
        <PeriodPicker
          period={query.period}
          firstMonth={metadata.firstMonth}
          lastMonth={metadata.lastMonth}
          onChange={actions.setPeriod}
        />
      </Card>
      <Card title={t('search.what')} id="filters">
        <FilterPanel filters={query.filters} onChange={actions.setFilters} />
      </Card>
    </div>
  );
}
