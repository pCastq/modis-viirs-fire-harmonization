import { useId } from 'react';
import styles from './Segmented.module.css';

export interface SegmentedOption<T extends string> {
  value: T;
  label: string;
  hint?: string;
}

interface Props<T extends string> {
  legend: string;
  value: T;
  options: readonly SegmentedOption<T>[];
  onChange: (value: T) => void;
  /** Nasconde la legenda alla vista ma la lascia agli screen reader. */
  hideLegend?: boolean;
}

/** Scelta esclusiva fra poche opzioni: gruppo di radio con aspetto a pulsanti. */
export function Segmented<T extends string>({
  legend,
  value,
  options,
  onChange,
  hideLegend,
}: Props<T>) {
  const name = useId();
  return (
    <fieldset className={styles.fieldset}>
      <legend className={hideLegend ? 'sr-only' : styles.legend}>{legend}</legend>
      <div className={styles.group}>
        {options.map((option) => (
          <label key={option.value} className={styles.option} title={option.hint}>
            <input
              className="sr-only"
              type="radio"
              name={name}
              value={option.value}
              checked={option.value === value}
              onChange={() => onChange(option.value)}
            />
            <span className={styles.face}>{option.label}</span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}
