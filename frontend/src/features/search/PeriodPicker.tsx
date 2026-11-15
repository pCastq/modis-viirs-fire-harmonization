import { Notice } from '@/components/Notice';
import { useI18n } from '@/i18n/I18nProvider';
import { isOrderedPeriod, PERIOD_PRESETS, periodForPreset } from '@/domain/period';
import type { Period } from '@/domain/query';
import styles from './PeriodPicker.module.css';

interface Props {
  period: Period;
  firstMonth: string;
  lastMonth: string;
  onChange: (period: Period) => void;
}

export function PeriodPicker({ period, firstMonth, lastMonth, onChange }: Props) {
  const { t } = useI18n();
  const change = (key: keyof Period) => (value: string) => {
    onChange({ ...period, [key]: value === '' ? null : value });
  };
  const ordered = isOrderedPeriod(period);

  return (
    <div className={styles.period}>
      <div className={styles.dates}>
        <label className={styles.field}>
          <span>{t('search.period.from')}</span>
          <input
            type="month"
            min={firstMonth}
            max={lastMonth}
            value={period.from ?? ''}
            placeholder={firstMonth}
            onChange={(e) => change('from')(e.target.value)}
          />
        </label>
        <label className={styles.field}>
          <span>{t('search.period.to')}</span>
          <input
            type="month"
            min={firstMonth}
            max={lastMonth}
            value={period.to ?? ''}
            placeholder={lastMonth}
            onChange={(e) => change('to')(e.target.value)}
          />
        </label>
      </div>
      <div className={styles.presets} role="group" aria-label={t('search.period.quick')}>
        {PERIOD_PRESETS.map((preset) => (
          <button
            key={preset.id}
            type="button"
            className={styles.preset}
            onClick={() => onChange(periodForPreset(preset.years, firstMonth, lastMonth))}
          >
            {t(`search.period.${preset.id}`)}
          </button>
        ))}
      </div>
      {!ordered ? <Notice tone="warn">{t('search.period.orderWarning')}</Notice> : null}
    </div>
  );
}
