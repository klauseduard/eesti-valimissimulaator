// Lehe olek ja selle kodeerimine URL-i fragmenti, et jagatud link taastaks sama vaate.
//
// Näide: #v=norstat-2023-03-03&s=1&k=et&r=4&kaart=voitja&p.REF=30.5,23.7,...
//   v      eelseadistus (küsitluse tunnus või rk2023)
//   s      juhuslikkuse seeme
//   k      keel (et või en)
//   h      1: kasuta 2023. aasta tegelikke hääli (ainult eelseadistusel rk2023 ja muutmata toetusega)
//   e      0: ära arvesta korduvates simulatsioonides küsitluse viga (vaikimisi arvestatakse)
//   j      lihtne: jaga erakonna hääled kandidaatide vahel lihtsa juhusliku jaotusega (vaikimisi kalibreeritud)
//   r      selgituses valitud ringkond (1–12)
//   samm   samm-sammult vaade on avatud selle sammuga (0, 1, …; liiga suur arv piiratakse vaates)
//   sammud samm-sammult vaate sammu suurus: etapp, ringkond või mandaat (vaikimisi mandaat)
//   kaart  kaardi vaade: voitja või erakonna lühend (erakonna toetus)
//   p.X    erakonna X muudetud toetus 12 ringkonnas, komaga eraldatud

export type Keel = 'et' | 'en';

/** häälte jaotus kandidaatide vahel (vt docs/kandidaatide-mudel.md); nimed on samad mis Kotlini koodis */
export type Jaotus = 'kalibreeritud' | 'lihtne';

export type Sammusuurus = 'etapp' | 'ringkond' | 'mandaat';
const SAMMUSUURUSED: Sammusuurus[] = ['etapp', 'ringkond', 'mandaat'];

export const KAART_VÕITJA = 'voitja';
export const RK2023 = 'rk2023';

export interface Olek {
  eelseadistus: string;
  /** erakonna lühend -> muudetud toetusprotsendid 12 ringkonnas */
  muudatused: Record<string, number[]>;
  seeme: number;
  keel: Keel;
  tegelikudHääled: boolean;
  /** kas korduvad simulatsioonid kõigutavad toetust küsitluse vea võrra */
  viga: boolean;
  jaotus: Jaotus;
  ringkond: number;
  kaart: string;
  /** samm-sammult vaate samm; null, kui vaade on suletud */
  samm: number | null;
  sammuSuurus: Sammusuurus;
}

export interface Teadaolevad {
  eelseadistused: string[];
  lühendid: string[];
  vaikimisiEelseadistus: string;
}

export function vaikimisiOlek(teadaolevad: Teadaolevad, keel: Keel = 'et'): Olek {
  return {
    eelseadistus: teadaolevad.vaikimisiEelseadistus,
    muudatused: {},
    seeme: 1,
    keel,
    tegelikudHääled: false,
    viga: true,
    jaotus: 'kalibreeritud',
    ringkond: 1,
    kaart: KAART_VÕITJA,
    samm: null,
    sammuSuurus: 'mandaat',
  };
}

/** toetusprotsendid ümardatakse kahe kümnendkohani, et link taastaks täpselt sama oleku */
export function ümarda(protsent: number): number {
  return Math.round(protsent * 100) / 100;
}

export function kodeeri(olek: Olek): string {
  const osad: string[] = [
    `v=${encodeURIComponent(olek.eelseadistus)}`,
    `s=${olek.seeme}`,
    `k=${olek.keel}`,
  ];
  if (olek.tegelikudHääled) osad.push('h=1');
  if (!olek.viga) osad.push('e=0');
  if (olek.jaotus !== 'kalibreeritud') osad.push(`j=${olek.jaotus}`);
  osad.push(`r=${olek.ringkond}`);
  osad.push(`kaart=${encodeURIComponent(olek.kaart)}`);
  if (olek.samm !== null) {
    osad.push(`samm=${olek.samm}`);
    if (olek.sammuSuurus !== 'mandaat') osad.push(`sammud=${olek.sammuSuurus}`);
  }
  for (const [lühend, protsendid] of Object.entries(olek.muudatused)) {
    osad.push(`p.${encodeURIComponent(lühend)}=${protsendid.map((p) => ümarda(p)).join(',')}`);
  }
  return '#' + osad.join('&');
}

