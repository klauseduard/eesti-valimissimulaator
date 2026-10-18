// Ehitab Riigikogu valimisringkondade piirid (TopoJSON) Maa- ja Ruumiameti
// haldus- ja asustusjaotuse andmetest.
//
//   node ehita.mjs            kasutab olemasolevat allikad/asustusyksus_shp.zip,
//                             puudumisel laadib selle alla
//   node ehita.mjs --laadi    laadib allikfaili alati uuesti alla
//
// Väljund: ../web/public/data/ringkonnad.topo.json

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import mapshaper from 'mapshaper';

const KAUST = path.dirname(fileURLToPath(import.meta.url));
const ALLIKAD = path.join(KAUST, 'allikad');
const ZIP = path.join(ALLIKAD, 'asustusyksus_shp.zip');
const ALLIKA_URL = 'https://s3.pilw.io/rp-kemit-kataster/EHAK/asustusyksus_shp.zip';
const VALJUND = path.resolve(KAUST, '..', 'web', 'public', 'data', 'ringkonnad.topo.json');

// Lihtsustamise samm meetrites (L-EST97 tasapinnal). Kogu Eesti ~800 px laiusel
// on 1 px ≈ 460 m; Tallinn ~300 px laiusel on 1 px ≈ 85 m. Seepärast lihtsustame
// ringkondi 1–3 peenemalt kui ülejäänuid. Jagatud piirid jäävad ühiseks, sest
// lihtsustus toimub topoloogiliselt (mapshaper jagab kaared naabrite vahel).
const SAMM_EESTI_M = 200;
const SAMM_TALLINN_M = 20;
// Eemaldame saared (ja muud eraldi rõngad), mis on väiksemad kui 1 km².
const SAARE_MIN_PINDALA_M2 = 1e6;
const KVANTEERIMINE = 1e5;

// Riigikogu valimise seadus § 6. Kõik koodid on EHAK koodid.
// Tallinna linnaosad (asustusüksuse kood AKOOD, omavalitsus OKOOD 0784).
const TALLINN_OKOOD = '0784';
const TALLINNA_LINNAOSAD = {
  '0176': 1, // Haabersti
  '0614': 1, // Põhja-Tallinn
  '0339': 1, // Kristiine
  '0298': 2, // Kesklinn
  '0387': 2, // Lasnamäe
  '0596': 2, // Pirita
  '0482': 3, // Mustamäe
  '0524': 3, // Nõmme
};
// Tartu linn (omavalitsus, 2017. a haldusreformi järgne piir).
const TARTU_LINN_OKOOD = '0793';
const TARTU_LINN_NR = 10;
// Maakonnad (MKOOD).
const MAAKONNAD = {
  '0037': 4, // Harju (v.a Tallinn)
  '0071': 4, // Rapla
  '0039': 5, // Hiiu
  '0056': 5, // Lääne
  '0074': 5, // Saare
  '0060': 6, // Lääne-Viru
  '0045': 7, // Ida-Viru
  '0052': 8, // Järva
  '0084': 8, // Viljandi
  '0050': 9, // Jõgeva
  '0079': 9, // Tartu (v.a Tartu linn)
  '0087': 11, // Võru
  '0081': 11, // Valga
  '0064': 11, // Põlva
  '0068': 12, // Pärnu
};

function ringkond(a) {
  if (a.OKOOD === TALLINN_OKOOD) return TALLINNA_LINNAOSAD[a.AKOOD];
  if (a.OKOOD === TARTU_LINN_OKOOD) return TARTU_LINN_NR;
  return MAAKONNAD[a.MKOOD];
}

// Sama loogika mapshaperi -each avaldisena.
const NR_AVALDIS =
  `nr = OKOOD == ${JSON.stringify(TALLINN_OKOOD)} ? ${JSON.stringify(TALLINNA_LINNAOSAD)}[AKOOD]` +
  ` : OKOOD == ${JSON.stringify(TARTU_LINN_OKOOD)} ? ${TARTU_LINN_NR}` +
  ` : ${JSON.stringify(MAAKONNAD)}[MKOOD]`;

