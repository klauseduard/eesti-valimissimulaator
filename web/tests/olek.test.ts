import { describe, expect, it } from 'vitest';
import { dekodeeri, kodeeri, vaikimisiOlek, type Teadaolevad } from '../src/lib/olek';

const teadaolevad: Teadaolevad = {
  eelseadistused: ['norstat-2023-03-03', 'emor-2023-03', 'rk2023'],
  lühendid: ['REF', 'EKRE', 'ÜK'],
  vaikimisiEelseadistus: 'norstat-2023-03-03',
};

describe('URL-i olek', () => {
  it('kodeeritud olek dekodeeritakse samaks', () => {
    const olek = {
      ...vaikimisiOlek(teadaolevad),
      eelseadistus: 'emor-2023-03',
      seeme: 42,
      keel: 'en' as const,
      ringkond: 7,
      viga: false,
      kaart: 'EKRE',
      muudatused: { REF: [30.55, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11], ÜK: Array(12).fill(0.5) },
    };
    const { olek: tagasi, vead } = dekodeeri(kodeeri(olek), teadaolevad);
    expect(vead).toEqual([]);
    expect(tagasi).toEqual(olek);
  });

  it('küsitluse viga on vaikimisi arvestatud ja link salvestab ainult väljalülitamise', () => {
    expect(vaikimisiOlek(teadaolevad).viga).toBe(true);
    expect(kodeeri(vaikimisiOlek(teadaolevad))).not.toContain('e=');
    expect(dekodeeri('#e=0', teadaolevad).olek.viga).toBe(false);
    expect(dekodeeri('#e=x', teadaolevad).vead).toEqual(['e=x']);
  });

  it('tühi fragment annab vaikimisi oleku', () => {
    expect(dekodeeri('', teadaolevad)).toEqual({ olek: vaikimisiOlek(teadaolevad), vead: [] });
    expect(dekodeeri('#', teadaolevad, 'en').olek.keel).toBe('en');
  });

  it('toetusprotsendid ümardatakse kahe kümnendkohani', () => {
    const olek = { ...vaikimisiOlek(teadaolevad), muudatused: { REF: Array(12).fill(12.3456) } };
    expect(dekodeeri(kodeeri(olek), teadaolevad).olek.muudatused.REF).toEqual(Array(12).fill(12.35));
  });

  it('tundmatud ja vigased osad jäetakse kõrvale ja teatatakse', () => {
    const { olek, vead } = dekodeeri('#v=tundmatu&s=x&r=13&kaart=XYZ&p.REF=1,2,3&p.ABC=' + Array(12).fill(1).join(',') + '&zzz=1', teadaolevad);
    expect(olek).toEqual(vaikimisiOlek(teadaolevad));
    expect(vead).toEqual(['v=tundmatu', 's=x', 'r=13', 'kaart=XYZ', 'p.REF=1,2,3', 'p.ABC=1,1,1,1,1,1,1,1,1,1,1,1', 'zzz=1']);
  });

  it('tegelikud hääled kehtivad ainult muutmata 2023 tulemusel', () => {
    expect(dekodeeri('#v=rk2023&h=1', teadaolevad).olek.tegelikudHääled).toBe(true);
    expect(dekodeeri('#v=emor-2023-03&h=1', teadaolevad).olek.tegelikudHääled).toBe(false);
    const muudetud = '#v=rk2023&h=1&p.REF=' + Array(12).fill(20).join(',');
    expect(dekodeeri(muudetud, teadaolevad).olek.tegelikudHääled).toBe(false);
  });

  it('protsent üle 100 on viga', () => {
    const { olek, vead } = dekodeeri('#p.REF=' + [101, ...Array(11).fill(1)].join(','), teadaolevad);
    expect(olek.muudatused).toEqual({});
    expect(vead).toHaveLength(1);
  });
});
