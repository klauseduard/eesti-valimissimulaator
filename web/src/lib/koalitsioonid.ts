// Koalitsioonide enamuse tõenäosused korduvatest simulatsioonidest. Arvestatakse ainult kohtade arvu, mitte
// erakondade poliitilist sobivust.

/** enamuseks vajalik kohtade arv */
export const ENAMUS = 51;

/** liige on üleliigne, kui koalitsioon ilma temata on enamuses vähemalt selles osas kordustest */
const ÜLELIIGNE_ALATES = 0.5;

/** näidatakse koalitsioone, mille enamuse tõenäosus on vähemalt see */
const VÄIKSEIM_TÕENÄOSUS = 0.01;

export interface Enamus {
  tõenäosus: number;
  keskmine: number;
}

/**
 * Koalitsiooni enamuse tõenäosus ja keskmine kohtade arv.
 *
 * @param mandaadid - mandaadid[i][k] on erakonna i kohtade arv korduses k
 * @param liikmed - koalitsiooni erakondade indeksid
 */
export function enamus(mandaadid: number[][], liikmed: number[]): Enamus {
  const kordi = mandaadid[0]?.length ?? 0;
  if (kordi === 0) return { tõenäosus: 0, keskmine: 0 };
  let enamuses = 0;
  let summa = 0;
  for (let k = 0; k < kordi; k++) {
    let kohti = 0;
    for (const i of liikmed) kohti += mandaadid[i][k];
    if (kohti >= ENAMUS) enamuses += 1;
    summa += kohti;
  }
  return { tõenäosus: enamuses / kordi, keskmine: summa / kordi };
}

export interface Koalitsioon extends Enamus {
  erakonnad: string[];
}

/**
 * Minimaalsed koalitsioonid kuni [suurim] erakonnaga: ükski liige pole üleliigne, st koalitsioon ilma ühegi
 * liikmeta pole enamuses vähemalt pooltes kordustes. Tõenäosuse järgi kahanevalt (võrdsuse korral väiksemad ja suurema keskmisega eespool). Arvesse
 * lähevad erakonnad, kes saavad keskmiselt vähemalt pool kohta. Erakondade arv on väike (kuni 9), seega on
 * kõigi alamhulkade läbivaatus odav: O(2^n · n · kordi).
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
