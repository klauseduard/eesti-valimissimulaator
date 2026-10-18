> **Märkus tehisintellekti kohta / Note on AI authorship.**
> Suurema osa selle faili tekstist kirjutas 2026. aasta oktoobris
> tehisintellekt (Claude, Anthropic, tööriistas Claude Code). Autori enda
> kirjutatud on ainult jaotised „Projekti kood / Project Code" ja
> „Saateks arvutiprogrammile" (2023), välja arvatud selle alguses olev
> kursiivis märkus.
>
> Most of the text in this file was written in October 2026 by AI
> (Claude by Anthropic, in Claude Code). Only the sections "Projekti kood /
> Project Code" and "Saateks arvutiprogrammile" (2023), apart from the
> italic note at its start, were written by the author.

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
below for how to run it locally.

The project started in February 2023 as an experiment with GitHub
Copilot. In October 2026 it was rebuilt with Claude Code: the seat
allocation now follows § 62 completely, the code compiles to the JVM and
to JavaScript, and the web page was added. The data are still those of
the 2023 election. The code and the rest of this document are in
Estonian.

## Saateks arvutiprogrammile

*See sissejuhatus on kirjutatud 2023. aasta veebruaris, kui projekt
alustati. 2026. aasta oktoobris tehtud uuendus on kirjeldatud järgmises
jaotises.*

Selle väikese viimistlemata rakenduse ülesandeks on simuleerida
Eesti Vabariigi parlamendivalimiste mandaatide jaotamist
kehtiva valimisseaduse alusel lähtuvalt andmefailis
sisalduvatest erakondade toetusprotsentidest. Tekstifailidena
on kaasa pandud 2023. aasta valimiste kandidaatide
ringkonna- ja üleriigilised nimekirjad, mis on kopeeritud
ametlikelt veebilehtedelt.

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

## 2026. aasta oktoobri uuendus

2026. aasta oktoobris tehti projekt Claude Code'i (Anthropic) abil
ümber. Algne 2023. aasta programm oli käeharjutus ega järginud
valimisseadust lõpuni. Uuendatud programm jagab mandaadid Riigikogu
valimise seaduse § 62 järgi: isikumandaadid (vähemalt lihtkvoot),
ringkonnamandaadid 5% künnise ületanud erakondadele (0,75 jäägi reegel,
kandidaadil vähemalt 10% lihtkvoodist) ja kompensatsioonimandaadid
modifitseeritud d'Hondti meetodil (kandidaadil vähemalt 5% lihtkvoodist),
sealhulgas viigireeglid. Test `Rk2023Test` jagab 2023. aasta tegelike
häälte põhjal samad 101 mandaati nagu ametlik tulemus. Kandidaatidele
seatud 10% ja 5% nõudeid need hääled ei rakenda, seega neid kontrollivad
väikesed näited failis `MandaadijaotusTest.kt`.

Uuenduse käigus muutusid ka järgmised asjad:

- Hääled simuleeritakse 2023. aasta valimiste kehtivate häälte põhjal
  (varem 2019. aasta osavõtu põhjal) ja Norstati andmetes saavad hääli kõik
  kaheksa küsitletud erakonda.
- Kood on Kotlin Multiplatformi projekt: sama kood on nii käsurea
  programmis kui ka JavaScripti moodulina veebilehel.
