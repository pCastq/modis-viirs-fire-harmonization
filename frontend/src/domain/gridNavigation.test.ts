import { moveInGrid } from './gridNavigation';

const size = { rows: 3, cols: 12 };

describe('moveInGrid', () => {
  it('moves with the arrows and stops at the edges', () => {
    expect(moveInGrid({ row: 1, col: 5 }, 'ArrowRight', size)).toEqual({ row: 1, col: 6 });
    expect(moveInGrid({ row: 1, col: 11 }, 'ArrowRight', size)).toEqual({ row: 1, col: 11 });
    expect(moveInGrid({ row: 0, col: 0 }, 'ArrowUp', size)).toEqual({ row: 0, col: 0 });
    expect(moveInGrid({ row: 2, col: 3 }, 'ArrowDown', size)).toEqual({ row: 2, col: 3 });
  });

  it('jumps with Home, End, PageUp and PageDown', () => {
    expect(moveInGrid({ row: 1, col: 5 }, 'Home', size)).toEqual({ row: 1, col: 0 });
    expect(moveInGrid({ row: 1, col: 5 }, 'End', size)).toEqual({ row: 1, col: 11 });
    expect(moveInGrid({ row: 1, col: 5 }, 'PageUp', size)).toEqual({ row: 0, col: 5 });
    expect(moveInGrid({ row: 1, col: 5 }, 'PageDown', size)).toEqual({ row: 2, col: 5 });
  });

  it('ignores other keys', () => {
    expect(moveInGrid({ row: 0, col: 0 }, 'a', size)).toBeNull();
  });
});
