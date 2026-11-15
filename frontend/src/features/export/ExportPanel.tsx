import { ApiError } from '@/api/client';
import { Button } from '@/components/Button';
import { Card } from '@/components/Card';
import { errorText } from '@/components/errorText';
import { Notice } from '@/components/Notice';
import type { SearchQuery } from '@/domain/query';
import { useCsvExport } from '@/hooks/queries';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './ExportPanel.module.css';

interface Props {
  query: SearchQuery;
  focusMonth: string | null;
}

/** I CSV richiesti dalla sfida: i punti rilevati e i conteggi mensili della ricerca. */
export function ExportPanel({ query, focusMonth }: Props) {
  const { t, fmt } = useI18n();
  const exporter = useCsvExport(query);
  const busy = exporter.isPending ? exporter.variables : null;
  const tooLarge = exporter.error instanceof ApiError && exporter.error.status === 413;
  const message = exporter.error ? errorText(exporter.error, t) : null;

  return (
    <Card title={t('export.title')} id="export" subtitle={t('export.subtitle')}>
      <div className={styles.buttons}>
        <Button
          variant="primary"
          busy={busy?.kind === 'monthly'}
          onClick={() => exporter.mutate({ kind: 'monthly' })}
        >
          {t('export.monthly')}
        </Button>
        <Button
          busy={busy?.kind === 'hotspots' && !busy.month}
          onClick={() => exporter.mutate({ kind: 'hotspots' })}
        >
          {t('export.hotspots')}
        </Button>
        {focusMonth ? (
          <Button
            busy={busy?.kind === 'hotspots' && busy.month === focusMonth}
            onClick={() => exporter.mutate({ kind: 'hotspots', month: focusMonth })}
          >
            {t('export.hotspotsOf', { month: fmt.monthKey(focusMonth) })}
          </Button>
        ) : null}
      </div>
      {message ? (
        <Notice tone={tooLarge ? 'warn' : 'error'} title={message.title}>
          {message.detail}
          {tooLarge ? t('export.tooLargeHint') : ''}
        </Notice>
      ) : null}
    </Card>
  );
}
