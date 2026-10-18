import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { KANDIDAADIMUDEL, kandidaadiOsakaalud, mudeliArvud } from '../src/lib/mudel';

describe('mudeli arvud tekstide jaoks', () => {
  it('kandidaatide oodatavad osakaalud vastavad Kotlini aruandele', () => {
    // docs/kandidaatide-mudeli-test.md: kümneliikmeline nimekiri ilma üleriigilise esiviisikuta
    const osad = kandidaadiOsakaalud(10, []);
    expect(osad.slice(0, 3).map((x) => x.toFixed(3))).toEqual(['0.342', '0.181', '0.107']);
    expect(osad.reduce((a, b) => a + b, 0)).toBeCloseTo(1, 12);
  });

  it('esiviisiku lisa suurendab esimese kandidaadi osa 34%-lt 47%-le', () => {
    expect(mudeliArvud.esimene).toBe(34);
    expect(mudeliArvud.esimeneEsiviisikus).toBe(47);
  });

  it('testitulemus tuleb kandidaadimudeli failist', () => {
    expect(KANDIDAADIMUDEL.testitulemus).toEqual({ kohti: 75, lihtne: 44.9, kalibreeritud: 51.4 });
  });

  it('README kordab samu arve', () => {
    const readme = readFileSync(new URL('../../README.md', import.meta.url), 'utf8');
    const { testitulemus } = KANDIDAADIMUDEL;
    for (const tekst of [
      `keskmiselt ${mudeliArvud.esimene}% erakonna`,
      `oodatav osa ${mudeliArvud.esimeneEsiviisikus}%`,
      `valitud ${testitulemus.kohti} kandidaadist õigesti keskmiselt ${String(testitulemus.kalibreeritud).replace('.', ',')}`,
      `nimetas õigesti ${String(testitulemus.lihtne).replace('.', ',')}`,
    ]) {
      expect(readme.replace(/\r?\n/g, ' ')).toContain(tekst);
    }
  });
});
