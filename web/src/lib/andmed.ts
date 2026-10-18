// Programmi andmefailid (src/jvmMain/resources) ja küsitluste kompaktne fail (./gradlew veebiandmed).
import üleriigilisedNimekirjad from '@andmed/yleriigiline.txt?raw';
import rk2023 from '@andmed/rk2023/tulemus.json?raw';
import küsitlused from '@veebiandmed/toetused.json';
import type { AndmeTekstid } from './protokoll';

const ringkonnaFailid = import.meta.glob('../../../src/jvmMain/resources/ringkond*.txt', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>;

const ringkondadeTekstid = Array.from({ length: 12 }, (_, i) => {
  const tekst = ringkonnaFailid[`../../../src/jvmMain/resources/ringkond${i + 1}.txt`];
  if (tekst === undefined) throw new Error(`Ringkonna ${i + 1} kandidaatide fail puudub`);
  return tekst;
});

export const andmeTekstid: AndmeTekstid = { ringkondadeTekstid, üleriigilisedNimekirjad, rk2023 };

export interface Küsitlus {
  küsitleja: string;
  kuupäev: string;
  /** küsitluse viga simulatsiooni jaoks: [üleriigiline, ringkondlik] logaritmilisel skaalal */
  viga: [number, number];
  erakonnad: string[];
  protsendid: number[][];
}

export const KÜSITLUSED: Record<string, Küsitlus> = küsitlused;
