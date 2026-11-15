import type { SatelliteCode } from '@/api/types';
import { capitalize } from '@/domain/format';
import type { HeatCell } from '@/domain/heatmap';
import { SATELLITES } from '@/domain/satellites';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './CellInspector.module.css';

interface Props {
  cell: HeatCell | null;
  /** Conteggi grezzi per satellite del mese ispezionato, quando noti. */
  bySatellite: Partial<Record<SatelliteCode, number | null>>;
  /** Come sono contati i valori: "armonizzato" / "grezzo", gia' nella lingua scelta. */
  modeLabel: string;
}

/**
 * Il dettaglio del mese sotto il cursore. Sta fuori dalla griglia (non e' un tooltip che copre
 * le celle) e viene annunciato agli screen reader.
 */
export function CellInspector({ cell, bySatellite, modeLabel }: Props) {
  const { t, fmt } = useI18n();
  if (!cell) {
    return <p className={styles.hint}>{t('calendar.inspector.hint')}</p>;
  }
  const satellites = SATELLITES.filter((s) => bySatellite[s.code] != null);
  return (
    <div className={styles.inspector} aria-live="polite">
      <strong className={styles.title}>
        {capitalize(fmt.monthName(cell.month))} {cell.year}
      </strong>
      <span>
        {t('calendar.inspector.hotspotsIn', { value: fmt.count(cell.value), mode: modeLabel })}
      </span>
      {cell.ratio !== null ? (
        <span>
          {t('calendar.inspector.versusTypical', { change: fmt.signedPercent(cell.ratio) })}{' '}
          <span className={styles.muted}>(×{fmt.decimal(cell.ratio)})</span>
        </span>
      ) : null}
      {cell.incomplete ? (
        <span className={styles.flag}>{t('calendar.inspector.incomplete')}</span>
      ) : null}
      {cell.provisional ? (
        <span className={styles.flag}>{t('calendar.inspector.provisional')}</span>
      ) : null}
      {satellites.length > 0 ? (
        <span className={styles.sats}>
          {satellites.map((s) => (
            <span key={s.code} className={styles.sat}>
              <i style={{ background: s.color }} aria-hidden="true" />
              {s.label} {fmt.count(bySatellite[s.code])}
            </span>
          ))}
        </span>
      ) : null}
    </div>
  );
}
