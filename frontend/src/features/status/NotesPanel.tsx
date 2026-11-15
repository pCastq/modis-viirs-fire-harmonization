import { Card } from '@/components/Card';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './NotesPanel.module.css';

interface Props {
  notes: readonly string[];
}

/** Le avvertenze dell'API (metodo provvisorio, risoluzione, filtri): la trasparenza sul dato. */
export function NotesPanel({ notes }: Props) {
  const { t } = useI18n();
  if (notes.length === 0) return null;
  return (
    <Card id="notes" title={t('notes.title')}>
      <ul className={styles.list}>
        {notes.map((note) => (
          <li key={note}>{note}</li>
        ))}
      </ul>
    </Card>
  );
}
