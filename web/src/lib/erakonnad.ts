// Erakondade lühendid ja värvid. Värvid järgivad erakondade tavapäraseid toone ning on kontrollitud
// värvipimeduse simulatsiooniga (vt docs/superpowers/specs/2026-10-18-veebiliides-design.md): kuue
// parlamendierakonna iga värvipaar on eristatav ka kaardil, kus kõik paarid on kõrvuti.
// Väiksemad nimekirjad esinevad ainult koos nimega (liugurid, tabelid), seega nende värvid on lisatunnus.

export interface ErakonnaInfo {
  lühend: string;
  värv: string;
}

export const ERAKONNAD: Record<string, ErakonnaInfo> = {
  'Eesti Reformierakond': { lühend: 'REF', värv: '#d39e2d' },
  'Eesti Konservatiivne Rahvaerakond': { lühend: 'EKRE', värv: '#4d47b2' },
  'Eesti Keskerakond': { lühend: 'KESK', värv: '#379a6f' },
  'Erakond Eesti 200': { lühend: 'E200', värv: '#20bef4' },
  'Sotsiaaldemokraatlik Erakond': { lühend: 'SDE', värv: '#c53328' },
  'ISAMAA Erakond': { lühend: 'ISAMAA', värv: '#4c82e8' },
  'Erakond Parempoolsed': { lühend: 'PP', värv: '#b25fa8' },
  'Erakond Eestimaa Rohelised': { lühend: 'ROH', värv: '#86a83a' },
  'EESTIMAA ÜHENDATUD VASAKPARTEI': { lühend: 'EÜVP', värv: '#9b4b2f' },
  'Üksikkandidaadid': { lühend: 'ÜK', värv: '#8c8c8c' },
};

/** erakondade püsiv järjekord diagrammidel, et erakonnad ei vahetaks kohta, kui tulemus muutub */
export const ERAKONDADE_JÄRJEKORD = Object.keys(ERAKONNAD);

export function erakonnaInfo(nimi: string): ErakonnaInfo {
  return ERAKONNAD[nimi] ?? { lühend: nimi.slice(0, 4).toUpperCase(), värv: '#8c8c8c' };
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
