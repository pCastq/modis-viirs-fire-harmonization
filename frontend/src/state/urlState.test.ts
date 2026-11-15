import { DEFAULT_QUERY, NO_FOCUS } from '@/domain/query';
import { parseState, serializeState } from './urlState';

describe('urlState', () => {
  it('uses defaults for an empty URL and writes an empty URL for the defaults', () => {
    const state = parseState('');
    expect(state.query).toEqual(DEFAULT_QUERY);
    expect(state.focus).toEqual(NO_FOCUS);
    expect(serializeState(state)).toBe('');
  });

  it('round-trips a full state', () => {
    const search =
      '?area=Italy&from=2015-01&to=2019-12&mode=RAW&dn=N&conf=HIGH&src=ALL&month=2019-08&date=2019-08-14';
    const state = parseState(search);
    expect(state.query.area).toEqual({ kind: 'country', code: 'Italy' });
    expect(state.query.filters).toEqual({
      mode: 'RAW',
      dayNight: 'N',
      minConfidence: 'HIGH',
      sourceType: 'ALL',
    });
    expect(state.focus).toEqual({ month: '2019-08', date: '2019-08-14' });
    expect(new URLSearchParams(serializeState(state))).toEqual(new URLSearchParams(search));
  });

  it('round-trips a polygon', () => {
    const state = parseState('?poly=10,40;10.5,40;10.5,40.5;10,40.5');
    expect(state.query.area.kind).toBe('polygon');
    expect(serializeState(state)).toBe('?poly=10%2C40%3B10.5%2C40%3B10.5%2C40.5%3B10%2C40.5');
  });

  it.each([
    ['unknown filter values', '?mode=BOH&dn=Z&conf=X&src=Y'],
    ['malformed months', '?from=2019-13&to=abc&month=19-01'],
    ['an out-of-range polygon', '?poly=200,40;10,40;10,41'],
    ['a polygon with too few points', '?poly=10,40;11,41'],
    ['a hostile country code', '?area=../etc/passwd'],
  ])('falls back to defaults for %s', (_label, search) => {
    const state = parseState(search);
    expect(state.query).toEqual(DEFAULT_QUERY);
    expect(state.focus).toEqual(NO_FOCUS);
  });

  it('drops a focus day that does not belong to the focus month', () => {
    expect(parseState('?month=2019-08&date=2019-09-01').focus).toEqual({
      month: '2019-08',
      date: null,
    });
    expect(parseState('?date=2019-09-01').focus).toEqual(NO_FOCUS);
  });
});
