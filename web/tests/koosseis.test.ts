import { describe, expect, it } from 'vitest';
import { keskmineKoosseis } from '../src/lib/koosseis';

describe('keskmine koosseis', () => {
  it('jagab korduste keskmised 101 kohaks suurima jäägi meetodil', () => {
    const kokkuvõte = { erakonnad: ['B', 'A', 'C'], mandaadid: [[30, 31], [50, 51], [21, 19]] };
    // keskmised A 50,5, B 30,5, C 20; võrdse jäägi korral saab koha eespool olev erakond (A)
    expect([...keskmineKoosseis(['A', 'B', 'C', 'D'], kokkuvõte)]).toEqual([['A', 51], ['B', 30], ['C', 20], ['D', 0]]);
  });
});
