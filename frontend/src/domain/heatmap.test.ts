import type { CalendarResponse } from '@/api/types';
import {
  buildCells,
  latestComparableMonth,
  maxValue,
  median,
  typicalByMonth,
  yearTotal,
} from './heatmap';

function calendar(values: (number | null)[][], incomplete?: boolean[][]): CalendarResponse {
  const years = values.map((_, i) => 2019 + i);
  const months = Array.from({ length: values[0]!.length }, (_, i) => i + 1);
  const falses = values.map((row) => row.map(() => false));
  return {
    years,
    months,
    values,
    incomplete: incomplete ?? falses,
    provisional: falses,
  } as unknown as CalendarResponse;
}

describe('median', () => {
  it('handles empty, odd and even lists', () => {
    expect(median([])).toBeNull();
    expect(median([5, 1, 3])).toBe(3);
    expect(median([4, 1, 3, 2])).toBe(2.5);
  });
});

describe('typicalByMonth', () => {
  it('needs at least three comparable years', () => {
    expect(typicalByMonth(calendar([[10], [20]]))).toEqual([null]);
    expect(typicalByMonth(calendar([[10], [20], [30]]))).toEqual([20]);
  });

  it('ignores incomplete and missing months', () => {
    const values = [[10], [20], [999], [30], [null]];
    const incomplete = [[false], [false], [true], [false], [false]];
    expect(typicalByMonth(calendar(values, incomplete))).toEqual([20]);
  });
});

describe('buildCells', () => {
  it('keeps missing data distinct from zero and computes the ratio to the typical month', () => {
    const cells = buildCells(
      calendar([
        [10, null],
        [20, null],
        [40, 0],
      ]),
    );
    expect(cells[2]![0]!.ratio).toBeCloseTo(2); // 40 / mediana(10,20,40)=20
    expect(cells[0]![1]!.value).toBeNull();
    expect(cells[2]![1]!.value).toBe(0);
    expect(cells[2]![1]!.ratio).toBeNull(); // una sola osservazione: niente "tipico"
  });
});

describe('maxValue / yearTotal', () => {
  it('maxValue skips incomplete months', () => {
    const cells = buildCells(calendar([[10], [99]], [[false], [true]]));
    expect(maxValue(cells)).toBe(10);
  });

  it('yearTotal is null when nothing was observed', () => {
    const cells = buildCells(
      calendar([
        [null, null],
        [1, 2],
      ]),
    );
    expect(yearTotal(cells[0]!)).toBeNull();
    expect(yearTotal(cells[1]!)).toBe(3);
  });
});

describe('latestComparableMonth', () => {
  it('skips missing and incomplete months at the end', () => {
    const cells = buildCells(
      calendar(
        [
          [1, 2, 3],
          [4, 5, null],
        ],
        [
          [false, false, false],
          [false, true, false],
        ],
      ),
    );
    expect(latestComparableMonth(cells)).toEqual({ year: 2019 + 1, month: 1 });
  });

  it('is null when nothing is comparable', () => {
    expect(latestComparableMonth(buildCells(calendar([[null]])))).toBeNull();
  });
});
