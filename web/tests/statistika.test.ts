import { describe, expect, it } from 'vitest';
import { sagedused, statistika } from '../src/lib/statistika';
import { partiid } from '../src/lib/protokoll';

describe('statistika', () => {
  it('arvutab vähima, suurima, keskmise ja protsentiilid', () => {
    const väärtused = Array.from({ length: 100 }, (_, i) => i + 1);
    expect(statistika(väärtused)).toEqual({ min: 1, max: 100, keskmine: 50.5, p05: 5, p95: 95 });
  });

  it('ühe väärtuse statistika', () => {
    expect(statistika([7])).toEqual({ min: 7, max: 7, keskmine: 7, p05: 7, p95: 7 });
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
