## Veebileht / Web page

Simulaatorit saab kasutada veebilehel / The simulator runs in the browser at
https://klauseduard.github.io/eesti-valimissimulaator/

## Projekti kood / Project Code

Projekti lähtekood on avalikult saadaval GitHubis / Source code is available on GitHub:
https://github.com/klauseduard/eesti-valimissimulaator

Projekti kloonimiseks / To clone the project:
```bash
git clone https://github.com/klauseduard/eesti-valimissimulaator.git
cd eesti-valimissimulaator
```

## In English

This program simulates how the 101 seats of the Estonian parliament
(Riigikogu) are allocated, following § 62 of the Riigikogu Election Act.
It takes party ratings per electoral district from opinion polls,
generates random votes for the 2023 candidates, and allocates personal,
district and compensation mandates. Repeating the simulation gives the
frequency with which each candidate is elected. A regression test
allocates the seats from the official 2023 votes and reproduces all 101
official seats. Run `./gradlew run --args='--runs=1000'` for 1000
simulations; the options are listed below.

The web page (linked above) runs the same Kotlin code compiled to
JavaScript. It lets you change party support with sliders or per
district, shows the seats on a parliament diagram and a district map,
and explains step by step how § 62 allocates the seats in each district
and in the compensation round. Its source is in `web/`; see "Veebileht"
below for how to run it locally. The code and the rest of this document
are in Estonian.

## Saateks arvutiprogrammile

Selle väikese viimistlemata rakenduse ülesandeks on simuleerida
Eesti Vabariigi parlamendivalimiste mandaatide jaotamist
kehtiva valimisseaduse alusel lähtuvalt andmefailis
sisalduvatest erakondade toetusprotsentidest. Tekstifailidena
on kaasa pandud 2023. aasta valimiste kandidaatide
ringkonna- ja üleriigilised nimekirjad, mis on kopeeritud
ametlikelt veebilehtedelt.

Programm jagab mandaadid Riigikogu valimise seaduse reeglite järgi:
isikumandaadid (vähemalt lihtkvoot), ringkonnamandaadid 5% künnise
ületanud erakondadele (0,75 jäägi reegel, kandidaadil vähemalt 10%
lihtkvoodist) ja kompensatsioonimandaadid modifitseeritud d'Hondti
meetodil (kandidaadil vähemalt 5% lihtkvoodist), sealhulgas viigireeglid
(RKVS § 62). Test `Rk2023Test` jagab 2023.
aasta tegelike häälte põhjal samad 101 mandaati nagu ametlik tulemus.
Kandidaatidele seatud 10% ja 5% nõudeid need hääled ei rakenda, seega
neid kontrollivad väikesed näited failis `MandaadijaotusTest.kt`.

Programm ei ole väga ambitsioonikas, sest on loodud eelkõige
käeharjutusena programmeerijat assisteeriva tööriista
(Github Copiloti) katsetamiseks. Käeharjutuse teemavalik
lähtus otsustamisraskustest lähenevate parlamendivalimiste
hääle andmisel ja uudishimust, kuivõrd võib mõne hääle
taktikaline jagamine mõjutada mandaatide jaotust parteide,
isikute ja erinevate ringkondade kandidaatide vahel.

Katsetusega sai alustatud üsna varsti pärast seda
kui värske Copiloti ärilitsentsi kasutajana (17. veebruari
õhtul) kontorist koju jõudsin. 

Asjaolu, et rakenduses on kasutatud valdavalt eestikeelseid
muutujate ja klasside nimesid ei tähenda, et igapäevaselt
nõnda toimiksin või toimida soovitaksin --
see on pigem esimeste Copiloti kasutamise katsete juhuslik
kõrvalnäht. Arvatavasti seostas Copilot sõnastatud eesmärgi
kiiresti mõne Eesti päritolu koodikatsetusega ja pakkus
esimesed koodiplokid seepärast välja eestikeelsetena.
Eestikeelne sõnavara läbisegi ingliskeelsete Kotlini
konstruktsioonide ja teegimeetoditega ei ole tingimata
mugav lugeda ja edaspidistes hüpoteetilistes iteratsioonides
võib kaaluda ingliskeelsetele nimedele üleminekut.

