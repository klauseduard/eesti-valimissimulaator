// Ringkondade piirid (public/data/ringkonnad.topo.json) ja kaardiprojektsioon, ühised suurele kaardile ja
// toetuse paneeli väikesele kaardile. Fail laaditakse ühe korra.
import { geoArea, geoConicConformal, type GeoProjection } from 'd3-geo';
import { feature } from 'topojson-client';
import type { Feature, FeatureCollection, Geometry, Polygon } from 'geojson';
import type { Topology, GeometryCollection } from 'topojson-specification';

export type Ringkond = Feature<Geometry, { nr: number }>;

/** Tallinna ringkonnad, mis on Eesti kaardil liiga väikesed ja saavad lisakaardi */
export const TALLINN = [1, 2, 3];

let laadimine: Promise<{ ringkonnad: Ringkond[]; allikas: string }> | null = null;

export function laadiRingkonnad(): Promise<{ ringkonnad: Ringkond[]; allikas: string }> {
  laadimine ??= (async () => {
    const vastus = await fetch(`${import.meta.env.BASE_URL}data/ringkonnad.topo.json`);
    if (!vastus.ok) throw new Error(`HTTP ${vastus.status}`);
    const topo = (await vastus.json()) as Topology<{ ringkonnad: GeometryCollection<{ nr: number }> }> & {
      metadata?: { allikas?: string };
    };
    const ringkonnad = (feature(topo, topo.objects.ringkonnad) as FeatureCollection<Geometry, { nr: number }>).features;
    return { ringkonnad, allikas: topo.metadata?.allikas ?? '' };
  })();
  // ebaõnnestunud laadimist võib uuesti proovida
  laadimine.catch(() => (laadimine = null));
  return laadimine;
}

/** Lamberti konformne kooniline projektsioon L-EST97 parameetritega */
export function projektsioon(): GeoProjection {
  return geoConicConformal().parallels([58, 59 + 20 / 60]).rotate([-24, 0]);
}

/** mitmeosalise ringkonna suurim osa (nt Tallinna lisakaardil jäävad väikesed saared välja) */
export function suurimOsa(geomeetria: Geometry): Polygon {
  if (geomeetria.type === 'Polygon') return geomeetria;
  if (geomeetria.type !== 'MultiPolygon') throw new Error(`Ootamatu geomeetria ${geomeetria.type}`);
  const osad: Polygon[] = geomeetria.coordinates.map((coordinates) => ({ type: 'Polygon', coordinates }));
  return osad.reduce((suurim, osa) => (geoArea(osa) > geoArea(suurim) ? osa : suurim));
}
