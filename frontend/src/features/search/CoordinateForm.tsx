import { useState, type FormEvent } from 'react';
import { Button } from '@/components/Button';
import { boundsProblem, type Bounds, type BoundsProblem } from '@/domain/geometry';
import { useI18n } from '@/i18n/I18nProvider';
import styles from './CoordinateForm.module.css';

interface Props {
  initial: Bounds | null;
  onApply: (bounds: Bounds) => void;
}

type Draft = Record<keyof Bounds, string>;

const FIELDS = ['north', 'south', 'west', 'east'] as const satisfies readonly (keyof Bounds)[];

/** Cosa non va nel rettangolo: un problema dei valori (`range`, `order`) o campi vuoti. */
type Problem = BoundsProblem | 'empty';

const PROBLEM_KEYS = {
  range: 'search.coordinates.errorRange',
  order: 'search.coordinates.errorOrder',
  empty: 'search.coordinates.errorEmpty',
} as const;

function toDraft(bounds: Bounds | null): Draft {
  const text = (n: number | undefined) => (n === undefined ? '' : String(n));
  return {
    north: text(bounds?.north),
    south: text(bounds?.south),
    west: text(bounds?.west),
    east: text(bounds?.east),
  };
}

/** Rettangolo di latitudine e longitudine digitato a mano, in alternativa al disegno. */
export function CoordinateForm({ initial, onApply }: Props) {
  const [draft, setDraft] = useState<Draft>(() => toDraft(initial));
  const { t } = useI18n();
  const [problem, setProblem] = useState<Problem | null>(null);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (Object.values(draft).some((v) => v.trim() === '')) {
      setProblem('empty');
      return;
    }
    const bounds: Bounds = {
      north: Number(draft.north.replace(',', '.')),
      south: Number(draft.south.replace(',', '.')),
      west: Number(draft.west.replace(',', '.')),
      east: Number(draft.east.replace(',', '.')),
    };
    const found = boundsProblem(bounds);
    setProblem(found);
    if (!found) onApply(bounds);
  };

  return (
    <form className={styles.form} onSubmit={submit} noValidate>
      <div className={styles.grid}>
        {FIELDS.map((key) => (
          <label key={key} className={styles.field}>
            <span>{t(`search.coordinates.${key}`)}</span>
            <input
              inputMode="decimal"
              value={draft[key]}
              onChange={(e) => setDraft({ ...draft, [key]: e.target.value })}
              aria-invalid={problem !== null}
            />
          </label>
        ))}
      </div>
      {problem ? (
        <p className={styles.problem} role="alert">
          {t(PROBLEM_KEYS[problem])}
        </p>
      ) : null}
      <Button type="submit" variant="primary">
        {t('search.coordinates.apply')}
      </Button>
    </form>
  );
}
