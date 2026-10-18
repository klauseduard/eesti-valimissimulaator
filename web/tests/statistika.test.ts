import { describe, expect, it } from 'vitest';
import { jaotaSuurimaJäägiga, sagedused, statistika } from '../src/lib/statistika';
import { partiid } from '../src/lib/protokoll';

describe('statistika', () => {
  it('arvutab vähima, suurima, keskmise ja protsentiilid', () => {
    const väärtused = Array.from({ length: 100 }, (_, i) => i + 1);
    expect(statistika(väärtused)).toEqual({ min: 1, max: 100, keskmine: 50.5, mediaan: 50, p05: 5, p95: 95 });
  });

  it('ühe väärtuse statistika', () => {
    expect(statistika([7])).toEqual({ min: 7, max: 7, keskmine: 7, mediaan: 7, p05: 7, p95: 7 });
  });

  it('mediaan on alati üks väärtustest, mitte kahe keskmine', () => {
    expect(statistika([3, 1, 4, 2]).mediaan).toBe(2);
    expect(statistika([10, 30, 20]).mediaan).toBe(20);
  });

  it('tühja hulga statistika on viga', () => {
    expect(() => statistika([])).toThrow();
  });

  it('loeb väärtuste sagedused', () => {
    expect(sagedused([3, 1, 3, 2, 3])).toEqual([[1, 1], [2, 1], [3, 3]]);
  });
});

describe('partiid', () => {
  it('jagab kordused partiideks', () => {
    expect(partiid(1000, 100)).toEqual(Array(10).fill(100));
    expect(partiid(250, 100)).toEqual([100, 100, 50]);
  });

  it('vigane sisend on viga', () => {
    expect(() => partiid(0, 100)).toThrow();
    expect(() => partiid(10, 0)).toThrow();
  });
});

describe('suurima jäägi meetod', () => {
  it('jagab kohad täisosade ja suurimate jääkide järgi', () => {
    // 101 * (0,325; 0,225; 0,45) = 32,825; 22,725; 45,45 -> 32 + 22 + 45 = 99, jääkidest saavad 0,825 ja 0,725
    expect(jaotaSuurimaJäägiga([32.5, 22.5, 45], 101)).toEqual([33, 23, 45]);
  });

  it('summa on alati soovitud arv', () => {
    const tulemus = jaotaSuurimaJäägiga([10.4, 20.4, 30.4, 39.8], 101);
    expect(tulemus.reduce((a, b) => a + b, 0)).toBe(101);
  });

  it('nullid jäävad nulliks', () => {
    expect(jaotaSuurimaJäägiga([0, 50.5, 50.5], 101)).toEqual([0, 51, 50]);
  });

  it('kõigi nullide korral on tulemus nullid', () => {
    expect(jaotaSuurimaJäägiga([0, 0], 101)).toEqual([0, 0]);
  });
});
