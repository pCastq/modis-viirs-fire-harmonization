import type { AnnualTotal, Trend } from '@/api/types';
import { senLine, trendDirection } from './trend';

const total = (year: number, value: number): AnnualTotal => ({
  year,
  total: value,
  provisional: false,
  relativeUncertainty: null,
});

describe('senLine', () => {
  it('passes through points that lie on a line', () => {
    const line = senLine([total(2000, 10), total(2001, 20), total(2002, 30)], 10)!;
    expect(line(2003)).toBeCloseTo(40);
  });

  it('is robust to one outlier (intercept is a median)', () => {
    const line = senLine(
      [total(2000, 10), total(2001, 20), total(2002, 30), total(2003, 1000)],
      10,
    )!;
    expect(line(2000)).toBeCloseTo(10);
  });

  it('needs at least two years', () => {
    expect(senLine([total(2000, 1)], 1)).toBeNull();
  });
});

describe('trendDirection', () => {
  const base: Trend = {
    senSlope: -5,
    mannKendallS: 0,
    zScore: 0,
    pValue: 0.5,
    years: 20,
    fewYears: false,
    significantAt5Percent: false,
  };
  it('only calls a direction when significant', () => {
    expect(trendDirection(base)).toBe('flat');
    expect(trendDirection({ ...base, significantAt5Percent: true })).toBe('down');
    expect(trendDirection({ ...base, senSlope: 5, significantAt5Percent: true })).toBe('up');
  });
});
