import { useCallback, useEffect, useMemo, useState } from 'react';
import { NO_FOCUS, type Filters, type Focus, type Period, type SearchQuery } from '@/domain/query';
import type { AreaSelection } from '@/domain/query';
import { parseState, serializeState, type AppState } from './urlState';

export interface AppActions {
  setArea: (area: AreaSelection) => void;
  setPeriod: (period: Period) => void;
  setFilters: (filters: Filters) => void;
  setFocus: (focus: Focus) => void;
}

function readLocation(): AppState {
  return parseState(window.location.search);
}

/**
 * Unica fonte di verita' dello stato dell'app, sincronizzata con l'URL.
 * Cambiare area, periodo o filtri azzera l'approfondimento: il mese scelto potrebbe non esistere più.
 */
export function useAppState(): { state: AppState; actions: AppActions } {
  const [state, setState] = useState<AppState>(readLocation);

  useEffect(() => {
    const onPop = () => setState(readLocation());
    window.addEventListener('popstate', onPop);
    return () => window.removeEventListener('popstate', onPop);
  }, []);

  const commit = useCallback((next: AppState) => {
    const url = `${window.location.pathname}${serializeState(next)}`;
    window.history.pushState(null, '', url);
    setState(next);
  }, []);

  const actions = useMemo<AppActions>(() => {
    const withQuery = (patch: Partial<SearchQuery>) =>
      commit({ query: { ...state.query, ...patch }, focus: NO_FOCUS });
    return {
      setArea: (area) => withQuery({ area }),
      setPeriod: (period) => withQuery({ period }),
      setFilters: (filters) => withQuery({ filters }),
      setFocus: (focus) => commit({ query: state.query, focus }),
    };
  }, [commit, state]);

  return { state, actions };
}
