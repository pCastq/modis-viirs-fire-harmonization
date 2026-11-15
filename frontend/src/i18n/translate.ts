import type { Locale } from './locales';
import { INTL_TAG } from './locales';
import { en } from './messages/en';
import { it } from './messages/it';
import type { Messages, MessageKey, Params } from './messages/types';

const CATALOGS: Record<Locale, Messages> = { en, it };

function lookup(messages: Messages, key: string): unknown {
  return key.split('.').reduce<unknown>((node, part) => {
    return node !== null && typeof node === 'object'
      ? (node as Record<string, unknown>)[part]
      : undefined;
  }, messages);
}

function interpolate(template: string, params: Params): string {
  return template.replace(/\{(\w+)\}/g, (whole, name: string) =>
    name in params ? String(params[name]) : whole,
  );
}

export type Translator = (key: MessageKey, params?: Params) => string;

/**
 * Traduttore per una lingua. Un messaggio e' un testo con segnaposto `{nome}`, oppure un oggetto
 * `{ one, other }` scelto dal parametro `count` secondo le regole dei plurali della lingua.
 * Se manca nella lingua scelta si ricade sull'inglese; se manca ovunque si mostra la chiave.
 */
export function createTranslator(locale: Locale): Translator {
  const plurals = new Intl.PluralRules(INTL_TAG[locale]);
  return (key, params = {}) => {
    let message = lookup(CATALOGS[locale], key) ?? lookup(en, key);
    if (message !== null && typeof message === 'object') {
      const forms = message as Record<string, string>;
      const category = plurals.select(Number(params.count ?? 0));
      message = forms[category] ?? forms.other;
    }
    return typeof message === 'string' ? interpolate(message, params) : key;
  };
}
