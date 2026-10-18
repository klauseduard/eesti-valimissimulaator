// Kotlini eksporditud klasside väljad on objekti omaduslikud väljad, arvutatud väljad (nt
// ErakonnaTulemusJs.mandaate) aga prototüübi getterid. postMessage ja JSON getterit ei kopeeri, seega
// teisendatakse tulemused enne saatmist või hoidmist tavalisteks objektideks.

type Funktsioon = (...args: never[]) => unknown;

/** Klassi andmeväljad tavalise objektina (meetodid jäetakse välja). */
export type Tavaline<T> = T extends readonly (infer E)[]
  ? Tavaline<E>[]
  : T extends object
    ? { [K in keyof T as T[K] extends Funktsioon ? never : K]: Tavaline<T[K]> }
    : T;

export function tavaliseks<T>(väärtus: T): Tavaline<T> {
  if (väärtus === null || väärtus === undefined || typeof väärtus !== 'object') return väärtus as Tavaline<T>;
  if (Array.isArray(väärtus)) return väärtus.map((v) => tavaliseks(v)) as Tavaline<T>;
  const tulemus: Record<string, unknown> = {};
  for (const [nimi, v] of Object.entries(väärtus)) tulemus[nimi] = tavaliseks(v);
  let proto = Object.getPrototypeOf(väärtus);
  while (proto && proto !== Object.prototype) {
    for (const [nimi, kirjeldus] of Object.entries(Object.getOwnPropertyDescriptors(proto))) {
      if (kirjeldus.get && !(nimi in tulemus)) {
        tulemus[nimi] = tavaliseks((väärtus as Record<string, unknown>)[nimi]);
      }
    }
    proto = Object.getPrototypeOf(proto);
  }
  return tulemus as Tavaline<T>;
}
