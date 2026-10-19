import { describe, expect, it } from 'vitest';
import { RK2023, vaikimisiOlek } from '../src/lib/olek';
import { jaotuseSammud, järgmine, eelmine, rühmaLõpp, type Sammusuurus } from '../src/lib/sammud';
import { EELSEADISTUSED, jaota, olekuToetus, type Tulemus } from '../src/lib/simulaator';

const teadaolevad = { eelseadistused: EELSEADISTUSED, lühendid: [], vaikimisiEelseadistus: 'koond-2023-03-03' };

function tulemus(tegelikud: boolean): Tulemus {
  const olek = tegelikud
    ? { ...vaikimisiOlek(teadaolevad), eelseadistus: RK2023, tegelikudHääled: true }
    : { ...vaikimisiOlek(teadaolevad), seeme: 1 };
  return jaota(olek, olekuToetus(olek));
}

describe.each([
  ['2023 tegelikud hääled', true],
  ['näidiskordus', false],
])('jaotuse sammud: %s', (_, tegelikud) => {
  const t = tulemus(tegelikud);
  const sammud = jaotuseSammud(t);
  const kohaSammud = sammud.filter((s) => s.kandidaat && s.mandaat);

  it('sammud jagavad täpselt 101 mandaati ja viimane seis on lõpptulemus', () => {
    expect(kohaSammud).toHaveLength(101);
    const lõpp = sammud[sammud.length - 1].jaotatud;
    for (const e of t.erakonnad) expect(lõpp[e.erakond] ?? 0, e.erakond).toBe(e.mandaate);
  });

  it('valitud kandidaadid ja mandaadiliigid on samad mis tulemuses', () => {
    const sammudest = kohaSammud.map((s) => `${s.kandidaat!.regNr}:${s.mandaat}`).sort();
    const tulemusest = t.valitud.map((k) => `${k.regNr}:${k.mandaat}`).sort();
    expect(sammudest).toEqual(tulemusest);
  });

  it('erakondade ringkonna- ja kompensatsioonimandaadid on samad mis tulemuses', () => {
    for (const e of t.erakonnad) {
      const omad = kohaSammud.filter((s) => s.erakond === e.erakond);
      expect(omad.filter((s) => s.mandaat === 'kompensatsioonimandaat').length, e.erakond).toBe(e.kompensatsioonimandaate);
      expect(omad.filter((s) => s.mandaat !== 'kompensatsioonimandaat').length, e.erakond).toBe(e.ringkonnamandaate);
    }
  });

  it('kompensatsioonimandaatide sammud on tulemuse järjekorras', () => {
    const komp = sammud.filter((s) => s.liik === 'kompensatsioonimandaat');
    expect(komp.map((s) => s.kandidaat!.regNr)).toEqual(t.kompensatsioon.map((k) => k.kandidaat.regNr));
    expect(komp.map((s) => s.kompensatsioon)).toEqual(t.kompensatsioon.map((_, i) => i));
  });

  it('jagamata ringkonnamandaatide arv vastab kompensatsioonimandaatide arvule', () => {
    const ringkondades = kohaSammud.filter((s) => s.mandaat !== 'kompensatsioonimandaat').length;
    expect(101 - ringkondades).toBe(t.kompensatsioon.length);
  });

  it('sammu suuruse muutmisel liigutakse rühma lõppu', () => {
    const kvoot = sammud.findIndex((x) => x.liik === 'kvoot');
    const ringkonnaLõpp = sammud.findIndex((x) => x.liik === 'ringkond-lõpp');
    expect(rühmaLõpp(sammud, kvoot, 'ringkond')).toBe(ringkonnaLõpp);
    expect(sammud[rühmaLõpp(sammud, kvoot, 'etapp')].liik).toBe('ringkond-lõpp');
    expect(sammud[rühmaLõpp(sammud, kvoot, 'etapp')].ringkond).toBe(12);
    expect(rühmaLõpp(sammud, kvoot, 'mandaat')).toBe(kvoot);
  });

  it.each(['etapp', 'ringkond', 'mandaat'] as Sammusuurus[])('liikumine sammuga %s jõuab algusest lõppu ja tagasi', (suurus) => {
    const peatused = [0];
    for (let i = 0; ; ) {
      const j = järgmine(sammud, i, suurus);
      if (j === i) break;
      expect(j).toBeGreaterThan(i);
      peatused.push(j);
      i = j;
    }
    expect(peatused[peatused.length - 1]).toBe(sammud.length - 1);
    // peatus on alati rühma viimane samm (või algus), nii et rühma jagamised on peatuses tehtud
    for (const p of peatused.slice(1)) {
      if (p + 1 < sammud.length) expect(sammud[p + 1][suurus === 'mandaat' ? 'indeks' : suurus === 'ringkond' ? 'rühm' : 'etapp']).not.toBe(sammud[p][suurus === 'mandaat' ? 'indeks' : suurus === 'ringkond' ? 'rühm' : 'etapp']);
    }
    const tagasi = [];
    for (let i = sammud.length - 1; ; ) {
      const j = eelmine(sammud, i, suurus);
      if (j === i) break;
      tagasi.push(j);
      i = j;
    }
    expect(tagasi.reverse()).toEqual(peatused.slice(0, -1));
    if (suurus === 'mandaat') expect(peatused).toHaveLength(sammud.length);
  });
});
