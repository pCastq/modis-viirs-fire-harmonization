import type { FactorDayNight, HarmonizationFactor } from '@/api/types';
import { factorRows, hasAnyFactor, isBorrowed, quarterMonths } from './harmonization';

function factor(
  quarter: 1 | 2 | 3 | 4,
  dayNight: FactorDayNight,
  k: number | null,
): HarmonizationFactor {
  return {
    quarter,
    dayNight,
    k,
    source: k === null ? null : 'COUNTRY',
    viirsCellDays: 100,
    modisCellDays: 50,
  };
}

describe('quarterMonths', () => {
  it('gives the first and last month of each quarter', () => {
    expect([1, 2, 3, 4].map(quarterMonths)).toEqual([
      [1, 3],
      [4, 6],
      [7, 9],
      [10, 12],
    ]);
  });
});

describe('factorRows', () => {
  it('builds one row per quarter with a cell per day/night filter', () => {
    const rows = factorRows([factor(1, 'ANY', 2), factor(1, 'NIGHT', 5), factor(3, 'DAY', 1.5)]);
    expect(rows.map((r) => r.quarter)).toEqual([1, 2, 3, 4]);
    expect(rows[0]!.cells.ANY?.k).toBe(2);
    expect(rows[0]!.cells.NIGHT?.k).toBe(5);
    expect(rows[0]!.cells.DAY).toBeNull();
    expect(rows[2]!.cells.DAY?.k).toBe(1.5);
    expect(rows[1]!.cells.ANY).toBeNull();
  });

  it('copes with no factors at all', () => {
    expect(factorRows([])).toHaveLength(4);
  });
});

describe('isBorrowed', () => {
  it('flags only the levels coarser than the country', () => {
    expect(isBorrowed('COUNTRY')).toBe(false);
    expect(isBorrowed(null)).toBe(false);
    expect(isBorrowed('REGION')).toBe(true);
    expect(isBorrowed('GLOBAL')).toBe(true);
  });
});

describe('hasAnyFactor', () => {
  it('is false when every k is missing', () => {
    expect(hasAnyFactor([factor(1, 'ANY', null)])).toBe(false);
    expect(hasAnyFactor([factor(1, 'ANY', null), factor(2, 'ANY', 2)])).toBe(true);
  });
});
