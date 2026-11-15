import { Notice } from '@/components/Notice';
import { QueryBoundary } from '@/components/QueryBoundary';
import { Skeleton } from '@/components/Skeleton';
import { CalendarPanel } from '@/features/calendar/CalendarPanel';
import { MonthDetailPanel } from '@/features/detail/MonthDetailPanel';
import { ExportPanel } from '@/features/export/ExportPanel';
import { AnalysisGrid, SummaryStrip } from '@/features/insights/AnalysisSection';
import { SearchPanel } from '@/features/search/SearchPanel';
import { Header } from '@/features/status/Header';
import { NotesPanel } from '@/features/status/NotesPanel';
import { useCalendar, useMetadata } from '@/hooks/queries';
import { monthKey } from '@/domain/months';
import { useI18n } from '@/i18n/I18nProvider';
import { useAppState } from '@/state/useAppState';
import styles from './App.module.css';

export function App() {
  const { t } = useI18n();
  const { state, actions } = useAppState();
  const metadata = useMetadata();
  const calendar = useCalendar(state.query);
  const { query, focus } = state;

  return (
    <>
      <a className={styles.skip} href="#calendar">
        {t('app.skipToCalendar')}
      </a>
      <Header metadata={metadata.data} />
      <div className={styles.layout}>
        <aside className={styles.sidebar} aria-label="Ricerca">
          <QueryBoundary {...metadata} onRetry={() => metadata.refetch()} skeletonHeight={420}>
            {(meta) => <SearchPanel query={query} metadata={meta} actions={actions} />}
          </QueryBoundary>
        </aside>

        <main className={styles.main}>
          {metadata.data?.activeMethodIsProvisional ? (
            <Notice tone="warn" title={t('app.provisionalTitle')}>
              {t('app.provisionalBody')}
            </Notice>
          ) : null}

          <QueryBoundary {...calendar} onRetry={() => calendar.refetch()} skeletonHeight={520}>
            {(data) => (
              <>
                <SummaryStrip query={query} calendar={data} />
                <CalendarPanel
                  calendar={data}
                  selectedMonth={focus.month}
                  onSelectMonth={(year, month) =>
                    actions.setFocus({ month: monthKey(year, month), date: null })
                  }
                />
                {focus.month ? (
                  <MonthDetailPanel
                    query={query}
                    focus={{ ...focus, month: focus.month }}
                    onFocus={actions.setFocus}
                  />
                ) : null}
                <AnalysisGrid query={query} calendar={data} focus={focus} />
                <ExportPanel query={query} focusMonth={focus.month} />
                <NotesPanel notes={data.notes} />
              </>
            )}
          </QueryBoundary>
          {calendar.isFetching && calendar.data ? (
            <Skeleton height={4} label={t('app.updating')} />
          ) : null}
        </main>
      </div>
    </>
  );
}
