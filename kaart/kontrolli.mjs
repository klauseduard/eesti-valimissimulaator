// Kontrollib faili web/public/data/ringkonnad.topo.json (või käsureal antud
// faili). Väljub koodiga 1, kui mõni kontroll ebaõnnestub.
//
// Meetod (vt ka LOEMIND.md):
//  1. Faili suurus ja struktuur: objekt "ringkonnad", täpselt 12 tükki,
//     omadus nr on täisarv ja hulk on täpselt {1..12}.
//  2. Rõngaste suund: d3-geo eeldab, et välisrõngas on päripäeva. Vale suuna
//     korral katab tükk kogu maakera miinus ringkonna, mistõttu kontrollime, et
//     iga tüki sfääriline pindala on alla 0,01 steradiaani (Eesti ≈ 0,0011).
//  3. Pindalad: iga ringkonna pindala erineb täislahutusega allikandmetest
//     arvutatud pindalast kuni 2%. See leiab kadunud saared ja koosseisu vead.
//  4. Jagatud kaared (täpne, ilma tolerantsita): naabrite ühine piir peab olema
//     sama kaar. Iga kaart kasutatakse kas üks kord (välispiir: rannajoon,
//     riigipiir, järvekallas) või kaks korda kahe ERI ringkonna poolt. Kui
//     kahel naabril oleks oma koopia piirist, tekiks nende vahele vahe või
//     ülekate; ühise kaare korral on see võimatu.
//  5. Naabrussuhted, mis tuletatakse jagatud kaartest, peavad vastama
//     oodatule (nt Tartu linn piirneb ainult ringkonnaga 9).
//  6. Ühend: kõigi ringkondade ühendil tohib olla ainult üks auk – Võrtsjärv
//     (allikandmetes ei kuulu järv ühessegi asustusüksusesse). Muu auk on vahe
//     ringkondade vahel. Lisaks võrdleme pindalade summat ühendi pindalaga
//     (absoluutne piir 0,01 km²): summa > ühend tähendab ülekatet, summa <
//     ühend vahet. Viimane on vajalik, sest mapshaperi -dissolve täidab väikesed
//     vahed ka valikuga gap-width=0, mistõttu need ei ilmu ühendisse aukudena.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import mapshaper from 'mapshaper';
import { geoArea } from 'd3-geo';
import * as topojson from 'topojson-client';

const KAUST = path.dirname(fileURLToPath(import.meta.url));
const FAIL = process.argv[2] ?? path.resolve(KAUST, '..', 'web', 'public', 'data', 'ringkonnad.topo.json');

const MAX_BAITE = 120 * 1024;
const MIN_BAITE = 50 * 1024; // ainult hoiatus: väiksem fail on ilmselt liiga lihtsustatud
const R = 6371008.8; // Maa keskmine raadius meetrites
const km2 = (sr) => (sr * R * R) / 1e6;

// Täislahutusega allikandmetest (asustusyksus_shp.zip, seis 02.09.2026)
// arvutatud sfäärilised pindalad, km².
const VIITEPINDALA = {
  1: 45.1, 2: 76.4, 3: 37.1, 4: 6895.6, 5: 5755.5, 6: 3675.3,
  7: 2955.7, 8: 6062.5, 9: 5710.1, 10: 153.2, 11: 6479.3, 12: 5391.0,
};
const PINDALA_TOLERANTS = 0.02;

// Oodatud maismaanaabrid (ringkondade paarid, millel on ühine piir).
const OODATUD_NAABRID = [
  [1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4],
  [4, 5], [4, 6], [4, 8], [4, 12],
  [5, 12],
  [6, 7], [6, 8], [6, 9],
  [7, 9],
  [8, 9], [8, 11], [8, 12],
  [9, 10], [9, 11],
];

// Võrtsjärve pindala on ~270 km².
const VORTSJARV_KM2 = [240, 300];
// Absoluutne piir. Suhteline piir kogupindalast (~43 000 km²) lubaks liiga
// suuri ülekatteid kahe suure ringkonna vahel.
const MAX_ULEKATE_KM2 = 0.01;

const vead = [];
const viga = (s) => vead.push(s);

// 1. Suurus ja struktuur
const baite = fs.statSync(FAIL).size;
console.log(`Faili suurus: ${(baite / 1024).toFixed(1)} KB`);
if (baite > MAX_BAITE) viga(`fail on suurem kui ${MAX_BAITE / 1024} KB`);
if (baite < MIN_BAITE) console.warn(`HOIATUS: fail on väiksem kui ${MIN_BAITE / 1024} KB`);

const topo = JSON.parse(fs.readFileSync(FAIL, 'utf8'));
const obj = topo.objects?.ringkonnad;
if (!obj) {
  console.error('VIGA: objekt "ringkonnad" puudub');
  process.exit(1);
}
const geomid = obj.geometries;
if (geomid.length !== 12) viga(`tükke on ${geomid.length}, oodatud 12`);
const nrid = geomid.map((g) => g.properties?.nr);
for (const nr of nrid) if (!Number.isInteger(nr)) viga(`nr ei ole täisarv: ${JSON.stringify(nr)}`);
const nrHulk = [...new Set(nrid)].sort((a, b) => a - b).join(',');
if (nrHulk !== '1,2,3,4,5,6,7,8,9,10,11,12' || nrid.length !== 12) viga(`nr väärtused on ${nrid.join(',')}`);
for (const g of geomid) {
  if (g.type !== 'Polygon' && g.type !== 'MultiPolygon') viga(`ringkond ${g.properties?.nr}: tüüp ${g.type}`);
}

