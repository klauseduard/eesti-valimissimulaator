export interface Statistika {
  min: number;
  max: number;
  keskmine: number;
  /** 5. ja 95. protsentiil: 90% kordustest jääb nende vahele */
  p05: number;
  p95: number;
}

/** Protsentiil lähima järgu meetodil; väärtused peavad olema kasvavalt sorteeritud. */
function protsentiil(sorteeritud: number[], p: number): number {
  const i = Math.min(sorteeritud.length - 1, Math.max(0, Math.ceil(p * sorteeritud.length) - 1));
  return sorteeritud[i];
}

export function statistika(väärtused: number[]): Statistika {
  if (väärtused.length === 0) throw new Error('Tühja hulga statistikat ei saa arvutada');
  const s = [...väärtused].sort((a, b) => a - b);
  return {
    min: s[0],
    max: s[s.length - 1],
    keskmine: s.reduce((a, b) => a + b, 0) / s.length,
    p05: protsentiil(s, 0.05),
    p95: protsentiil(s, 0.95),
  };
}

/** Mitu korda iga väärtus esineb, väärtuse järgi kasvavalt. */
export function sagedused(väärtused: number[]): [number, number][] {
  const loendur = new Map<number, number>();
  for (const v of väärtused) loendur.set(v, (loendur.get(v) ?? 0) + 1);
  return [...loendur.entries()].sort((a, b) => a[0] - b[0]);
}

export interface SagedaseimTulemus {
  /** iga rea (nt erakonna) väärtus kõige sagedasemas korduses */
  väärtused: number[];
  /** mitmes korduses see tulemus esines */
  kordi: number;
  osakaal: number;
}

/**
 * Kõige sagedasem terviktulemus: kordus, mille kõigi ridade väärtuste kombinatsioon esines kõige
 * rohkem kordi. Erinevalt iga rea eraldi kõige sagedasemast väärtusest on summa alati sama mis ühes
 * korduses (nt 101 mandaati). Võrdse sageduse korral valitakse esimesena esinenud tulemus.
 *
 * @param read - read[i][k] on rea i väärtus korduses k
 */
export function sagedaseimTulemus(read: number[][]): SagedaseimTulemus {
  const kordi = read[0]?.length ?? 0;
  if (kordi === 0) throw new Error('Kordusteta ei saa sagedaseimat tulemust leida');
  const loendur = new Map<string, { k: number; kordi: number }>();
  let parim: { k: number; kordi: number } | null = null;
  for (let k = 0; k < kordi; k++) {
    const võti = read.map((rida) => rida[k]).join(',');
    const kirje = loendur.get(võti) ?? { k, kordi: 0 };
    kirje.kordi += 1;
    loendur.set(võti, kirje);
    if (parim === null || kirje.kordi > parim.kordi || (kirje.kordi === parim.kordi && kirje.k < parim.k)) parim = kirje;
  }
  return { väärtused: read.map((rida) => rida[parim!.k]), kordi: parim!.kordi, osakaal: parim!.kordi / kordi };
}
