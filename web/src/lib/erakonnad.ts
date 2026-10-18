// Erakondade lühendid ja värvid. Värvid järgivad erakondade tavapäraseid toone ning on kontrollitud
// dataviz-oskuse validaatoriga (validate_palette.js, kõik paarid): heledas teemas pindadel #ffffff ja #f4f1ea,
// tumedas pindadel #1c1b19 ja #131311. Tumeda teema värvid on samad toonid OKLCH heleduse vahemikku
// 0,48–0,67 nihutatuna; kolme sinise (EKRE, ISAMAA, E200) eristamiseks on EKRE nihutatud lillaks, E200
// türkiissinisele ja KESK tumedamaks roheliseks. Kuue parlamendierakonna iga paar on eristatav ka kaardil,
// kus kõik paarid on kõrvuti. Väiksemad nimekirjad esinevad ainult koos nimega (liugurid, tabelid), seega
// nende värvid on lisatunnus (EÜVP ja SDE on värvipimedale peaaegu ühesugused).
import { teema } from './teema.svelte';

export interface ErakonnaInfo {
  lühend: string;
  värv: string;
}

interface Erakonnavärvid {
  lühend: string;
  hele: string;
  tume: string;
}

export const ERAKONNAD: Record<string, Erakonnavärvid> = {
  'Eesti Reformierakond': { lühend: 'REF', hele: '#d39e2d', tume: '#be8902' },
  'Eesti Konservatiivne Rahvaerakond': { lühend: 'EKRE', hele: '#4d47b2', tume: '#86399b' },
  'Eesti Keskerakond': { lühend: 'KESK', hele: '#379a6f', tume: '#1e7c4d' },
  'Erakond Eesti 200': { lühend: 'E200', hele: '#20bef4', tume: '#00a7b9' },
  'Sotsiaaldemokraatlik Erakond': { lühend: 'SDE', hele: '#c53328', tume: '#c53328' },
  'ISAMAA Erakond': { lühend: 'ISAMAA', hele: '#4c82e8', tume: '#3c71d5' },
  'Erakond Parempoolsed': { lühend: 'PP', hele: '#b25fa8', tume: '#b25fa8' },
  'Erakond Eestimaa Rohelised': { lühend: 'ROH', hele: '#86a83a', tume: '#7fa031' },
  'EESTIMAA ÜHENDATUD VASAKPARTEI': { lühend: 'EÜVP', hele: '#9b4b2f', tume: '#9b4b2f' },
  'Üksikkandidaadid': { lühend: 'ÜK', hele: '#8c8c8c', tume: '#8c8c8c' },
};

/** erakondade püsiv järjekord diagrammidel, et erakonnad ei vahetaks kohta, kui tulemus muutub */
export const ERAKONDADE_JÄRJEKORD = Object.keys(ERAKONNAD);

/** erakonna lühend ja praeguse teema värv; teema muutudes uuenevad seda kasutavad komponendid ise */
export function erakonnaInfo(nimi: string): ErakonnaInfo {
  const e = ERAKONNAD[nimi];
  if (!e) return { lühend: nimi.slice(0, 4).toUpperCase(), värv: '#8c8c8c' };
  return { lühend: e.lühend, värv: teema.tume ? e.tume : e.hele };
}

export function järjekorraNr(nimi: string): number {
  const i = ERAKONDADE_JÄRJEKORD.indexOf(nimi);
  return i < 0 ? ERAKONDADE_JÄRJEKORD.length : i;
}

export function nimiLühendist(lühend: string): string | undefined {
  return Object.entries(ERAKONNAD).find(([, info]) => info.lühend === lühend)?.[0];
}

/** värvilise pinna peale sobiv tekstivärv: tume heledal ja valge tumedal pinnal (suhteline heledus WCAG järgi) */
export function kontrastTekst(värv: string): string {
  const [r, g, b] = [1, 3, 5].map((i) => parseInt(värv.slice(i, i + 2), 16) / 255);
  const lin = (c: number) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
  return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b) > 0.3 ? '#141413' : '#ffffff';
}
