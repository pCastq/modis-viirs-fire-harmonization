import type { ReactNode } from 'react';
import { ApiError } from '@/api/client';
import { useI18n } from '@/i18n/I18nProvider';
import { Button } from './Button';
import { errorText } from './errorText';
import { Notice } from './Notice';
import { Skeleton } from './Skeleton';

interface Props<T> {
  data: T | undefined;
  isPending: boolean;
  error: Error | null;
  onRetry?: () => void;
  skeletonHeight?: number;
  children: (data: T) => ReactNode;
}

/** Decide cosa mostrare di una richiesta: attesa, errore comprensibile (con "Riprova") o dati. */
export function QueryBoundary<T>({
  data,
  isPending,
  error,
  onRetry,
  skeletonHeight,
  children,
}: Props<T>) {
  const { t } = useI18n();
  if (data !== undefined) return <>{children(data)}</>;
  if (error) {
    const { title, detail } = errorText(error, t);
    const retryable = !(error instanceof ApiError) || error.isNetwork || error.status >= 500;
    return (
      <Notice tone="error" title={title}>
        <p>{detail}</p>
        {onRetry && retryable ? (
          <Button variant="secondary" onClick={onRetry}>
            {t('common.retry')}
          </Button>
        ) : null}
      </Notice>
    );
  }
  return isPending ? <Skeleton height={skeletonHeight} /> : null;
}
