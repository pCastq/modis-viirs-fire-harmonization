import type { EnglishMessages } from './en';

/** Un messaggio: un testo, oppure le forme singolare/plurale scelte da `count`. */
export type Message = string | { one: string; other: string };

export interface MessageTree {
  [key: string]: Message | MessageTree;
}

export type Params = Record<string, string | number>;

type Join<K extends string, P extends string> = `${K}.${P}`;

/** Tutti i percorsi "a.b.c" che portano a un messaggio. */
export type Paths<T> = {
  [K in keyof T & string]: T[K] extends Message
    ? K
    : T[K] extends object
      ? Join<K, Paths<T[K]>>
      : never;
}[keyof T & string];

/** Le lingue hanno tutte la stessa forma dell'inglese: una chiave mancante non compila. */
export type Messages = EnglishMessages;
export type MessageKey = Paths<Messages>;
