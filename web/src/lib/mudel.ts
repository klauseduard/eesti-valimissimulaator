// Mudelite arvud tekstide jaoks (kandidaatide jaotus, küsitluse viga), võetuna Kotlini programmi loodud
// failidest, et tekstid ei jääks mudelist maha.
import kandidaadimudel from '@andmed/kandidaadimudel.json';
import koondmudel from '@andmed/kusitlused/koondmudel.json';
// otse küsitluste failist, mitte andmed.ts kaudu, et metoodika leht ei laadiks kandidaatide faile
import KÜSITLUSED from '@veebiandmed/toetused.json';

export const KANDIDAADIMUDEL = kandidaadimudel;
export const KOONDMUDEL = koondmudel;

/** erakonna üleriigilise nimekirja esimesed nii mitu kandidaati saavad kaalu lisa (ESINUMBRITE_ARV Kotlinis) */
const ESINUMBRITE_ARV = 5;

/**
 * Kalibreeritud jaotuse oodatavad osakaalud (Positsioonimudel.ootuspärasedOsakaalud Kotlinis): positsiooni p
 * kaal on exp(−b·(p − 1)) + c, üleriigilise esiviisiku kandidaadil korrutatuna (1 + d).
 *
 * @param esiviisikus - ringkonnanimekirja positsioonid (alates 1), mille kandidaat on üleriigilises esiviisikus
 */
export function kandidaadiOsakaalud(pikkus: number, esiviisikus: number[]): number[] {
  const { b, c, d } = kandidaadimudel;
  const kaalud = Array.from({ length: pikkus }, (_, i) => (Math.exp(-b * i) + c) * (esiviisikus.includes(i + 1) ? 1 + d : 1));
  const summa = kaalud.reduce((x, y) => x + y, 0);
  return kaalud.map((k) => k / summa);
}

/** tüüpiline suhteline viga protsentides (üks standardhälve logaritmilisel skaalal) */
export const tüüpilineViga = (logViga: number) => Math.round((Math.exp(logViga) - 1) * 100);

const KÜMME = 10;

export const mudeliArvud = {
  /** kümneliikmelise nimekirja esimese kandidaadi oodatav osa, protsentides */
  esimene: Math.round(kandidaadiOsakaalud(KÜMME, [])[0] * 100),
  /** sama, kui ta on erakonna üleriigilise nimekirja esiviisikus */
  esimeneEsiviisikus: Math.round(kandidaadiOsakaalud(KÜMME, [1])[0] * 100),
  esiviisik: ESINUMBRITE_ARV,
  ...kandidaadimudel.testitulemus,
  /** eelseadistuste küsitluse viga protsentides */
  vigaKoond: tüüpilineViga(KÜSITLUSED['koond-2023-03-03'].viga[0]),
  vigaNorstat: tüüpilineViga(KÜSITLUSED['norstat-2023-03-03'].viga[0]),
  vigaEmor: tüüpilineViga(KÜSITLUSED['emor-2023-03'].viga[0]),
  vigaRingkond: tüüpilineViga(KÜSITLUSED['koond-2023-03-03'].viga[1]),
  /** küsitlejate ühine viga kolm päeva enne valimisi (failis on dispersioon) */
  ühineViga: tüüpilineViga(koondmudel.horisondid['3'].ühine ** 0.5),
};
