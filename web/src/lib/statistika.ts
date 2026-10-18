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

/**
 * Jagab [kokku] kohta väärtuste vahel võrdeliselt suurima jäägi meetodil: igaüks saab oma osa täisosa,
 * ülejäänud kohad saavad suurima murdosaga väärtused (võrdsuse korral eespool olev). Diagrammil kasutatakse
 * seda keskmiste mandaatide näitamiseks täpselt 101 kohaga.
 */
export function jaotaSuurimaJäägiga(väärtused: number[], kokku: number): number[] {
  const summa = väärtused.reduce((a, b) => a + b, 0);
  if (summa <= 0) return väärtused.map(() => 0);
  const osad = väärtused.map((v) => (v * kokku) / summa);
  const tulemus = osad.map(Math.floor);
  const jääk = kokku - tulemus.reduce((a, b) => a + b, 0);
  const järjekord = osad.map((o, i) => ({ i, murd: o - Math.floor(o) })).sort((a, b) => b.murd - a.murd || a.i - b.i);
  for (let k = 0; k < jääk; k++) tulemus[järjekord[k].i] += 1;
  return tulemus;
}
