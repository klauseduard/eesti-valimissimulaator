// Kasutajaliidese tekstid eesti ja inglise keeles. Erakondade, kandidaatide ja ringkondade nimed on
// pärisnimed ja jäävad eestikeelseks.
import et from '../keeled/et.json';
import en from '../keeled/en.json';
import type { Keel } from './olek';

export const SÕNASTIKUD: Record<Keel, Record<string, string>> = { et, en };

let aktiivne = $state<Keel>('et');

export const i18n = {
  get keel(): Keel {
    return aktiivne;
  },
  set keel(keel: Keel) {
    aktiivne = keel;
    if (typeof document !== 'undefined') document.documentElement.lang = keel;
  },
};

/** Tõlgib võtme; {nimi} asendatakse parameetriga. Puuduva võtme korral tagastatakse võti ise. */
export function t(võti: string, parameetrid?: Record<string, string | number>): string {
  const tekst = SÕNASTIKUD[aktiivne][võti] ?? võti;
  if (!parameetrid) return tekst;
  // parameetrite nimedes on täpitähti, seega Unicode'i tähtede klass, mitte \w
  return tekst.replace(/\{([\p{L}\p{N}_]+)\}/gu, (koht, nimi: string) => (nimi in parameetrid ? String(parameetrid[nimi]) : koht));
}

/** brauseri eelistatud keel esimesel külastusel */
export function brauseriKeel(): Keel {
  if (typeof navigator === 'undefined') return 'et';
  return navigator.languages?.some((k) => k.toLowerCase().startsWith('et')) || navigator.language?.startsWith('et')
    ? 'et'
    : 'en';
}

export function arv(väärtus: number, kohti = 0): string {
  return new Intl.NumberFormat(aktiivne === 'et' ? 'et-EE' : 'en-GB', {
    minimumFractionDigits: kohti,
    maximumFractionDigits: kohti,
  }).format(väärtus);
}

export function protsent(väärtus: number, kohti = 1): string {
  // eesti keeles käib protsendimärgi ette tühik, inglise keeles mitte
  return arv(väärtus, kohti) + (aktiivne === 'et' ? '\u00a0%' : '%');
}
