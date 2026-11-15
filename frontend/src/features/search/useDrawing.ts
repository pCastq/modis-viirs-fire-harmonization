import { useCallback, useState } from 'react';
import { drawReducer, IDLE, type DrawMode, type DrawState } from '@/domain/drawing';
import type { LonLat } from '@/domain/geometry';

/** Collega la macchina a stati del disegno a React; chiama `onComplete` quando l'area è pronta. */
export function useDrawing(onComplete: (ring: readonly LonLat[]) => void) {
  const [state, setState] = useState<DrawState>(IDLE);

  const dispatch = useCallback(
    (action: Parameters<typeof drawReducer>[1]) => {
      const step = drawReducer(state, action);
      setState(step.state);
      if (step.completed) onComplete(step.completed);
    },
    [state, onComplete],
  );

  return {
    state,
    start: (mode: DrawMode) => dispatch({ type: 'start', mode }),
    click: (point: LonLat) => dispatch({ type: 'click', point }),
    finish: () => dispatch({ type: 'finish' }),
    cancel: () => dispatch({ type: 'cancel' }),
  };
}