// 2. ja 3. Rõngaste suund ja pindalad
const fc = topojson.feature(topo, obj);
let pindalaSumma = 0;
for (const f of fc.features) {
  const nr = f.properties.nr;
  const sr = geoArea(f);
  if (sr >= 0.01) {
    viga(`ringkond ${nr}: pindala ${sr.toFixed(3)} sr – rõngaste suund on vale`);
    continue;
  }
  const a = km2(sr);
  pindalaSumma += a;
  const viide = VIITEPINDALA[nr];
  const suhe = a / viide - 1;
  console.log(`  ${String(nr).padStart(2)}: ${a.toFixed(1).padStart(7)} km² (allikas ${viide} km², ${(suhe * 100).toFixed(2)}%)`);
  if (Math.abs(suhe) > PINDALA_TOLERANTS) viga(`ringkond ${nr}: pindala erineb allikast ${(suhe * 100).toFixed(1)}%`);
}

// 4. Jagatud kaared
const kasutajad = topo.arcs.map(() => []); // kaare indeks -> ringkondade nr-id
function kogu(arcs, nr) {
  if (typeof arcs[0] === 'number') {
    for (const i of arcs) kasutajad[i < 0 ? ~i : i].push(nr);
  } else {
    for (const a of arcs) kogu(a, nr);
  }
}
for (const g of geomid) kogu(g.arcs, g.properties.nr);
const naabrid = new Set();
let valispiir = 0;
let uhispiir = 0;
kasutajad.forEach((k, i) => {
  if (k.length === 0) return;
  if (k.length === 1) valispiir++;
  else if (k.length === 2 && k[0] !== k[1]) {
    uhispiir++;
    naabrid.add([Math.min(...k), Math.max(...k)].join('-'));
  } else viga(`kaar ${i} on kasutusel ${k.length} korda (ringkonnad ${k.join(',')})`);
});
console.log(`Kaari: ${valispiir} välispiiril, ${uhispiir} kahe ringkonna ühisel piiril`);

// 5. Naabrussuhted
const oodatud = new Set(OODATUD_NAABRID.map((p) => p.join('-')));
for (const p of oodatud) if (!naabrid.has(p)) viga(`ringkondadel ${p} puudub ühine piir`);
for (const p of naabrid) if (!oodatud.has(p)) viga(`ootamatu ühine piir ringkondade ${p} vahel`);

// 6. Ühend: augud ja ülekate
const valjund = await mapshaper.applyCommands(
  '-i ringkonnad.json -dissolve gap-width=0 -o format=geojson uhend.json',
  { 'ringkonnad.json': topo },
);
const uhend = JSON.parse(valjund['uhend.json']);
const uhendiGeom = (uhend.features ? uhend.features : uhend.geometries.map((geometry) => ({ geometry })))
  .map((f) => f.geometry);
const tahk = (rong) => {
  const sr = geoArea({ type: 'Polygon', coordinates: [rong] });
  return km2(Math.min(sr, 4 * Math.PI - sr));
};
let uhendiPindala = 0;
const augud = [];
for (const g of uhendiGeom) {
  const poly = g.type === 'Polygon' ? [g.coordinates] : g.coordinates;
  for (const p of poly) {
    uhendiPindala += tahk(p[0]);
    for (const auk of p.slice(1)) {
      const a = tahk(auk);
      uhendiPindala -= a;
      augud.push(a);
    }
  }
}
const vortsjarv = augud.filter((a) => a >= VORTSJARV_KM2[0] && a <= VORTSJARV_KM2[1]);
const muud = augud.filter((a) => !(a >= VORTSJARV_KM2[0] && a <= VORTSJARV_KM2[1]));
console.log(`Ühendi augud: ${augud.map((a) => a.toFixed(3) + ' km²').join(', ') || 'puuduvad'}`);
if (vortsjarv.length !== 1) viga(`Võrtsjärve suurusega auke on ${vortsjarv.length}, oodatud 1`);
if (muud.length) viga(`ühendis on ${muud.length} ootamatut auku (vahed ringkondade vahel?)`);
const ulekate = pindalaSumma - uhendiPindala;
console.log(`Pindalade summa ${pindalaSumma.toFixed(1)} km², ühend ${uhendiPindala.toFixed(1)} km², vahe ${ulekate.toFixed(5)} km²`);
if (ulekate > MAX_ULEKATE_KM2) viga(`pindalade summa ületab ühendi pindala ${ulekate.toFixed(3)} km² võrra (ülekate ringkondade vahel)`);
if (ulekate < -MAX_ULEKATE_KM2) viga(`ühendi pindala ületab pindalade summa ${(-ulekate).toFixed(3)} km² võrra (vahe ringkondade vahel)`);

if (vead.length) {
  console.error('\nKONTROLL EBAÕNNESTUS:');
  for (const v of vead) console.error('  ' + v);
  process.exit(1);
}
console.log('\nKõik kontrollid läbitud.');
