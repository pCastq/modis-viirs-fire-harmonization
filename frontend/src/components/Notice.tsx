import type { ReactNode } from 'react';
import styles from './Notice.module.css';

export type NoticeTone = 'info' | 'warn' | 'error';

interface Props {
  tone?: NoticeTone;
  title?: string;
  children: ReactNode;
}

/** Messaggio in evidenza. Gli errori sono annunciati subito dagli screen reader. */
export function Notice({ tone = 'info', title, children }: Props) {
  return (
    <div className={`${styles.notice} ${styles[tone]}`} role={tone === 'error' ? 'alert' : 'note'}>
      {title ? <strong className={styles.title}>{title}</strong> : null}
      <div>{children}</div>
    </div>
  );
}
