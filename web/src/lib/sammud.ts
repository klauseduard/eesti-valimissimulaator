// Mandaatide jagamine sammudena samm-sammult vaate jaoks. Sammud tulevad ühe jaotuse salvestatud andmetest
// (Kotlini arvutus); siin neid ei arvutata uuesti, vaid pannakse RKVS § 62 järjekorda: 5% künnis, iga ringkonna
// lihtkvoot, isikumandaadid ja ringkonnamandaadid, seejärel kompensatsioonimandaadid ükshaaval.
//
// Ringkonnas ei jagata kohti üksteise järel: erakonna kõik ringkonnamandaadid tulevad ühest arvutusest (mitu
// lihtkvooti hääled sisaldavad). Ühe koha sammud on seal ainult selgituse jaoks. Kompensatsioonimandaadid
// jagatakse tõesti järjest, sest iga sammu võrdlusarvud sõltuvad eelmistest.
import type { Tulemus } from './simulaator';

export type Kandidaat = Tulemus['valitud'][number];
export type Etapp = 'algus' | 'künnis' | 'ringkonnad' | 'kompensatsioon' | 'lõpp';
export type { Sammusuurus } from './olek';
import type { Sammusuurus } from './olek';
export type Mandaadiliik = 'isikumandaat' | 'ringkonnamandaat' | 'kompensatsioonimandaat';

export interface Samm {
  indeks: number;
  liik:
    | 'algus'
    | 'künnis'
    | 'kvoot'
    | 'isikumandaat'
    | 'ringkonnamandaat'
    | 'jagamata'
    | 'ringkond-lõpp'
    | 'kompensatsioon-algus'
    | 'kompensatsioonimandaat'
    | 'lõpp';
  etapp: Etapp;
  /** samm-sammult liikumise rühm suurusel „ringkond": algus, künnis, r1…r12, komp, lõpp */
  rühm: string;
  ringkond?: number;
  erakond?: string;
  /** kandidaat, kes selles sammus mandaadi saab (või jagamata koha puhul jääks alla 10% nõude) */
  kandidaat?: Kandidaat;
  /** jagatud mandaadi liik; puudub, kui samm mandaati ei jaga */
  mandaat?: Mandaadiliik;
  /** erakonna mitmes koht ringkonnas (ringkonnamandaadi ja jagamata koha sammud) */
  koht?: number;
  /** kas see koht tuleb 0,75 jäägist (RKVS § 62 lg 3) */
  jäägist?: boolean;
  /** sammu indeks massiivis tulemus.kompensatsioon */
  kompensatsioon?: number;
  /** erakonna nimi -> jagatud mandaatide arv pärast seda sammu */
  jaotatud: Record<string, number>;
  /** jagatud mandaate kokku pärast seda sammu */
  kokku: number;
}

/** Jaotuse sammud RKVS § 62 järjekorras; viimase sammu seis on lõpptulemus. */
export function jaotuseSammud(tulemus: Tulemus): Samm[] {
  const sammud: Samm[] = [];
  const jaotatud: Record<string, number> = {};
  let kokku = 0;

  function lisa(samm: Omit<Samm, 'indeks' | 'jaotatud' | 'kokku'>) {
    if (samm.mandaat && samm.erakond) {
      jaotatud[samm.erakond] = (jaotatud[samm.erakond] ?? 0) + 1;
      kokku += 1;
    }
    sammud.push({ ...samm, indeks: sammud.length, jaotatud: { ...jaotatud }, kokku });
  }

  lisa({ liik: 'algus', etapp: 'algus', rühm: 'algus' });
  lisa({ liik: 'künnis', etapp: 'künnis', rühm: 'künnis' });

  for (const r of tulemus.ringkonnad) {
    const rühm = `r${r.ringkond}`;
    const ringkonnast = { etapp: 'ringkonnad' as const, rühm, ringkond: r.ringkond };
    lisa({ liik: 'kvoot', ...ringkonnast });

    // isikumandaadid: kõik kandidaadid, kelle hääled on vähemalt lihtkvoot (ka künnise alla jäänud nimekirjad)
    const isikumandaadiga = new Set<number>();
    for (const k of [...r.isikumandaadid].sort((a, b) => b.hääli - a.hääli)) {
      isikumandaadiga.add(k.regNr);
      lisa({ liik: 'isikumandaat', ...ringkonnast, erakond: k.erakond, kandidaat: k, mandaat: 'isikumandaat' });
    }

    // künnise ületanud erakondade kohad; isikumandaadiga kandidaadid on juba arvestatud
    for (const e of r.erakonnad) {
      for (let koht = 1; koht <= e.mandaate; koht++) {
        const jäägist = e.lisamandaat && koht === e.mandaate;
        const kandidaat = e.valitud[koht - 1];
        if (kandidaat) {
          if (isikumandaadiga.has(kandidaat.regNr)) continue;
          lisa({
            liik: 'ringkonnamandaat', ...ringkonnast, erakond: e.erakond, kandidaat, mandaat: 'ringkonnamandaat', koht, jäägist,
          });
        } else {
          lisa({
            liik: 'jagamata', ...ringkonnast, erakond: e.erakond, koht, jäägist,
            kandidaat: e.alla10Protsendi[koht - e.valitud.length - 1],
          });
        }
      }
    }
    lisa({ liik: 'ringkond-lõpp', ...ringkonnast });
  }

  lisa({ liik: 'kompensatsioon-algus', etapp: 'kompensatsioon', rühm: 'komp' });
  tulemus.kompensatsioon.forEach((k, i) => {
    lisa({
      liik: 'kompensatsioonimandaat', etapp: 'kompensatsioon', rühm: 'komp',
      erakond: k.erakond, kandidaat: k.kandidaat, mandaat: 'kompensatsioonimandaat', kompensatsioon: i,
    });
  });
  lisa({ liik: 'lõpp', etapp: 'lõpp', rühm: 'lõpp' });
  return sammud;
}

function võti(samm: Samm, suurus: Sammusuurus): string | number {
  return suurus === 'mandaat' ? samm.indeks : suurus === 'ringkond' ? samm.rühm : samm.etapp;
}

/** peatus on rühma viimane samm: seal on rühma kõik mandaadid jagatud */
function onPeatus(sammud: Samm[], i: number, suurus: Sammusuurus): boolean {
  return i === sammud.length - 1 || võti(sammud[i], suurus) !== võti(sammud[i + 1], suurus);
}

/** peatus, kus rühm, kuhu samm kuulub, on lõpuni jagatud: samm ise või järgmine peatus */
export function rühmaLõpp(sammud: Samm[], i: number, suurus: Sammusuurus): number {
  return onPeatus(sammud, i, suurus) ? i : järgmine(sammud, i, suurus);
}

/** järgmine peatus antud sammu suurusega; lõpus jääb samaks */
export function järgmine(sammud: Samm[], i: number, suurus: Sammusuurus): number {
  for (let j = i + 1; j < sammud.length; j++) if (onPeatus(sammud, j, suurus)) return j;
  return i;
}

/** eelmine peatus antud sammu suurusega; alguses jääb samaks */
export function eelmine(sammud: Samm[], i: number, suurus: Sammusuurus): number {
  for (let j = i - 1; j >= 0; j--) if (onPeatus(sammud, j, suurus)) return j;
  return i;
}
