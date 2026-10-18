// Põhilõime ja taustalõime (tööline.ts) vahelised sõnumid.
import type { KokkuvõteJs } from '@simulaator';
import type { Tavaline } from './tavaline';

export interface ToetuseAndmed {
  erakonnad: string[];
  protsendid: number[][];
}

export interface AndmeTekstid {
  ringkondadeTekstid: string[];
  üleriigilisedNimekirjad: string;
  rk2023: string;
}

export type Kokkuvõte = Tavaline<KokkuvõteJs>;

export type SõnumTöölisele =
  | { tüüp: 'andmed'; andmed: AndmeTekstid }
  | { tüüp: 'alusta'; id: number; toetus: ToetuseAndmed; seeme: number; viga: [number, number]; kordi: number; partii: number }
  | { tüüp: 'peata'; id: number };

export type SõnumTöölselt =
  | { tüüp: 'vahetulemus'; id: number; kokkuvõte: Kokkuvõte; valmis: boolean }
  | { tüüp: 'viga'; id: number; teade: string };

/** Jagab korduste arvu partiideks; viimane partii võib olla väiksem. */
export function partiid(kordi: number, suurus: number): number[] {
  if (!(kordi > 0) || !(suurus > 0)) throw new Error('Korduste arv ja partii suurus peavad olema positiivsed');
  const tulemus: number[] = [];
  for (let jääk = kordi; jääk > 0; jääk -= suurus) tulemus.push(Math.min(suurus, jääk));
  return tulemus;
}
