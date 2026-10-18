import { describe, expect, it } from 'vitest';
import { sagedaseimTulemus, sagedused, statistika } from '../src/lib/statistika';
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

describe('sagedaseim tulemus', () => {
  it('leiab kordustest kõige sagedasema terviktulemuse ja selle osakaalu', () => {
    // read: erakonnad, veerud: kordused
    const mandaadid = [
      [3, 2, 3, 3],
      [1, 2, 1, 1],
    ];
    expect(sagedaseimTulemus(mandaadid)).toEqual({ väärtused: [3, 1], kordi: 3, osakaal: 0.75 });
  });

  it('võrdse sageduse korral valitakse esimesena esinenud tulemus', () => {
    expect(sagedaseimTulemus([[1, 2], [2, 1]])).toEqual({ väärtused: [1, 2], kordi: 1, osakaal: 0.5 });
  });

  it('tulemuse summa on alati ühe korduse summa', () => {
    const mandaadid = [[50, 51, 52], [51, 50, 49]];
    const { väärtused } = sagedaseimTulemus(mandaadid);
    expect(väärtused.reduce((a, b) => a + b, 0)).toBe(101);
  });

  it('kordusteta on viga', () => {
    expect(() => sagedaseimTulemus([[]])).toThrow();
  });
});
