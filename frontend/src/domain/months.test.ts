import { clampMonthKey, daysInMonth, monthKey, parseMonthKey } from './months';

describe('months', () => {
  it('formats and parses keys', () => {
    expect(monthKey(2019, 8)).toBe('2019-08');
    expect(parseMonthKey('2019-08')).toEqual({ year: 2019, month: 8 });
    expect(parseMonthKey('2019-13')).toBeNull();
  });

  it('knows leap years', () => {
    expect(daysInMonth(2020, 2)).toBe(29);
    expect(daysInMonth(2019, 2)).toBe(28);
    expect(daysInMonth(2019, 12)).toBe(31);
  });

  it('clamps chronologically', () => {
    expect(clampMonthKey('1999-01', '2000-11', '2025-12')).toBe('2000-11');
    expect(clampMonthKey('2030-01', '2000-11', '2025-12')).toBe('2025-12');
    expect(clampMonthKey('2010-05', '2000-11', '2025-12')).toBe('2010-05');
  });
});