async function laadiAlla() {
  fs.mkdirSync(ALLIKAD, { recursive: true });
  console.log(`Laadin alla ${ALLIKA_URL}`);
  const vastus = await fetch(ALLIKA_URL);
  if (!vastus.ok) throw new Error(`Allalaadimine ebaõnnestus: HTTP ${vastus.status}`);
  fs.writeFileSync(ZIP, Buffer.from(await vastus.arrayBuffer()));
}

function kontrolliKoosseisu(kirjed) {
  const vead = [];
  const linnaosad = {};
  let tartuLinn = 0;
  for (const a of kirjed) {
    const nr = ringkond(a);
    if (!Number.isInteger(nr)) {
      vead.push(`ringkond puudub: ${a.AKOOD} ${a.ANIMI} (${a.ONIMI}, ${a.MNIMI})`);
    }
    if (a.OKOOD === TALLINN_OKOOD) linnaosad[a.AKOOD] = (linnaosad[a.AKOOD] || 0) + 1;
    if (a.OKOOD === TARTU_LINN_OKOOD) tartuLinn++;
  }
  for (const kood of Object.keys(TALLINNA_LINNAOSAD)) {
    if (linnaosad[kood] !== 1) vead.push(`Tallinna linnaosa ${kood} esineb ${linnaosad[kood] || 0} korda (oodatud 1)`);
  }
  if (tartuLinn === 0) vead.push(`Tartu linna (OKOOD ${TARTU_LINN_OKOOD}) üksusi ei leitud`);
  const leitud = new Set(kirjed.map(ringkond));
  for (let nr = 1; nr <= 12; nr++) if (!leitud.has(nr)) vead.push(`ringkond ${nr} on tühi`);
  if (vead.length) throw new Error('Allikandmed ei vasta ringkondade koosseisule:\n  ' + vead.join('\n  '));
}

async function main() {
  if (process.argv.includes('--laadi') || !fs.existsSync(ZIP)) await laadiAlla();

  const sisend = `-i "${ZIP}" encoding=win1257`;

  // 1. Atribuudid: koosseisu kontroll ja andmete seisu kuupäev.
  const atr = await mapshaper.applyCommands(`${sisend} -o format=json atribuudid.json`);
  const kirjed = JSON.parse(atr['atribuudid.json']);
  kontrolliKoosseisu(kirjed);
  const seisud = [...new Set(kirjed.map((a) => String(a.EKSPORT).slice(0, 10)))];
  if (seisud.length !== 1) throw new Error(`Ootamatu EKSPORT väärtuste hulk: ${seisud.join(', ')}`);
  const seis = seisud[0]; // AAAA-KK-PP
  const [a, k, p] = seis.split('-');
  const viide = `Haldus- ja asustusjaotus: Maa- ja Ruumiamet ${p}.${k}.${a}`;

  // 2. Ringkondade geomeetria.
  const kask = [
    sisend,
    `-each '${NR_AVALDIS}'`,
    '-filter-fields nr',
    '-dissolve nr',
    `-filter-islands min-area=${SAARE_MIN_PINDALA_M2} remove-empty`,
    `-simplify variable interval='nr <= 3 ? ${SAMM_TALLINN_M} : ${SAMM_EESTI_M}' keep-shapes`,
    '-clean',
    '-proj wgs84',
    '-sort nr',
    '-rename-layers ringkonnad',
    `-o format=topojson quantization=${KVANTEERIMINE} ringkonnad.json`,
  ].join(' ');
  const valjund = await mapshaper.applyCommands(kask);
  const topo = JSON.parse(valjund['ringkonnad.json']);

  // Lisame allikaviite faili enda juurde, et veebileht saaks seda näidata.
  topo.metadata = { allikas: viide, seis };

  fs.mkdirSync(path.dirname(VALJUND), { recursive: true });
  const tekst = JSON.stringify(topo);
  fs.writeFileSync(VALJUND, tekst);
  console.log(`Kirjutatud ${path.relative(process.cwd(), VALJUND)} (${(tekst.length / 1024).toFixed(1)} KB)`);
  console.log(`Andmete seis ${seis}. Viide: "${viide}"`);
}

main().catch((viga) => {
  console.error(viga.message || viga);
  process.exit(1);
});
