import { describe, expect, it } from 'vitest';
import { enamus, koalitsioonid } from '../src/lib/koalitsioonid';
import { häältPuudu, kompensatsiooniPiir } from '../src/lib/piirikohad';

// kolm erakonda, neli kordust; igas korduses kokku 101 kohta
const erakonnad = ['A', 'B', 'C'];
const mandaadid = [
  [50, 52, 49, 51],
  [30, 29, 32, 30],
  [21, 20, 20, 20],
];

describe('koalitsioonid', () => {
  it('enamuse tõenäosus on nende korduste osakaal, kus kohti on vähemalt 51', () => {
    expect(enamus(mandaadid, [0])).toEqual({ tõenäosus: 0.5, keskmine: 50.5 });
    expect(enamus(mandaadid, [1, 2])).toEqual({ tõenäosus: 0.5, keskmine: 50.5 });
    expect(enamus(mandaadid, [0, 2]).tõenäosus).toBe(1);
  });

  it('loetleb minimaalsed koalitsioonid tõenäosuse järgi', () => {
    // Liige on üleliigne, kui koalitsioon ilma temata on enamuses vähemalt pooltes kordustes. A üksi on enamuses
    // pooltes kordustes, seega A + B ja A + C ei ole minimaalsed. B + C on enamuses pooltes kordustes
    // (51, 49, 52, 50) ja kumbki üksi mitte kunagi. Võrdse tõenäosuse korral on väiksem koalitsioon eespool.
    const tulemus = koalitsioonid(erakonnad, mandaadid);
    expect(tulemus.map((k) => k.erakonnad.join('+'))).toEqual(['A', 'B+C']);
    expect(tulemus[0].tõenäosus).toBe(0.5);
  });

  it('ilma üleliigsete liikmeteta koalitsioonid', () => {
    const m = [
      [40, 40],
      [35, 35],
      [26, 26],
    ];
    const tulemus = koalitsioonid(erakonnad, m);
    expect(tulemus.map((k) => k.erakonnad.join('+'))).toEqual(['A+B', 'A+C', 'B+C']);
  });
});

describe('piirikohad', () => {
  it('ringkonnamandaadini puuduvad hääled 0,75 reegliga', () => {
    // lihtkvoot 100: 160 häält = 1,6 kvooti -> 1 koht; 175 häälega 2 kohta, seega puudu 15
    expect(häältPuudu(160, 100)).toBe(15);
    // 180 häält = 1,8 kvooti -> 2 kohta; järgmine koht 275 häälega, seega puudu 95
    expect(häältPuudu(180, 100)).toBe(95);
  });

  it('viimase kompensatsioonimandaadi piir', () => {
    // viimane samm: A sai mandaadi võrdlusarvuga 1000; B võrdlusarv 950 kahe saadud mandaadiga
    const piir = kompensatsiooniPiir({
      erakonnad: ['A', 'B'],
      võrdlusarvud: [1000, 950],
      jubaMandaate: [5, 2],
      erakond: 'A',
    });
    // B hääled = 950 · 3^0,9; vaja 1000 · 3^0,9, seega puudu 50 · 3^0,9 ≈ 134,4 -> 135
    expect(piir).toEqual({ võitja: 'A', järgmine: 'B', puudu: 135 });
  });
});
