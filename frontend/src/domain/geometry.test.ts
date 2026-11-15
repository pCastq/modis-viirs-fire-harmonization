import {
  approxAreaKm2,
  boundsProblem,
  boundsToRing,
  geometryToRing,
  ringBounds,
  ringToGeometry,
} from './geometry';

describe('geometry', () => {
  const bounds = { west: 10, south: 40, east: 11, north: 41 };

  it('turns bounds into a ring and back', () => {
    expect(ringBounds(boundsToRing(bounds))).toEqual(bounds);
  });

  it('closes the GeoJSON ring and reopens it', () => {
    const ring = boundsToRing(bounds);
    const geometry = ringToGeometry(ring);
    expect(geometry.coordinates[0]).toHaveLength(5);
    expect(geometry.coordinates[0]![0]).toEqual(geometry.coordinates[0]![4]);
    expect(geometryToRing(geometry)).toEqual(ring);
  });

  it('validates manually entered bounds', () => {
    expect(boundsProblem(bounds)).toBeNull();
    expect(boundsProblem({ ...bounds, west: -181 })).toBe('range');
    expect(boundsProblem({ ...bounds, north: 91 })).toBe('range');
    expect(boundsProblem({ ...bounds, west: Number.NaN })).toBe('range');
    expect(boundsProblem({ ...bounds, east: 9 })).toBe('order');
    expect(boundsProblem({ ...bounds, north: 40 })).toBe('order');
  });

  it('approximates the area of one degree at the equator as ~12.300 km2', () => {
    expect(approxAreaKm2({ west: 0, south: 0, east: 1, north: 1 })).toBeGreaterThan(12000);
    expect(approxAreaKm2({ west: 0, south: 0, east: 1, north: 1 })).toBeLessThan(12500);
  });
});