- Lisandus veebileht kaardi, parlamendi joonise ja mandaatide jagamise
  selgitusega (vt jaotist „Veebileht").
- Testid kontrollivad reegleid, korduvat simulatsiooni ja 2023. aasta
  tulemust nii JVM-il kui ka Node'is.

Andmed on endiselt 2023. aasta omad: kandidaatide nimekirjad, küsitlused
ja ringkondade mandaatide arv. 2027. aasta valimiste andmeid veel pole.
Muudatuste täpne loetelu on failis `CHANGELOG.md`.

## Kuidas hääled simuleeritakse

Andmestikke on kahte liiki. Üksikküsitlused (Norstat, Kantar Emor) on
ühe küsitleja tulemused. Küsitluste koond ühendab kõigi küsitlejate
viimased küsitlused: iga küsitleja küsitlustest arvutatakse ajaliselt
kaalutud keskmine, ja küsitlejate keskmised liidetakse võrdsete kaaludega
(„üks küsitleja, üks hääl"), nii et sama küsitleja korduvad küsitlused ei
loe kui sõltumatud. Küsitlejate varasema täpsuse järgi kaalumist katsetati,
kuid see ei olnud tagasitestis täpsem. Koondi ringkondade profiil tuleb
Norstati küsitlusest. Mudeli valikud on kirjas failis
`docs/kusitluste-koond.md` ja tagasitesti tulemused failis
`docs/tagasitestimine.md`.

Korduvas simulatsioonis (veebilehel ja käsuga `--runs=N`) kõigutab iga
kordus kõigepealt erakondade toetust küsitluse vea võrra. Iga erakonna
toetus korrutatakse juhusliku teguriga, mis koosneb kahest osast: ühine
osa kõigi ringkondade jaoks (küsitlus eksib erakonna toetuses enamasti
kõikjal samas suunas) ja väiksem osa iga ringkonna jaoks. Seejärel
skaleeritakse ringkonna erakondade toetused tagasi esialgse summani.
Üleriigilise osa suurus tuleb küsitlejate täpsuse tagasitestist 2011–2023
valimistel: see on kõigi küsitlejate ühise vea ja küsitleja majaefekti
summa, ning koondi viga on väiksem kui üksikküsitluse oma (logaritmilisel
skaalal umbes 0,11 ja 0,16). Ringkondliku osa (0,251) saab mõõta ainult
2023. aasta valimistel, sest varasemate valimiste ringkondade tulemusi
avaandmetes pole. Küsitluse vea saab välja lülitada veebilehel või käsuga
`--ilma-veata`. Ühekordne simulatsioon ja veebilehe näidiskordus kasutavad
küsitluse toetust täpselt.

Erakonna hääled ringkonnas on erakonna toetusprotsent selles ringkonnas
korrutatud ringkonna kehtivate häälte arvuga. Kehtivate häälte arv on
võetud 2023. aasta valimiste ametlikust tulemusest. Norstati andmetes
(reitingud.ee) on kaheksa erakonna toetus, Emori andmetes (fail
`Andmed.kt`) kuue erakonna toetus. Teiste nimekirjade kandidaadid ja
üksikkandidaadid hääli ei saa.

Erakonna hääled jagatakse ringkonna kandidaatide vahel juhuslikult,
kuid nii, et kandidaadi oodatav osa sõltub tema kohast erakonna
ringkonnanimekirjas. Oodatav osa kahaneb nimekirja järjekorras:
kümneliikmelise nimekirja esimene saab keskmiselt 34% erakonna
häältest, teine 18%, kolmas 11%. Kui esimene kandidaat kuulub erakonna
üleriigilise nimekirja esiviisikusse, on tema oodatav osa 47%. Igas korduses
kõiguvad osad oodatava ümber Dirichlet' jaotuse järgi. Jaotuse
parameetrid on sobitatud 2023. aasta valimiste kandidaatide häältele;
mudel ja selle test on kirjeldatud failides `docs/kandidaatide-mudel.md`
ja `docs/kandidaatide-mudeli-test.md`. Testis, kus iga ringkond jäeti
kordamööda sobitamisest välja, nimetas see jaotus ringkonna tasandil
valitud 75 kandidaadist õigesti keskmiselt 51,4. Simulaatori esialgne
lihtne jaotus nimetas õigesti 44,9: selles saab esimene kandidaat
juhusliku osa erakonna häältest (keskmiselt poole), teine juhusliku osa
ülejäänust ja nii edasi. Lihtne jaotus on endiselt valitav (käsureal
`--jaotus=lihtne`).

Kandidaatide failis on kandidaadid erakonna ringkonnanimekirja
järjekorras, mille määrab erakond (RKVS § 29 lg 7): failid on
registreerimisnumbrite järjekorras ja 2023. aasta ametliku kandidaatide
faili järgi kattus see kõigi 112 ringkonnanimekirja puhul kandidaatide
järjekorraga ringkonnanimekirjas. Kumbki jaotus ei arvesta kandidaadi
isiklikku populaarsust, seega sõltub kandidaadi valituks osutumise
sagedus peamiselt tema kohast nimekirjas. Tulemusi tuleb lugeda selle
eelduse valguses.

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
- `--ilma-veata`: ära arvesta korduvates simulatsioonides küsitluse viga.
- `--jaotus=lihtne`: jaga erakonna hääled kandidaatide vahel simulaatori
  esialgse lihtsa jaotusega (vaikimisi 2023. aasta järgi kalibreeritud
  jaotus).

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