export interface Dekodeeritud {
  olek: Olek;
  /** lingi osad, mida ei saanud kasutada (veateate jaoks) */
  vead: string[];
}

export function dekodeeri(fragment: string, teadaolevad: Teadaolevad, vaikimisiKeel: Keel = 'et'): Dekodeeritud {
  const olek = vaikimisiOlek(teadaolevad, vaikimisiKeel);
  const vead: string[] = [];
  const tekst = fragment.startsWith('#') ? fragment.slice(1) : fragment;
  if (tekst.trim() === '') return { olek, vead };

  for (const osa of tekst.split('&')) {
    if (osa === '') continue;
    const i = osa.indexOf('=');
    const võti = i < 0 ? osa : osa.slice(0, i);
    let väärtus: string;
    try {
      väärtus = decodeURIComponent(i < 0 ? '' : osa.slice(i + 1));
    } catch {
      vead.push(osa);
      continue;
    }

    if (võti === 'v') {
      if (teadaolevad.eelseadistused.includes(väärtus)) olek.eelseadistus = väärtus;
      else vead.push(osa);
    } else if (võti === 's') {
      const seeme = Number(väärtus);
      if (Number.isInteger(seeme) && Math.abs(seeme) <= 2 ** 31 - 1) olek.seeme = seeme;
      else vead.push(osa);
    } else if (võti === 'k') {
      if (väärtus === 'et' || väärtus === 'en') olek.keel = väärtus;
      else vead.push(osa);
    } else if (võti === 'h') {
      olek.tegelikudHääled = väärtus === '1';
    } else if (võti === 'e') {
      if (väärtus === '0' || väärtus === '1') olek.viga = väärtus === '1';
      else vead.push(osa);
    } else if (võti === 'j') {
      if (väärtus === 'kalibreeritud' || väärtus === 'lihtne') olek.jaotus = väärtus;
      else vead.push(osa);
    } else if (võti === 'samm') {
      const samm = Number(väärtus);
      if (väärtus !== '' && Number.isInteger(samm) && samm >= 0 && samm < 10_000) olek.samm = samm;
      else vead.push(osa);
    } else if (võti === 'sammud') {
      if ((SAMMUSUURUSED as string[]).includes(väärtus)) olek.sammuSuurus = väärtus as Sammusuurus;
      else vead.push(osa);
    } else if (võti === 'r') {
      const ringkond = Number(väärtus);
      if (Number.isInteger(ringkond) && ringkond >= 1 && ringkond <= 12) olek.ringkond = ringkond;
      else vead.push(osa);
    } else if (võti === 'kaart') {
      if (väärtus === KAART_VÕITJA || teadaolevad.lühendid.includes(väärtus)) olek.kaart = väärtus;
      else vead.push(osa);
    } else if (võti.startsWith('p.')) {
      const lühend = decodeURIComponent(võti.slice(2));
      const protsendid = väärtus.split(',').map(Number);
      const korras = teadaolevad.lühendid.includes(lühend) && protsendid.length === 12 &&
        protsendid.every((p) => Number.isFinite(p) && p >= 0 && p <= 100);
      if (korras) olek.muudatused[lühend] = protsendid.map(ümarda);
      else vead.push(osa);
    } else {
      vead.push(osa);
    }
  }

  // tegelikud hääled kehtivad ainult muutmata 2023. aasta tulemusel
  if (olek.tegelikudHääled && (olek.eelseadistus !== RK2023 || Object.keys(olek.muudatused).length > 0)) {
    olek.tegelikudHääled = false;
  }
  return { olek, vead };
}
