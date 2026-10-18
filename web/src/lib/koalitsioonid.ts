// Koalitsioonide enamuse tõenäosused korduvatest simulatsioonidest. Arvestatakse ainult kohtade arvu, mitte
// erakondade poliitilist sobivust.

/** enamuseks vajalik kohtade arv */
export const ENAMUS = 51;

/** liige on üleliigne, kui koalitsioon ilma temata on enamuses vähemalt selles osas kordustest */
const ÜLELIIGNE_ALATES = 0.5;

/** näidatakse koalitsioone, mille enamuse tõenäosus on vähemalt see */
const VÄIKSEIM_TÕENÄOSUS = 0.01;

import { statistika, type Statistika } from './statistika';

export interface Enamus extends Statistika {
  tõenäosus: number;
}

/**
 * Koalitsiooni enamuse tõenäosus ja kohtade arvu jaotus (keskmine, mediaan, 90% vahemik).
 *
 * @param mandaadid - mandaadid[i][k] on erakonna i kohtade arv korduses k
 * @param liikmed - koalitsiooni erakondade indeksid
 */
export function enamus(mandaadid: number[][], liikmed: number[]): Enamus {
  const kordi = mandaadid[0]?.length ?? 0;
  if (kordi === 0) return { tõenäosus: 0, ...statistika([0]) };
  const kohad = Array.from({ length: kordi }, (_, k) => liikmed.reduce((s, i) => s + mandaadid[i][k], 0));
  return { tõenäosus: kohad.filter((k) => k >= ENAMUS).length / kordi, ...statistika(kohad) };
}

export interface Koalitsioon extends Enamus {
  erakonnad: string[];
}

/**
 * Minimaalsed koalitsioonid kuni [suurim] erakonnaga: ükski liige pole üleliigne, st koalitsioon ilma ühegi
 * liikmeta pole enamuses vähemalt pooltes kordustes. Tõenäosuse järgi kahanevalt (võrdsuse korral väiksemad ja suurema keskmisega eespool). Arvesse
 * lähevad erakonnad, kes saavad keskmiselt vähemalt pool kohta. Erakondade arv on väike (kuni 9), seega on
 * kõigi alamhulkade läbivaatus odav: O(2^n · kordi · (n + log kordi)), sest iga alamhulga kohad sorteeritakse.
 */
export function koalitsioonid(erakonnad: string[], mandaadid: number[][], suurim = 4): Koalitsioon[] {
  const kordi = mandaadid[0]?.length ?? 0;
  if (kordi === 0) return [];
  const osalised = erakonnad
    .map((_, i) => i)
    .filter((i) => mandaadid[i].reduce((a, b) => a + b, 0) / kordi >= 0.5);
  const vahemälu = new Map<number, Enamus>();
  const tõenäosus = (mask: number): Enamus => {
    const olemas = vahemälu.get(mask);
    if (olemas) return olemas;
    const e = enamus(mandaadid, osalised.filter((_, j) => mask & (1 << j)));
    vahemälu.set(mask, e);
    return e;
  };

  const tulemus: Koalitsioon[] = [];
  for (let mask = 1; mask < 1 << osalised.length; mask++) {
    const suurus = osalised.filter((_, j) => mask & (1 << j)).length;
    if (suurus > suurim) continue;
    const e = tõenäosus(mask);
    if (e.tõenäosus < VÄIKSEIM_TÕENÄOSUS) continue;
    const minimaalne = osalised.every((_, j) => !(mask & (1 << j)) || tõenäosus(mask & ~(1 << j)).tõenäosus < ÜLELIIGNE_ALATES);
    if (!minimaalne) continue;
    tulemus.push({ erakonnad: osalised.filter((_, j) => mask & (1 << j)).map((i) => erakonnad[i]), ...e });
  }
  return tulemus.sort(
    (a, b) => b.tõenäosus - a.tõenäosus || a.erakonnad.length - b.erakonnad.length || b.keskmine - a.keskmine,
  );
}
