import { describe, expect, it } from 'vitest';
import { ERAKONNAD, erakonnaInfo } from '../src/lib/erakonnad';
import { teema } from '../src/lib/teema.svelte';

describe('erakondade värvid', () => {
  it('värv tuleb praeguse teema järgi', () => {
    teema.tume = false;
    expect(erakonnaInfo('Eesti Konservatiivne Rahvaerakond').värv).toBe('#4d47b2');
    teema.tume = true;
    expect(erakonnaInfo('Eesti Konservatiivne Rahvaerakond').värv).toBe('#86399b');
    teema.tume = false;
  });

  it('igal erakonnal on hele ja tume värv ning tundmatu erakond saab halli', () => {
    for (const e of Object.values(ERAKONNAD)) expect([e.hele, e.tume].every((v) => /^#[0-9a-f]{6}$/.test(v))).toBe(true);
    expect(erakonnaInfo('Tundmatu').värv).toBe('#8c8c8c');
  });
});
