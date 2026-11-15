import { classifyAnomaly } from './anomaly';

describe('classifyAnomaly', () => {
  it.each([
    [null, null],
    [2.4, 'veryHigh'],
    [1.0, 'high'],
    [0.99, 'typical'],
    [-0.99, 'typical'],
    [-1.5, 'low'],
    [-2, 'veryLow'],
  ])('z=%s -> %s', (z, level) => {
    expect(classifyAnomaly(z)).toBe(level);
  });
});
