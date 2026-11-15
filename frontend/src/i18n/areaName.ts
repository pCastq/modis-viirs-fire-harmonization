import type { AreaInfo } from '@/api/types';
import type { Translator } from './translate';

/** Il nome del paese e' un nome proprio dell'API; l'area disegnata la nomina l'interfaccia. */
export function areaName(area: AreaInfo, t: Translator): string {
  return area.kind === 'country' ? area.label.replaceAll('_', ' ') : t('search.area.drawnArea');
}
