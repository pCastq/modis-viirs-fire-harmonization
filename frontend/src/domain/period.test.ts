import { isOrderedPeriod, lastYearsPeriod, periodForPreset } from './period';

describe('period', () => {
  it('takes the last N whole years', () => {
    expect(lastYearsPeriod('2025-12', '2000-11', 10)).toEqual({ from: '2016-01', to: null });
    expect(lastYearsPeriod('2025-06', '2000-11', 5)).toEqual({ from: '2021-01', to: null });
  });

  it('does not start before the data do', () => {
    expect(lastYearsPeriod('2005-12', '2000-11', 10)).toEqual({ from: null, to: null });
  });

  it('"all" means no bounds', () => {
    expect(periodForPreset(null, '2000-11', '2025-12')).toEqual({ from: null, to: null });
  });

  it('checks the order of the two ends', () => {
    expect(isOrderedPeriod({ from: '2010-01', to: '2009-12' })).toBe(false);
    expect(isOrderedPeriod({ from: '2010-01', to: null })).toBe(true);
    expect(isOrderedPeriod({ from: '2010-01', to: '2010-01' })).toBe(true);
  });
});
