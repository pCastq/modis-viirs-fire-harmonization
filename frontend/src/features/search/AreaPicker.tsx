import { useCallback, useMemo, useState } from 'react';
import type { BoundaryInfo } from '@/api/types';
import { Button } from '@/components/Button';
import { Notice } from '@/components/Notice';
import { Segmented, type SegmentedOption } from '@/components/Segmented';
import { boundsToRing, type Bounds, type LonLat } from '@/domain/geometry';
import type { AreaSelection } from '@/domain/query';
import { useI18n } from '@/i18n/I18nProvider';
import { AreaMap } from './AreaMap';
import { CoordinateForm } from './CoordinateForm';
import { useDrawing } from './useDrawing';
import styles from './AreaPicker.module.css';

type Method = 'country' | 'draw' | 'coords';

interface Props {
  area: AreaSelection;
  boundaries: readonly BoundaryInfo[];
  maxVertices: number;
  onChange: (area: AreaSelection) => void;
}

function boundsOf(area: AreaSelection, boundaries: readonly BoundaryInfo[]): Bounds | null {
  if (area.kind !== 'country') return null;
  return boundaries.find((b) => b.code === area.code) ?? null;
}

/** Tre modi per dire "dove": un paese dall'elenco, un'area disegnata sulla mappa, o le coordinate. */
export function AreaPicker({ area, boundaries, maxVertices, onChange }: Props) {
  const { t } = useI18n();
  const methods: SegmentedOption<Method>[] = [
    { value: 'country', label: t('search.area.country') },
    { value: 'draw', label: t('search.area.draw') },
    { value: 'coords', label: t('search.area.coordinates') },
  ];
  const [method, setMethod] = useState<Method>(area.kind === 'polygon' ? 'draw' : 'country');
  const [tooMany, setTooMany] = useState(false);

  const complete = useCallback(
    (ring: readonly LonLat[]) => {
      const tooLong = ring.length > maxVertices;
      setTooMany(tooLong);
      if (!tooLong) onChange({ kind: 'polygon', ring });
    },
    [maxVertices, onChange],
  );
  const drawing = useDrawing(complete);

  const bounds = useMemo(() => boundsOf(area, boundaries), [area, boundaries]);
  const ring = area.kind === 'polygon' ? area.ring : null;

  const chooseMethod = (next: Method) => {
    drawing.cancel();
    setTooMany(false);
    setMethod(next);
  };

  return (
    <div className={styles.picker}>
      <Segmented
        legend={t('search.area.howToChoose')}
        hideLegend
        value={method}
        options={methods}
        onChange={chooseMethod}
      />

      {method === 'country' ? (
        <label className={styles.field}>
          <span>{t('search.area.country')}</span>
          <select
            value={area.kind === 'country' ? area.code : ''}
            onChange={(e) => onChange({ kind: 'country', code: e.target.value })}
          >
            {area.kind !== 'country' ? (
              <option value="">{t('search.area.drawnArea')}</option>
            ) : null}
            {boundaries.map((b) => (
              <option key={b.code} value={b.code}>
                {b.name}
              </option>
            ))}
          </select>
        </label>
      ) : null}

      {method === 'draw' ? (
        <div className={styles.tools}>
          <div className={styles.row}>
            <Button
              onClick={() => drawing.start('rectangle')}
              aria-pressed={drawing.state.mode === 'rectangle'}
            >
              {t('search.area.rectangle')}
            </Button>
            <Button
              onClick={() => drawing.start('polygon')}
              aria-pressed={drawing.state.mode === 'polygon'}
            >
              {t('search.area.polygon')}
            </Button>
            {drawing.state.mode === 'polygon' ? (
              <Button
                variant="primary"
                onClick={drawing.finish}
                disabled={drawing.state.points.length < 3}
              >
                {t('search.area.closeArea')}
              </Button>
            ) : null}
            {drawing.state.mode ? (
              <Button variant="ghost" onClick={drawing.cancel}>
                {t('search.area.cancel')}
              </Button>
            ) : null}
          </div>
          <p className={styles.hint} aria-live="polite">
            {drawing.state.mode === 'rectangle'
              ? t('search.area.hintRectangle')
              : drawing.state.mode === 'polygon'
                ? t('search.area.hintPolygon', { count: drawing.state.points.length })
                : t('search.area.hintIdle')}
          </p>
        </div>
      ) : null}

      {method === 'coords' ? (
        <CoordinateForm
          initial={bounds ?? null}
          onApply={(b) => onChange({ kind: 'polygon', ring: boundsToRing(b) })}
        />
      ) : null}

      {tooMany ? (
        <Notice tone="warn">{t('search.area.tooManyVertices', { max: maxVertices })}</Notice>
      ) : null}

      <AreaMap
        ring={ring}
        bounds={bounds}
        drawMode={drawing.state.mode}
        draftPoints={drawing.state.points}
        onPoint={drawing.click}
      />
    </div>
  );
}
