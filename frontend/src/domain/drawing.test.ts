import { drawReducer, IDLE, type DrawState } from './drawing';

describe('drawReducer', () => {
  it('ignores clicks when not drawing', () => {
    expect(drawReducer(IDLE, { type: 'click', point: [1, 2] }).state).toEqual(IDLE);
  });

  it('completes a rectangle with two opposite corners, in any order', () => {
    let step = drawReducer(IDLE, { type: 'start', mode: 'rectangle' });
    step = drawReducer(step.state, { type: 'click', point: [11, 41] });
    expect(step.completed).toBeUndefined();
    step = drawReducer(step.state, { type: 'click', point: [10, 40] });
    expect(step.completed).toEqual([
      [10, 40],
      [11, 40],
      [11, 41],
      [10, 41],
    ]);
    expect(step.state).toEqual(IDLE);
  });

  it('does not accept a degenerate rectangle', () => {
    let step = drawReducer(IDLE, { type: 'start', mode: 'rectangle' });
    step = drawReducer(step.state, { type: 'click', point: [10, 40] });
    step = drawReducer(step.state, { type: 'click', point: [10, 41] });
    expect(step.completed).toBeUndefined();
    expect(step.state.points).toEqual([[10, 40]]);
  });

  it('needs three vertices to finish a polygon', () => {
    let step = drawReducer(IDLE, { type: 'start', mode: 'polygon' });
    step = drawReducer(step.state, { type: 'click', point: [0, 0] });
    step = drawReducer(step.state, { type: 'click', point: [1, 0] });
    expect(drawReducer(step.state, { type: 'finish' }).completed).toBeUndefined();
    step = drawReducer(step.state, { type: 'click', point: [1, 1] });
    expect(drawReducer(step.state, { type: 'finish' }).completed).toHaveLength(3);
  });

  it('cancel always goes back to idle', () => {
    const drawing: DrawState = { mode: 'polygon', points: [[0, 0]] };
    expect(drawReducer(drawing, { type: 'cancel' }).state).toEqual(IDLE);
  });
});
