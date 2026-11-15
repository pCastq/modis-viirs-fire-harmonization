import { boundsToRing, type LonLat } from './geometry';

export type DrawMode = 'rectangle' | 'polygon';

export interface DrawState {
  mode: DrawMode | null;
  points: readonly LonLat[];
}

export const IDLE: DrawState = { mode: null, points: [] };

export type DrawAction =
  | { type: 'start'; mode: DrawMode }
  | { type: 'click'; point: LonLat }
  | { type: 'finish' }
  | { type: 'cancel' };

export interface DrawStep {
  state: DrawState;
  /** L'anello completato, quando il disegno è finito. */
  completed?: readonly LonLat[];
}

export const MIN_POLYGON_POINTS = 3;

/**
 * Macchina a stati del disegno, senza nulla di grafico (quindi testabile):
 * rettangolo = due clic sugli angoli opposti; poligono = un clic per vertice e poi "finish".
 */
export function drawReducer(state: DrawState, action: DrawAction): DrawStep {
  switch (action.type) {
    case 'start':
      return { state: { mode: action.mode, points: [] } };
    case 'cancel':
      return { state: IDLE };
    case 'click': {
      if (!state.mode) return { state };
      const points = [...state.points, action.point];
      if (state.mode === 'rectangle' && points.length === 2) {
        const [a, b] = points as [LonLat, LonLat];
        const bounds = {
          west: Math.min(a[0], b[0]),
          east: Math.max(a[0], b[0]),
          south: Math.min(a[1], b[1]),
          north: Math.max(a[1], b[1]),
        };
        // due clic nello stesso punto non delimitano nulla: si resta in attesa del secondo angolo
        if (bounds.west === bounds.east || bounds.south === bounds.north) {
          return { state: { mode: state.mode, points: [a] } };
        }
        return { state: IDLE, completed: boundsToRing(bounds) };
      }
      return { state: { mode: state.mode, points } };
    }
    case 'finish':
      if (state.mode === 'polygon' && state.points.length >= MIN_POLYGON_POINTS) {
        return { state: IDLE, completed: state.points };
      }
      return { state };
  }
}
