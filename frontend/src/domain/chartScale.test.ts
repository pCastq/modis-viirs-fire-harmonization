import { linearScale, niceAxis } from './chartScale';

describe('linearScale', () => {
  it('maps the domain onto the range, also inverted (SVG y axis)', () => {
    const y = linearScale([0, 100], [200, 0]);
    expect(y(0)).toBe(200);
    expect(y(50)).toBe(100);
    expect(y(100)).toBe(0);
  });

  it('does not divide by zero on an empty domain', () => {
    expect(linearScale([5, 5], [0, 10])(5)).toBe(0);
  });
});

describe('niceAxis', () => {
  it.each([
    [97, 100],
    [230, 250],
    [4200, 5000],
    [30000, 30000],
  ])('rounds %d up to a round top (%d) that covers the maximum', (max, top) => {
    const axis = niceAxis(max);
    expect(axis.top).toBe(top);
    expect(axis.top).toBeGreaterThanOrEqual(max);
    expect(axis.ticks[0]).toBe(0);
    expect(axis.ticks.at(-1)).toBe(axis.top);
  });

  it('copes with empty data', () => {
    expect(niceAxis(0).top).toBe(1);
  });
});
