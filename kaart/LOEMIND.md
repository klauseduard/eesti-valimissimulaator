# Valimisringkondade kaart

See kaust ehitab faili `web/public/data/ringkonnad.topo.json`, mis sisaldab
Riigikogu 12 valimisringkonna piire. Fail on TopoJSON-vormingus, koordinaadid on
WGS84 pikkus- ja laiuskraadid (EPSG:4326), nii et veebileht saab neid otse
d3-geo projektsioonidega joonistada. Fail on hoidlas olemas; ümberehitamine on
vajalik ainult siis, kui piirid või ringkondade koosseis muutuvad.

## Allikas

Andmestik: Maa- ja Ruumiameti **haldus- ja asustusjaotus**, kiht
„Asustusüksused SHP" (`asustusyksus_shp.zip`, L-EST97 / EPSG:3301,
mõõtkava 1:10 000).

- Andmestiku leht: <https://geoportaal.maaamet.ee/est/ruumiandmed/haldus-ja-asustusjaotus-p119.html>
- Allalaadimise aadress: <https://s3.pilw.io/rp-kemit-kataster/EHAK/asustusyksus_shp.zip>
- Alla laaditud: 18.10.2026
- Andmete seis (väli `EKSPORT`): 02.09.2026

Asustusüksuste kiht sisaldab iga üksuse juures ka omavalitsuse ja maakonna
koodi (`OKOOD`, `MKOOD`) ning Tallinna linnaosad on selles eraldi üksustena
(`TYYP` = 6). Seepärast piisab ühest kihist ja kõik ringkonnad tekivad samast
geomeetriast, mis tagab, et naabrite piirid langevad täpselt kokku.

Suured järved ei kuulu ühessegi asustusüksusesse. Peipsi järv jääb seetõttu
kaardist välja ja Võrtsjärv on auk ringkondade 8, 9 ja 11 vahel. See on
allikandmete omadus, mitte viga.

## Litsents

Andmestiku lehel on kirjas:

> Haldus- ja asustusüksuste andmed on avaandmed. Andmete avaldamisel tuleb
> viidata Maa- ja Ruumiametile ning märkida andmete seisu kuupäev.
> Näiteks: "Haldus- ja asustusjaotus: Maa- ja Ruumiamet 18.10.2026"

