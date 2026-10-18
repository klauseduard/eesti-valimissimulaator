// Kotlini simulaatori ümbris: eelseadistused, toetuse muutmine ja tulemuste teisendamine.
import { Simulaator, ToetusJs, type TulemusJs } from '@simulaator';
import { andmeTekstid, KÜSITLUSED } from './andmed';
import { erakonnaInfo, nimiLühendist } from './erakonnad';
import { RK2023, ümarda, type Olek } from './olek';
import type { ToetuseAndmed } from './protokoll';
import { tavaliseks, type Tavaline } from './tavaline';

export type Tulemus = Tavaline<TulemusJs>;

export const simulaator = new Simulaator(
  andmeTekstid.ringkondadeTekstid,
  andmeTekstid.üleriigilisedNimekirjad,
  andmeTekstid.rk2023,
);

/**
 * eelseadistuste tunnused valiku järjekorras: küsitluste koond, üksikküsitlused, 2023. aasta tulemus.
 * Vaikimisi on koond, sest see oli tagasitestis täpsem kui üksik küsitlus (docs/tagasitestimine.md).
 */
export const VAIKIMISI_EELSEADISTUS = 'koond-2023-03-03';
export const EELSEADISTUSED = [
  ...Object.keys(KÜSITLUSED).filter((k) => k.startsWith('koond-')),
  ...Object.keys(KÜSITLUSED).filter((k) => !k.startsWith('koond-')),
  RK2023,
];

export const RINGKONNAD = tavaliseks(simulaator.ringkonnad());

export function eelseadistuseToetus(tunnus: string): ToetuseAndmed {
  if (tunnus === RK2023) {
    const t = simulaator.rk2023Toetus();
    return { erakonnad: [...t.erakonnad], protsendid: t.protsendid.map((r) => [...r]) };
  }
  const küsitlus = KÜSITLUSED[tunnus];
  if (!küsitlus) throw new Error(`Tundmatu eelseadistus ${tunnus}`);
  return { erakonnad: [...küsitlus.erakonnad], protsendid: küsitlus.protsendid.map((r) => [...r]) };
}

/**
 * Eelseadistuse küsitluse viga [üleriigiline, ringkondlik]; 2023. aasta tegelikul tulemusel küsitluse viga pole.
 */
export function eelseadistuseViga(tunnus: string): [number, number] {
  if (tunnus === RK2023) return [0, 0];
  const küsitlus = KÜSITLUSED[tunnus];
  if (!küsitlus) throw new Error(`Tundmatu eelseadistus ${tunnus}`);
  return küsitlus.viga;
}

/** eelseadistuse toetus koos olekus olevate muudatustega */
export function olekuToetus(olek: Olek): ToetuseAndmed {
  const toetus = eelseadistuseToetus(olek.eelseadistus);
  for (const [lühend, protsendid] of Object.entries(olek.muudatused)) {
    const nimi = nimiLühendist(lühend);
    const i = nimi === undefined ? -1 : toetus.erakonnad.indexOf(nimi);
    if (i >= 0) toetus.protsendid[i] = [...protsendid];
  }
  return toetus;
}

function js(toetus: ToetuseAndmed): ToetusJs {
  return new ToetusJs(toetus.erakonnad, toetus.protsendid);
}

export function üleriigilineToetus(toetus: ToetuseAndmed, erakond: string): number {
  return simulaator.üleriigilineToetus(js(toetus), erakond);
}

export function kontrolliToetust(toetus: ToetuseAndmed): string[] {
  return [...simulaator.kontrolliToetust(js(toetus))];
}

/**
 * Muudab erakonna üleriigilist toetust (vt Kotlini muudaÜleriigilistToetust) ja tagastab muudatused,
 * mis tuleb olekusse lisada: kõigi erakondade ümardatud toetus.
 */
export function üleriigiliseToetuseMuudatused(toetus: ToetuseAndmed, erakond: string, uus: number): Record<string, number[]> {
  const muudetud = simulaator.muudaÜleriigilistToetust(js(toetus), erakond, uus);
  const muudatused: Record<string, number[]> = {};
  muudetud.erakonnad.forEach((nimi, i) => {
    muudatused[erakonnaInfo(nimi).lühend] = muudetud.protsendid[i].map(ümarda);
  });
  return muudatused;
}

export function jaota(olek: Olek, toetus: ToetuseAndmed): Tulemus {
  if (olek.tegelikudHääled) return tavaliseks(simulaator.jaotaTegelikeHäältega());
  return tavaliseks(simulaator.jaota(js(toetus), olek.seeme, olek.jaotus));
}