Veel üks märkus:
Asusin esialgu programmikoodi Copilotile antud instruktsioonidest
tühjendama, kuid taipasin enne sellega lõpule jõudmist, et
näidisrakenduses olnuks mõistlikum need alles jätta ja peatusin.

## Kuidas hääled simuleeritakse

Erakonna hääled ringkonnas on erakonna toetusprotsent selles ringkonnas
korrutatud ringkonna kehtivate häälte arvuga. Kehtivate häälte arv on
võetud 2023. aasta valimiste ametlikust tulemusest. Norstati andmetes
(reitingud.ee) on kaheksa erakonna toetus, Emori andmetes (fail
`Andmed.kt`) kuue erakonna toetus. Teiste nimekirjade kandidaadid ja
üksikkandidaadid hääli ei saa.

Erakonna hääled jagatakse ringkonna kandidaatide vahel juhuslikult
kandidaatide failis olevas järjekorras: esimene kandidaat saab juhusliku
osa erakonna häältest (keskmiselt poole), teine juhusliku osa
ülejäänust ja nii edasi. Failis on kandidaadid erakonna
ringkonnanimekirja järjekorras, mille määrab erakond (RKVS § 29 lg 7):
failid on registreerimisnumbrite järjekorras ja 2023. aasta ametliku
kandidaatide faili järgi kattus see kõigi 112 ringkonnanimekirja puhul
kandidaatide järjekorraga ringkonnanimekirjas. Seetõttu
sõltub kandidaadi valituks osutumise sagedus peamiselt tema kohast
nimekirjas, mitte tema isiklikust populaarsusest. Tulemusi tuleb
lugeda selle eelduse valguses.

Valimisringkondade mandaatide arv on 2023. aasta valimiste oma. Iga
valimise jaoks jaotab Vabariigi Valimiskomisjon mandaadid ringkondade
vahel uuesti valijate arvu järgi (RKVS § 7), seega 2027. aasta
valimiste simuleerimiseks tuleb uuendada ringkondade mandaatide arvu,
kandidaatide nimekirju ja reitinguid.

## Kuidas rakendust kasutada?

Rakenduse ehitamiseks ja käivitamiseks on vaja Java arenduskeskkonda
(JDK) versiooniga 17 või uuem. Programm kompileeritakse JDK 21-ga, mille
Gradle laadib esimesel ehitamisel vajadusel ise alla.

Ühe simulatsiooni käivitamiseks kirjuta:

        ./gradlew run

See kasutab programmiga kaasas olevaid viimaseid Norstati reitinguid
(3. märts 2023). Programm trükib ringkonna- ja kompensatsioonimandaadid
ning kirjutab valitud kandidaadid faili `runs/<ajatempel>-mandaadid.txt`.
Kaust `runs/` luuakse vajadusel ise.

Kuna hääled jagatakse kandidaatide vahel juhuslikult, tasub
simulatsiooni korrata. Näiteks 1000 korduse jaoks kirjuta:

        ./gradlew run --args='--runs=1000'

Programm kirjutab iga kandidaadi valituks osutumise kordade arvu faili
`runs/<ajatempel>-sagedused.txt` ja trükib erakondade mandaatide arvu
vähima, suurima ja keskmise väärtuse. 1000 kordust võtab koos
Gradle käivitamisega umbes kaks sekundit. Skriptid `run-n-times.sh` ja `run-emor-n-times.sh` teevad sama:

        ./run-n-times.sh 1000

Käsurea argumendid on järgmised:

- `--pollster=emor`: kasuta Emori andmeid failist `Andmed.kt`
  (vaikimisi Norstati andmed).
