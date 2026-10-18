// Joonistab väljundfailist kaks SVG-eelvaadet samamoodi nagu veebileht (d3-geo,
// lon/lat sisend): allikad/eelvaade-eesti.svg (800 px) ja
// allikad/eelvaade-tallinn.svg (ringkonnad 1–3, 300 px).

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { geoMercator, geoPath } from 'd3-geo';
import * as topojson from 'topojson-client';

const KAUST = path.dirname(fileURLToPath(import.meta.url));
const FAIL = process.argv[2] ?? path.resolve(KAUST, '..', 'web', 'public', 'data', 'ringkonnad.topo.json');
const VARVID = ['#e41a1c', '#377eb8', '#4daf4a', '#984ea3', '#ff7f00', '#ffd92f',
  '#a65628', '#f781bf', '#999999', '#66c2a5', '#fc8d62', '#8da0cb'];

const topo = JSON.parse(fs.readFileSync(FAIL, 'utf8'));
const koik = topojson.feature(topo, topo.objects.ringkonnad);

function svg(features, laius, failinimi) {
  const fc = { type: 'FeatureCollection', features };
  const projektsioon = geoMercator().fitWidth(laius, fc);
  const tee = geoPath(projektsioon);
  const [[, y0], [, y1]] = tee.bounds(fc);
  const korgus = Math.ceil(y1 - y0);
  const kujud = features.map((f) =>
    `<path d="${tee(f)}" fill="${VARVID[f.properties.nr - 1]}" stroke="#222" stroke-width="0.6"/>`);
  const sildid = features.map((f) => {
    const [x, y] = tee.centroid(f);
    return `<text x="${x.toFixed(1)}" y="${y.toFixed(1)}" font-size="14" font-family="sans-serif" text-anchor="middle" dominant-baseline="middle">${f.properties.nr}</text>`;
  });
  const sisu = `<svg xmlns="http://www.w3.org/2000/svg" width="${laius}" height="${korgus}" viewBox="0 0 ${laius} ${korgus}">` +
    `<rect width="100%" height="100%" fill="#fff"/>${kujud.join('')}${sildid.join('')}</svg>`;
  fs.mkdirSync(path.join(KAUST, 'allikad'), { recursive: true });
  fs.writeFileSync(path.join(KAUST, 'allikad', failinimi), sisu);
  console.log(`Kirjutatud allikad/${failinimi} (${laius}×${korgus})`);
}

svg(koik.features, 800, 'eelvaade-eesti.svg');
svg(koik.features.filter((f) => f.properties.nr <= 3), 300, 'eelvaade-tallinn.svg');
