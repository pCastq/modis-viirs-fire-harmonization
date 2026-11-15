import { createFormatters, capitalize, EM_DASH } from './format';

const fmtEn = createFormatters('en');
const fmtIt = createFormatters('it');

describe('formatters', () => {
  it('writes counts with the separator of the language, also for four digits', () => {
    expect(fmtEn.count(7374)).toBe('7,374');
    expect(fmtIt.count(7374)).toBe('7.374');
    expect(fmtEn.count(1234567)).toBe('1,234,567');
    expect(fmtIt.count(12.6)).toBe('13');
  });

  it('shows "no data" as a dash, never as zero', () => {
    expect(fmtEn.count(null)).toBe(EM_DASH);
    expect(fmtIt.compact(undefined)).toBe(EM_DASH);
    expect(fmtEn.count(0)).toBe('0');
  });

  it.each([
    [0, '0', '0'],
    [999, '999', '999'],
    [1000, '1K', '1K'],
    [1500, '1.5K', '1,5K'],
    [61234, '61.2K', '61,2K'],
    [999_949, '999.9K', '999,9K'],
    [999_950, '1M', '1M'],
    [1_200_000, '1.2M', '1,2M'],
  ])('compacts %d', (value, english, italian) => {
    expect(fmtEn.compact(value)).toBe(english);
    expect(fmtIt.compact(value)).toBe(italian);
  });

  it('formats percentages and signed differences', () => {
    expect(fmtEn.percent(0.35)).toBe('35%');
    expect(fmtEn.signedPercent(1.8)).toBe('+80%');
    expect(fmtIt.signedPercent(0.6)).toBe('−40%');
    expect(fmtEn.signedPercent(1)).toBe('0%');
  });

  it('names months in the language', () => {
    expect(fmtEn.monthName(8)).toBe('August');
    expect(fmtIt.monthName(8)).toBe('agosto');
    expect(fmtEn.monthShort(1)).toBe('Jan');
    expect(fmtEn.monthKey('2019-08')).toBe('August 2019');
    expect(fmtIt.monthKey('2019-08')).toBe('agosto 2019');
    expect(fmtEn.monthKey('bad')).toBe('bad');
  });

  it('capitalizes the first letter only', () => {
    expect(capitalize('agosto')).toBe('Agosto');
    expect(capitalize('')).toBe('');
  });
});
