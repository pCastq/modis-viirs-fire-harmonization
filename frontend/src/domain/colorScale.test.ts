import { divergingPaint, luminance, sequentialPaint } from './colorScale';

function contrast(a: number, b: number): number {
  const [hi, lo] = a > b ? [a, b] : [b, a];
  return (hi + 0.05) / (lo + 0.05);
}

describe('color scales', () => {
  it('sequential: lowest is light, highest is dark', () => {
    expect(sequentialPaint(0, 100).background).toBe('rgb(255 255 204)');
    expect(sequentialPaint(100, 100).background).toBe('rgb(128 0 38)');
  });

  it('sequential: values beyond the maximum saturate instead of failing', () => {
    expect(sequentialPaint(500, 100)).toEqual(sequentialPaint(100, 100));
    expect(sequentialPaint(5, 0).background).toBe('rgb(255 255 204)');
  });

  it('diverging: ratio 1 is neutral, extremes are the two hues', () => {
    expect(divergingPaint(1).background).toBe('rgb(247 247 247)');
    expect(divergingPaint(4).background).toBe('rgb(179 88 6)');
    expect(divergingPaint(0.25).background).toBe('rgb(84 39 136)');
    expect(divergingPaint(0).background).toBe('rgb(84 39 136)');
  });

  it('text over every sequential and diverging step has WCAG AA contrast (4.5:1)', () => {
    const samples = [...Array(21).keys()].map((i) => i / 20);
    const paints = [
      ...samples.map((t) => sequentialPaint(t * 100, 100)),
      ...samples.map((t) => divergingPaint(4 ** (2 * t - 1))),
    ];
    for (const p of paints) {
      const bg = p.background.match(/\d+/g)!.map(Number) as [number, number, number];
      const fg = p.text === '#ffffff' ? 1 : 0;
      expect(contrast(luminance(bg), fg)).toBeGreaterThanOrEqual(4.5);
    }
  });
});
