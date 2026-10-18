import { describe, expect, it } from 'vitest';
import et from '../src/keeled/et.json';
import en from '../src/keeled/en.json';
import fs from 'node:fs';
import path from 'node:path';

const LÄHTEKOOD = path.resolve(import.meta.dirname, '../src');

function failid(kaust: string): string[] {
  return fs.readdirSync(kaust, { withFileTypes: true }).flatMap((f) => {
    const tee = path.join(kaust, f.name);
    return f.isDirectory() ? failid(tee) : /\.(svelte|ts)$/.test(f.name) ? [tee] : [];
  });
}

describe('sõnastikud', () => {
  it('eesti ja inglise sõnastikus on samad võtmed', () => {
    expect(Object.keys(en).sort()).toEqual(Object.keys(et).sort());
  });

  it('mõlemas keeles on samad parameetrid', () => {
    const parameetrid = (tekst: string) => [...tekst.matchAll(/\{([\p{L}\p{N}_]+)\}/gu)].map((m) => m[1]).sort();
    for (const võti of Object.keys(et)) {
      expect(parameetrid((en as Record<string, string>)[võti]), võti).toEqual(parameetrid((et as Record<string, string>)[võti]));
    }
  });

  it('tõlge asendab ka täpitähtedega parameetrid', async () => {
    const { t } = await import('../src/lib/i18n.svelte');
    expect(t('selgitus.kvoot', { hääli: '100', mandaate: 4, kvoot: '25' })).toBe('100 häält / 4 mandaati = 25 häält.');
    expect(t('viide', { lõige: '3' })).toBe('RKVS § 62 lg 3');
  });

  it('iga lähtekoodis kasutatud püsiv võti on sõnastikus', () => {
    const kasutatud = new Set<string>();
    for (const fail of failid(LÄHTEKOOD)) {
      for (const m of fs.readFileSync(fail, 'utf8').matchAll(/\bt\(\s*'([^']+)'/g)) kasutatud.add(m[1]);
    }
    const puudu = [...kasutatud].filter((v) => !(v in et));
    expect(puudu).toEqual([]);
  });
});
