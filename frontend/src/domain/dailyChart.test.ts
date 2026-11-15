import type { DayDetail } from '@/api/types';
import { maxDayTotal, toDayBars } from './dailyChart';

const days: DayDetail[] = [
  { date: '2019-08-01', bySatellite: { AQUA: 4, SNPP: 10 }, status: { AQUA: 'OK', SNPP: 'OK' } },
  { date: '2019-08-02', bySatellite: { SNPP: 7 }, status: { AQUA: 'MISSING', SNPP: 'PARTIAL' } },
];

describe('toDayBars', () => {
  it('stacks satellites in the fixed order and sums the day', () => {
    const [first] = toDayBars(days);
    expect(first!.segments.map((s) => s.code)).toEqual(['AQUA', 'SNPP']);
    expect(first!.total).toBe(14);
    expect(first!.gaps).toEqual([]);
    expect(first!.day).toBe(1);
  });

  it('reports missing and partial observations as gaps, not as zeros', () => {
    const second = toDayBars(days)[1]!;
    expect(second.segments).toEqual([{ code: 'SNPP', count: 7 }]);
    expect(second.gaps).toEqual([
      { code: 'AQUA', status: 'MISSING' },
      { code: 'SNPP', status: 'PARTIAL' },
    ]);
  });

  it('finds the tallest day', () => {
    expect(maxDayTotal(toDayBars(days))).toBe(14);
    expect(maxDayTotal([])).toBe(0);
  });
});
