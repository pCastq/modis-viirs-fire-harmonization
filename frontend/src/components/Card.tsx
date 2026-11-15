import type { ReactNode } from 'react';
import styles from './Card.module.css';

interface Props {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  children: ReactNode;
  /** Identificatore per collegare la sezione al suo titolo (accessibilita'). */
  id?: string;
}

export function Card({ title, subtitle, actions, children, id }: Props) {
  const headingId = id ? `${id}-title` : undefined;
  return (
    <section className={styles.card} aria-labelledby={headingId} id={id}>
      <header className={styles.header}>
        <div>
          <h2 className={styles.title} id={headingId}>
            {title}
          </h2>
          {subtitle ? <p className={styles.subtitle}>{subtitle}</p> : null}
        </div>
        {actions ? <div className={styles.actions}>{actions}</div> : null}
      </header>
      {children}
    </section>
  );
}