(<https://geoportaal.maaamet.ee/est/ruumiandmed/haldus-ja-asustusjaotus-p119.html>)

Lause „Andmete avaldamisel tuleb viidata Maa- ja Ruumiametile" viitab
kasutustingimustele, mille lingi pealkiri on „Maa- ja Ruumiameti avaandmete
litsents" (<https://kataster.ee/sites/default/files/documents/2024-03/MTP_andmeteenuste_kasutustingimused.pdf>).
Seal on kirjas:

> 1.5. Avalikud andmeteenused ja nende kaudu saadavad andmed on igaühele
> vabaks kasutamiseks.

> 2.5. Teenuse kasutaja võib teenuste kaudu saadud juurdepääsupiiranguta
> andmeid kasutada nii ärilisel kui mitte-ärilisel eesmärgil, sh teha andmed
> üldsusele kättesaadavaks.

Maa- ja Ruumiameti üldine avaandmete litsents (01.01.2025,
<https://geoportaal.maaruum.ee/avaandmete-litsents>) käib sõnastuse järgi
topograafia andmekogu andmete kohta, kuid lubab samuti tuletisi ja nende
levitamist:

> Litsentsisaaja võib teha andmetest tuletisi, sobitada andmeid kokku oma
> toodete või teenustega, kasutada andmeid ärilisel või mitte-ärilisel
> eesmärgil, andmeid edasi levitada.

> Litsentsisaaja kohustub andmete publitseerimisel ja edasi levitamisel
> viitama andmete päritolule, märkides ära litsentsiandja nime, andmestiku
> nimetuse ning andmete vanuse või väljavõtte kuupäeva.

Järeldus: lihtsustatud piirifaili tuletamine ja avaldamine avalikul veebilehel
on lubatud. Kohustuslik on viide, mis sisaldab Maa- ja Ruumiameti nime,
andmestiku nimetust ja andmete seisu kuupäeva (mitte allalaadimise kuupäeva).

### Viide veebilehe jaluses

```
Haldus- ja asustusjaotus: Maa- ja Ruumiamet 02.09.2026
```

Soovitatav on teha sellest tekstist link aadressile
<https://geoportaal.maaruum.ee/avaandmete-litsents>. Üldine litsents nõuab
litsentsi teksti või viidet sellele, kui andmeid edasi levitatakse, ja
TopoJSON-fail on andmete tuletis, mida veebileht levitab.

Sama tekst ja kuupäev on ka TopoJSON-faili ülataseme väljal `metadata`
(`metadata.allikas` ja `metadata.seis`). Pärast ümberehitamist uueneb kuupäev
automaatselt, nii et jalus võib teksti lugeda otse failist.

## Väljundfail

- Objekt `ringkonnad`: 12 tükki (`Polygon` või `MultiPolygon`), igal üks omadus
  `nr` (täisarv 1–12), järjestatud `nr` järgi.
- Eraldi objekti `tallinn` ei ole. Ringkonnad 1–3 on lihtsustatud 20 m
  sammuga ja ülejäänud 200 m sammuga, seega sobib sama objekt nii kogu Eesti
  joonistamiseks (~800 px laiusel on 1 px ≈ 460 m) kui ka Tallinna suurendatud
  lisakaardiks (~300 px laiusel on 1 px ≈ 85 m). Tallinna lisakaardi jaoks
  filtreeri tükid `nr <= 3`. Aegna saar (ringkond 2) asub linnast ~10 km
  põhja pool, seega kõrgendab see `fitWidth`/`fitSize` abil sobitatud
  lisakaarti (300 px laiusel ~377 px kõrge).
- Saared, mille pindala on alla 1 km², on eemaldatud. Saaremaa, Hiiumaa, Muhu,
  Vormsi, Kihnu, Ruhnu, Piirissaar, Aegna jt on alles.
- Koordinaadid on kvanteeritud (1e5 sammu kummaski teljes, ≈ 2,4 m põhja-lõuna ja ≈ 3,7 m ida-lääne suunas).
- Suurus: umbes 57 KB.

Rõngaste suund vastab d3-geo ootusele (välisrõngas päripäeva), seega
`topojson.feature(topo, topo.objects.ringkonnad)` annab otse joonistatavad
tükid.

## Ümberehitamine

Eesmärk: uus `web/public/data/ringkonnad.topo.json`, mis läbib kontrolli.
Eeldus: Node.js 20 või uuem ja võrguühendus.

1. Mine kausta `kaart`.
2. Paigalda sõltuvused käsuga `npm ci`.
3. Ehita fail käsuga `npm run ehita`.
4. Kontrolli faili käsuga `npm run kontrolli`.
5. Soovi korral joonista eelvaated käsuga `npm run eelvaade`.
6. Uuenda selles failis andmete seisu kuupäeva ja allalaadimise kuupäeva.

```
cd kaart
npm ci
npm run ehita
npm run kontrolli
```

`npm run ehita` laadib allikfaili kausta `kaart/allikad/` ainult siis, kui
seda seal veel ei ole. Värske allika saamiseks käivita `node ehita.mjs --laadi`.
Õnnestumise korral trükib skript faili suuruse ja viite teksti, näiteks:

```
Kirjutatud ../web/public/data/ringkonnad.topo.json (57.1 KB)
Andmete seis 2026-09-02. Viide: "Haldus- ja asustusjaotus: Maa- ja Ruumiamet 02.09.2026"
```

Kontroll lõpeb reaga `Kõik kontrollid läbitud.` ja väljumiskoodiga 0.
Kaust `kaart/allikad/` on git-ist välja jäetud.

Kui allikandmetes mõni EHAK kood muutub (näiteks uus haldusreform), katkestab
`ehita.mjs` töö ja nimetab üksused, millele ringkonda ei leitud. Kui piirid
muutuvad, tuleb `kontrolli.mjs` failis uuendada ka viitepindalad
(`VIITEPINDALA`) ja oodatud naabrid (`OODATUD_NAABRID`).

## Töövoog

`ehita.mjs` teeb mapshaperiga järgmised sammud:

1. Loeb `asustusyksus_shp.zip` (kodeering windows-1257).
2. Kontrollib, et iga üksus saab ringkonna, et kõik 8 Tallinna linnaosa
   esinevad täpselt üks kord ja et Tartu linn on olemas.
3. Määrab igale üksusele ringkonna numbri EHAK koodide järgi (tabel allpool).
4. Ühendab üksused ringkondadeks (`-dissolve nr`).
5. Eemaldab alla 1 km² suurused saared.
6. Lihtsustab topoloogiliselt, säilitades jagatud piirid ja väikesed tükid
   (`-simplify variable interval=… keep-shapes`), ning parandab ristumised
   (`-clean`).
7. Teisendab WGS84-sse (`-proj wgs84`) ja kirjutab TopoJSON-i.

## Kontrollimeetod

`kontrolli.mjs` väljub koodiga 1, kui mõni järgmistest tingimustest ei kehti.

- Fail on kuni 120 KB (alla 50 KB annab hoiatuse).
- Objektis `ringkonnad` on täpselt 12 tükki ja `nr` väärtused on täpselt 1–12.
- Iga tüki sfääriline pindala on alla 0,01 steradiaani. Vale rõngasuuna
  korral oleks see ligi 4π, sest d3-geo tõlgendaks tükki kogu maakerana
  miinus ringkond.
- Iga ringkonna pindala erineb täislahutusega allikast arvutatud pindalast
  kuni 2%. See leiab kadunud saared ja vale koosseisu.
- Iga kaart kasutatakse üks kord (välispiir) või kaks korda kahe erineva
  ringkonna poolt. Kui naabrid kasutavad ühist kaart, ei saa nende vahel olla
  vahet ega ülekatet. Seepärast on see kontroll täpne ja tolerantsita.
- Jagatud kaartest tuletatud naabrussuhted vastavad oodatule. Näiteks
  piirneb ringkond 10 ainult ringkonnaga 9.
- Kõigi ringkondade ühendil on täpselt üks auk, Võrtsjärv (240–300 km²).
  Iga muu auk oleks vahe ringkondade vahel.
- Pindalade summa ja ühendi pindala erinevad kuni 0,01 km². Kui summa on
  suurem, katavad ringkonnad üksteist; kui ühend on suurem, on nende vahel
  vahe. See piir on absoluutne, sest suhteline piir kogupindalast (~43 000 km²)
  lubaks mitme ruutkilomeetri suurust viga. Väikesed vahed leiab ainult see
  kontroll, sest mapshaperi `-dissolve` täidab need ühendis.

## Ringkondade koosseis

Riigikogu valimise seadus § 6. Koodid on EHAK koodid.

| Nr | Koosseis | Allikas määratud |
|---:|---|---|
| 1 | Tallinna Haabersti, Põhja-Tallinna ja Kristiine linnaosa | `AKOOD` 0176, 0614, 0339 |
| 2 | Tallinna Kesklinna, Lasnamäe ja Pirita linnaosa | `AKOOD` 0298, 0387, 0596 |
| 3 | Tallinna Mustamäe ja Nõmme linnaosa | `AKOOD` 0482, 0524 |
| 4 | Harju maakond (v.a Tallinn) ja Rapla maakond | `MKOOD` 0037 (v.a `OKOOD` 0784), 0071 |
| 5 | Hiiu, Lääne ja Saare maakond | `MKOOD` 0039, 0056, 0074 |
| 6 | Lääne-Viru maakond | `MKOOD` 0060 |
| 7 | Ida-Viru maakond | `MKOOD` 0045 |
| 8 | Järva ja Viljandi maakond | `MKOOD` 0052, 0084 |
| 9 | Jõgeva maakond ja Tartu maakond (v.a Tartu linn) | `MKOOD` 0050, 0079 (v.a `OKOOD` 0793) |
| 10 | Tartu linn (2017. a haldusreformi järgne omavalitsus) | `OKOOD` 0793 |
| 11 | Võru, Valga ja Põlva maakond | `MKOOD` 0087, 0081, 0064 |
| 12 | Pärnu maakond | `MKOOD` 0068 |
