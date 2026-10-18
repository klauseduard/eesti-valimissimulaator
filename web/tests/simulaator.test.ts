import { describe, expect, it } from 'vitest';
import { ToetusJs } from '@simulaator';
import {
  EELSEADISTUSED, eelseadistuseToetus, jaota, kontrolliToetust, olekuToetus, simulaator,
  üleriigilineToetus, üleriigiliseToetuseMuudatused,
} from '../src/lib/simulaator';
import { RK2023, vaikimisiOlek } from '../src/lib/olek';
import { tavaliseks } from '../src/lib/tavaline';

const teadaolevad = { eelseadistused: EELSEADISTUSED, lühendid: [], vaikimisiEelseadistus: 'norstat-2023-03-03' };

describe('simulaator veebilehel', () => {
  it('2023 tegelike häältega annab ametlikud mandaadid', () => {
    const olek = { ...vaikimisiOlek(teadaolevad), eelseadistus: RK2023, tegelikudHääled: true };
    const tulemus = jaota(olek, olekuToetus(olek));
    const mandaadid = Object.fromEntries(tulemus.erakonnad.filter((e) => e.mandaate > 0).map((e) => [e.erakond, e.mandaate]));
    expect(mandaadid).toEqual({
      'Eesti Reformierakond': 37, 'Eesti Konservatiivne Rahvaerakond': 17, 'Eesti Keskerakond': 16,
      'Erakond Eesti 200': 14, 'Sotsiaaldemokraatlik Erakond': 9, 'ISAMAA Erakond': 8,
    });
    expect(tulemus.valitud).toHaveLength(101);
    expect(tulemus.ringkonnad[0].erakonnad[0].valitud.length).toBeGreaterThan(0);
  });

  it('kõik eelseadistused annavad 101 mandaati', () => {
    for (const eelseadistus of EELSEADISTUSED) {
      const olek = { ...vaikimisiOlek(teadaolevad), eelseadistus };
      expect(jaota(olek, olekuToetus(olek)).valitud, eelseadistus).toHaveLength(101);
    }
  });

  it('muudatused asendavad eelseadistuse toetuse', () => {
    const olek = { ...vaikimisiOlek(teadaolevad), muudatused: { REF: Array(12).fill(50) } };
    const toetus = olekuToetus(olek);
    expect(toetus.protsendid[toetus.erakonnad.indexOf('Eesti Reformierakond')]).toEqual(Array(12).fill(50));
  });

  it('üleriigilise toetuse muutmine annab kõigi erakondade ümardatud toetuse', () => {
    const toetus = eelseadistuseToetus('norstat-2023-03-03');
    const muudatused = üleriigiliseToetuseMuudatused(toetus, 'Eesti Reformierakond', 35);
    expect(Object.keys(muudatused)).toHaveLength(8);
    for (const protsendid of Object.values(muudatused)) {
      expect(protsendid).toHaveLength(12);
      for (const p of protsendid) expect(Math.round(p * 100) / 100).toBe(p);
    }
    const olek = { ...vaikimisiOlek(teadaolevad), muudatused };
    expect(üleriigilineToetus(olekuToetus(olek), 'Eesti Reformierakond')).toBeCloseTo(35, 1);
  });

  it('üle 100 protsendi ringkonnas annab vea', () => {
    const toetus = eelseadistuseToetus('norstat-2023-03-03');
    toetus.protsendid[0][0] += 60;
    expect(kontrolliToetust(toetus)).toHaveLength(1);
  });

  it('korduva simulatsiooni kokkuvõte teisendub tavaliseks objektiks', () => {
    const toetus = eelseadistuseToetus('emor-2023-03');
    const korduv = simulaator.korduv(new ToetusJs(toetus.erakonnad, toetus.protsendid), 1, true);
    korduv.korda(20);
    const kokkuvõte = tavaliseks(korduv.kokkuvõte());
    // tavaline objekt läbib struktuurse kloonimise (postMessage) muutumatult
    expect(structuredClone(kokkuvõte)).toEqual(kokkuvõte);
    expect(kokkuvõte.kordi).toBe(20);
    expect(kokkuvõte.mandaadid.every((m) => m.length === 20)).toBe(true);
    expect(kokkuvõte.kandidaadid.reduce((s, k) => s + k.kordi, 0)).toBe(20 * 101);
  });

  it('korduva simulatsiooni esimene kordus on sama mis üksik simulatsioon sama seemnega', () => {
    const olek = { ...vaikimisiOlek(teadaolevad), eelseadistus: 'emor-2023-03', seeme: 5 };
    const toetus = olekuToetus(olek);
    const üksik = jaota(olek, toetus).valitud.map((k) => k.nimi).sort();
    const korduv = simulaator.korduv(new ToetusJs(toetus.erakonnad, toetus.protsendid), 5, false);
    korduv.korda(1);
    expect(tavaliseks(korduv.kokkuvõte()).kandidaadid.map((k) => k.nimi).sort()).toEqual(üksik);
  });
});
