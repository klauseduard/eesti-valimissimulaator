// Kõige lähemal olevad kohad näidiskorduses: mitu häält puudus järgmisest ringkonnamandaadist või viimasest
// kompensatsioonimandaadist (RKVS § 62 lg 3 ja 5).

/**
 * Hääled, mis puuduvad erakonnal ringkonnas veel ühe ringkonnamandaadi saamiseks. Erakond saab iga täis
 * lihtkvoodi eest ühe koha ja ühe lisaks, kui jääk on vähemalt 0,75 lihtkvoodist; järgmine koht tuleb seega
 * häältega (täiskvoodid + 0,75) · kvoot või, kui jääk juba annab lisakoha, (täiskvoodid + 1,75) · kvoot.
 */
export function häältPuudu(hääli: number, lihtkvoot: number): number {
  const kvoote = hääli / lihtkvoot;
  const täis = Math.floor(kvoote);
  const järgmine = kvoote - täis >= 0.75 ? täis + 1.75 : täis + 0.75;
  return Math.ceil(järgmine * lihtkvoot - hääli);
}

/** modifitseeritud d'Hondti jagaja */
const jagaja = (mandaate: number) => (mandaate + 1) ** 0.9;

export interface KompensatsiooniPiir {
  võitja: string;
  järgmine: string;
  /** hääled, mis puudusid järgmisel erakonnal viimase kompensatsioonimandaadi saamiseks */
  puudu: number;
}

/**
 * Viimase kompensatsioonimandaadi piir: järgmise erakonna (teine suurim võrdlusarv viimases sammus) häälte arv
 * on tema võrdlusarv korda jagaja; mandaadi saamiseks oleks see pidanud olema võitja võrdlusarv korda sama jagaja.
 */
export function kompensatsiooniPiir(samm: {
  erakonnad: string[];
  võrdlusarvud: number[];
  jubaMandaate: number[];
  erakond: string;
}): KompensatsiooniPiir | null {
  const võitja = samm.erakonnad.indexOf(samm.erakond);
  const teised = samm.erakonnad.map((_, i) => i).filter((i) => i !== võitja);
  if (võitja < 0 || teised.length === 0) return null;
  const järgmine = teised.reduce((a, b) => (samm.võrdlusarvud[b] > samm.võrdlusarvud[a] ? b : a));
  const j = jagaja(samm.jubaMandaate[järgmine]);
  const puudu = Math.ceil((samm.võrdlusarvud[võitja] - samm.võrdlusarvud[järgmine]) * j);
  return { võitja: samm.erakond, järgmine: samm.erakonnad[järgmine], puudu };
}
