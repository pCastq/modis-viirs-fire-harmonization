import { DEFAULT_LOCALE, LOCALES, detectLocale, saveLocale, isLocale } from './locales';
import { en } from './messages/en';
import { it as italian } from './messages/it';
import { createTranslator } from './translate';

/** Elenca "a.b.c" -> testo (le forme plurali contano come un solo messaggio). */
function flatten(node: unknown, prefix = ''): Map<string, string> {
  const out = new Map<string, string>();
  for (const [key, value] of Object.entries(node as Record<string, unknown>)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (typeof value === 'string') out.set(path, value);
    else if ('one' in (value as object) && 'other' in (value as object)) {
      out.set(path, `${(value as { one: string }).one}|${(value as { other: string }).other}`);
    } else for (const [k, v] of flatten(value, path)) out.set(k, v);
  }
  return out;
}

const placeholders = (text: string) => [...text.matchAll(/\{(\w+)\}/g)].map((m) => m[1]).sort();

describe('catalogues', () => {
  const english = flatten(en);
  const italianMessages = flatten(italian);

  it('defaults to English', () => {
    expect(DEFAULT_LOCALE).toBe('en');
    expect(LOCALES).toContain('it');
  });

  it('have the same keys in every language', () => {
    expect([...italianMessages.keys()].sort()).toEqual([...english.keys()].sort());
  });

  it('use the same placeholders for each message, so no value is lost in translation', () => {
    for (const [key, text] of english) {
      expect(placeholders(italianMessages.get(key)!), key).toEqual(placeholders(text));
    }
  });

  it('have no empty message', () => {
    for (const [key, text] of [...english, ...italianMessages]) {
      expect(text.trim(), key).not.toBe('');
    }
  });
});

describe('createTranslator', () => {
  it('replaces placeholders and leaves unknown ones visible', () => {
    const t = createTranslator('en');
    expect(t('header.dataRange', { from: 'A', to: 'B' })).toBe('Data: A – B');
    expect(t('header.dataRange', { from: 'A' })).toBe('Data: A – {to}');
  });

  it('chooses the plural form with the rules of the language', () => {
    const t = createTranslator('en');
    expect(t('insights.anomaly.reference', { count: 1 })).toBe('Reference: 1 year');
    expect(t('insights.anomaly.reference', { count: 20 })).toBe('Reference: 20 years');
    expect(createTranslator('it')('insights.anomaly.reference', { count: 1 })).toBe(
      'Riferimento: 1 anno',
    );
  });

  it('shows the key when a message does not exist', () => {
    // @ts-expect-error chiave volutamente inesistente
    expect(createTranslator('en')('no.such.key')).toBe('no.such.key');
  });
});

describe('detectLocale', () => {
  const storage = (value: string | null) => ({
    getItem: () => value,
    setItem: () => undefined,
  });

  it('prefers the URL, then the saved choice, then English', () => {
    expect(detectLocale('?lang=it', storage('en'))).toBe('it');
    expect(detectLocale('', storage('it'))).toBe('it');
    expect(detectLocale('', storage(null))).toBe('en');
    expect(detectLocale('', null)).toBe('en');
  });

  it('ignores unsupported languages', () => {
    expect(detectLocale('?lang=fr', storage('de'))).toBe('en');
    expect(isLocale('fr')).toBe(false);
  });

  it('survives a storage that throws (private mode)', () => {
    const broken = {
      getItem: () => {
        throw new Error('blocked');
      },
      setItem: () => {
        throw new Error('blocked');
      },
    };
    expect(detectLocale('', broken)).toBe('en');
    expect(() => saveLocale('it', broken)).not.toThrow();
  });
});
