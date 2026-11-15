export interface GridPosition {
  row: number;
  col: number;
}

export interface GridSize {
  rows: number;
  cols: number;
}

/** Spostamento con le frecce / Home / End / PagUp / PagGiu sulla griglia; resta nei limiti. */
export function moveInGrid(pos: GridPosition, key: string, size: GridSize): GridPosition | null {
  const clampRow = (r: number) => Math.min(size.rows - 1, Math.max(0, r));
  const clampCol = (c: number) => Math.min(size.cols - 1, Math.max(0, c));
  switch (key) {
    case 'ArrowRight':
      return { row: pos.row, col: clampCol(pos.col + 1) };
    case 'ArrowLeft':
      return { row: pos.row, col: clampCol(pos.col - 1) };
    case 'ArrowDown':
      return { row: clampRow(pos.row + 1), col: pos.col };
    case 'ArrowUp':
      return { row: clampRow(pos.row - 1), col: pos.col };
    case 'Home':
      return { row: pos.row, col: 0 };
    case 'End':
      return { row: pos.row, col: size.cols - 1 };
    case 'PageUp':
      return { row: 0, col: pos.col };
    case 'PageDown':
      return { row: size.rows - 1, col: pos.col };
    default:
      return null;
  }
}
