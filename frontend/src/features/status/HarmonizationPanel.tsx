import type { FactorDayNight, FactorSource, HarmonizationResponse } from '@/api/types';
import { Card } from '@/components/Card';
import { Notice } from '@/components/Notice';
import { QueryBoundary } from '@/components/QueryBoundary';
import {
  DAY_NIGHT_COLUMNS,
  factorRows,
  hasAnyFactor,
  isBorrowed,
  quarterMonths,
} from '@/domain/harmonization';
import type { SearchQuery } from '@/domain/query';
import { useI18n } from '@/i18n/I18nProvider';
import { useHarmonization } from '@/hooks/queries';
import styles from './HarmonizationPanel.module.css';

interface Props {
  query: SearchQuery;
}

const COLUMN_LABEL = {
  ANY: 'harmonization.all',
  DAY: 'harmonization.day',
  NIGHT: 'harmonization.night',
} as const satisfies Record<
  FactorDayNight,
  'harmonization.all' | 'harmonization.day' | 'harmonization.night'
>;

/**
 * La trasparenza sull'armonizzazione: i moltiplicatori k che il metodo applica al paese scelto, uno per trimestre e per
 * momento della giornata, e da dove vengono. Sono gli stessi che usa il calendario.
 */
export function HarmonizationPanel({ query }: Props) {
  const { t } = useI18n();
  const harmonization = useHarmonization(query);
  if (query.area.kind !== 'country') {
    return (
      <Card id="harmonization" title={t('harmonization.title')}>
        <Notice>{t('harmonization.freeArea')}</Notice>
      </Card>
    );
  }
  const country = query.area.code.replaceAll('_', ' ');
  return (
    <Card
      id="harmonization"
      title={t('harmonization.title')}
      subtitle={t('harmonization.subtitle', { area: country })}
    >
      <QueryBoundary
        {...harmonization}
        onRetry={() => harmonization.refetch()}
        skeletonHeight={220}
      >
        {(data) => <FactorTable data={data} />}
      </QueryBoundary>
    </Card>
  );
}

function FactorTable({ data }: { data: HarmonizationResponse }) {
  const { t, fmt } = useI18n();
  const rows = factorRows(data.factors);
  const borrowedText = (source: FactorSource | null) =>
    source === 'REGION' ? t('harmonization.borrowedRegion') : t('harmonization.borrowedGlobal');
  return (
    <>
      <p className={styles.intro}>{t('harmonization.intro')}</p>
      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <caption>{t('harmonization.tableCaption')}</caption>
          <thead>
            <tr>
              <th scope="col">{t('harmonization.quarter')}</th>
              {DAY_NIGHT_COLUMNS.map((column) => (
                <th key={column} scope="col">
                  {t(COLUMN_LABEL[column])}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => {
              const [first, last] = quarterMonths(row.quarter);
              return (
                <tr key={row.quarter}>
                  <th scope="row">
                    {t('harmonization.quarterRange', {
                      from: fmt.monthShort(first),
                      to: fmt.monthShort(last),
                    })}
                  </th>
                  {DAY_NIGHT_COLUMNS.map((column) => {
                    const cell = row.cells[column];
                    if (!cell || cell.k === null) {
                      return (
                        <td key={column} className={styles.missing}>
                          <span aria-label={t('harmonization.notCalibrated')}>
                            {fmt.factor(null)}
                          </span>
                        </td>
                      );
                    }
                    const borrowed = isBorrowed(cell.source);
                    const title = borrowed
                      ? `${borrowedText(cell.source)}. ${t('harmonization.cellTitle', { viirs: fmt.count(cell.viirsCellDays), modis: fmt.count(cell.modisCellDays) })}`
                      : t('harmonization.cellTitle', {
                          viirs: fmt.count(cell.viirsCellDays),
                          modis: fmt.count(cell.modisCellDays),
                        });
                    return (
                      <td
                        key={column}
                        title={title}
                        className={borrowed ? styles.borrowed : styles.k}
                      >
                        ×{fmt.factor(cell.k)}
                        {borrowed ? '*' : ''}
                      </td>
                    );
                  })}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <ul className={styles.notes}>
        <li>
          {data.calibration && hasAnyFactor(data.factors)
            ? t('harmonization.calibration', {
                from: fmt.monthKey(data.calibration.from),
                to: fmt.monthKey(data.calibration.to),
                months: data.calibration.monthsUsed,
                method: `${data.method.id}@${data.method.version}`,
              })
            : t('harmonization.noCalibration')}
        </li>
        {data.factors.some((f) => isBorrowed(f.source)) ? (
          <li>* {t('harmonization.borrowedFootnote')}</li>
        ) : null}
        <li>{t('harmonization.unitNote')}</li>
      </ul>
      <div className={styles.bands}>
        <p className={styles.bandsTitle}>{t('harmonization.bandsTitle')}</p>
        <ul className={styles.bandList}>
          {data.uncertaintyBands.map((band) => (
            <li key={band.level} className={styles.band}>
              {t('harmonization.bandLabel', { level: band.level })}: ±
              {fmt.percent(band.relativeUncertainty)}
            </li>
          ))}
        </ul>
      </div>
    </>
  );
}
