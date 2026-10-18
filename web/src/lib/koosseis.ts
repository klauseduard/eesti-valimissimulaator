// Korduste keskmine koosseis 101 kohana: seda näitavad poolring ja kleepuv koosseisu riba, et mõlemal oleksid
// samad arvud.
import { jaotaSuurimaJäägiga } from './statistika';

export const MANDAATE = 101;

/**
 * Erakondade keskmised mandaadid kordustes, jagatuna 101 kohaks suurima jäägi meetodil (võrdse jäägi korral
 * saab koha [erakonnad] järjekorras eespool olev). Erakond, keda kokkuvõttes pole, saab 0.
 */
export function keskmineKoosseis(
  erakonnad: string[],
  kokkuvõte: { erakonnad: string[]; mandaadid: number[][] },
): Map<string, number> {
  const keskmised = erakonnad.map((e) => {
    const m = kokkuvõte.mandaadid[kokkuvõte.erakonnad.indexOf(e)];
    return m && m.length > 0 ? m.reduce((a, b) => a + b, 0) / m.length : 0;
  });
  const jaotus = jaotaSuurimaJäägiga(keskmised, MANDAATE);
  return new Map(erakonnad.map((e, i) => [e, jaotus[i]]));
}