- `--reitingud=FAIL`: kasuta antud Norstati reitingufaili. Kaasas on
  failid kaustas `src/jvmMain/resources/reitingud/`, nt
  `--reitingud=src/jvmMain/resources/reitingud/reitingud-2023-02-27.json`.
- `--runs=N`: korda simulatsiooni N korda.
- `--seed=N`: juhuslikkuse seeme, et tulemus oleks korratav.

Kaustas `examples/` on 2023. aasta kevadel avaldatud reitingute põhjal
arvutatud sagedused, igaüks 1000 korduse ja seemne 1 põhjal. Erand on
fail `probabilities.txt`, mis on loodud programmi varasema versiooniga
100 korduse põhjal (vt faili päist). Need failid kasutavad 2023. aasta
valimiste kehtivate häälte arvu, mida küsitluste ajal veel ei teatud.

Ühekordsete käivituste failide (`runs/*-mandaadid.txt`) kokkulugemiseks
on skript `extract-counts-in-runs.sh`, mis kirjutab tulemuse faili
`stats.txt`.

## Veebileht

Veebileht on kaustas `web/` (Svelte, TypeScript, Vite). Mandaatide
jagamise ja simuleerimise kood on Kotlinis kaustas `src/commonMain/` ja
kompileeritakse nii JVM-i käsurea programmiks kui ka JavaScripti
mooduliks, mida veebileht kasutab. Seega annavad käsurea programm ja
veebileht sama seemne korral sama tulemuse. Ringkondade piirid on
failis `web/public/data/ringkonnad.topo.json`; nende allikas, litsents ja
ehitamine on kirjeldatud failis `kaart/LOEMIND.md`.

Veebilehe käivitamiseks oma arvutis on vaja ka Node.js-i (versioon 22
või uuem):

        cd web
        npm ci
        npm run kotlin
        npm run dev

`npm run kotlin` ehitab Kotlini mooduli ja küsitluste andmefaili
(`./gradlew jsBrowserProductionLibraryDistribution veebiandmed`); seda
tuleb korrata pärast Kotlini koodi muutmist. `npm run dev` trükib
aadressi, millel leht avaneb.

Veebilehe testid:

        npm run check
        npm test
        npm run build
        npm run e2e

`npm run e2e` käivitab brauseritestid ehitatud lehe peal; esimesel korral
on vaja brauser alla laadida käsuga `npx playwright install chromium`.
Pärast muudatuse jõudmist harusse `main` ehitab ja testib GitHub Actions
(fail `.github/workflows/sait.yml`) lehe ning avaldab selle GitHub Pagesis.

## Korratavus ja testid

Kui soovid sama tulemust korduvalt saada, anna käivitamisel juhuslikkuse seeme:

        ./gradlew run --args='--seed=1'

Testide käivitamiseks kirjuta:

        ./gradlew allTests

See käivitab Kotlini testid nii JVM-il kui ka Node'is.

Testid kontrollivad mandaatide jagamist kolmel viisil. Väikesed näited
kontrollivad lihtkvooti, 0,75 jäägi reeglit, kompensatsioonimandaatide
jagajaid, 5% künnist, isikumandaate, kandidaatide 10% ja 5% nõudeid ning
varureeglit, kui 5% nõudele vastavaid kandidaate ei ole, ning
viigireegleid. Kuldsed failid (`src/commonTest/resources/kuldsed/`) kontrollivad, et
sama seemne korral annab simulatsioon sama tulemuse. Test `Rk2023Test`
jagab mandaadid 2023. aasta valimiste tegelike häälte põhjal ja võrdleb
tulemust ametliku tulemusega (vt `src/jvmTest/resources/rk2023/LOEMIND.md`).
Kui muudad mandaatide jagamise reegleid teadlikult, salvesta kuldsed
failid uuesti ülaltoodud käsuga.
