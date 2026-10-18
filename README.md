## Projekti kood / Project Code

Projekti lähtekood on avalikult saadaval GitHubis / Source code is available on GitHub:
https://github.com/klauseduard/eesti-valimissimulaator

Projekti kloonimiseks / To clone the project:
```bash
git clone https://github.com/klauseduard/eesti-valimissimulaator.git
cd eesti-valimissimulaator
```

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
Eestikeelne sõnavara läbisegi ingiskeelsete Kotlini
konstruktsioonide ja teegimeetoditega ei ole tingimata
mugav lugeda ja edaspidistes hüpoteetilistes iteratsioonides
võib kaaluda ingliskeelsetele nimedele üleminekut.

Veel üks märkus:
Asusin esialgu programmikoodi Copilotile antud instruktsioonidest
tühjendama, kuid taipasin enne sellega lõpule jõudmist, et
näidisrakenduses olnuks mõistlikum need alles jätta ja peatusin.

Programmist tehtud lihtsustused ei ole mõeldud väikeparteide
diskrimineerimiseks -- ajanappuses jaotasin fiktiivseid hääli
üksnes erakondadele, kellel oli teadaolevalt realistlik
võimalus mandaate võita. Väikeparteide jagu hääli on programmi
käesolevas versioonis jäetud kandidaatide vahel jagamata, sest
see ei mõjuta valimistulemust ja vajaks eraldi
simulatsioonialgoritmi sammude realiseerimist.

## Kuidas rakendust kasutada?

Rakenduse käivitamiseks käsurealt kirjuta:
    
        ./gradlew run
        
See käivitab simulatsiooni Norstati reitingute pealt. Kui soovid kasutada
Emori reitinguid, käivita

        ./gradlew run --args='--pollster=emor'
        
Enne käivitamist tekita kaust nimega "runs/", kuhu iga käivitamise järel
salvestatakse simuleeritud valimistulemus. Mitu korda tasub rakendust
käivitada seepärast, et tulemused on tõenäosuslikud ning korduval
gnereerimisel tõenäosused koonduvad.

Kui soovid rakendust käivitada tsüklis, võid kasutada bashi skripti
`run-n-times.sh` (või `run-emor-n-times.sh`). Kaustas `runs/` leiduvate raportite alusel
kandidaatide nimede esinemiskordade ülelugemiseks lisasin skripti
`extract-counts-in-runs.sh`, mis kirjutab väljundi faili `stats.txt`.

Näiteks, kui soovid käivitada rakendust 100 korda, kirjuta:

         run-n-times.sh 100
või

         run-emor-n-times.sh 100
         
Pärast seda saad kasutada skripti `extract-counts-in-runs.sh`, mis
liidab kokku iga kandidaadi nime esinemiskorrad väljundfailides.
Kui käivitasid algoritmi sada korda, väljendab see ühtlasi
iga kandidaadi valituks saamise tõenäosust.

Kui tahad puhtalt lehelt alustada, pead kaustas runs/ olevad failid
ära kustutama käsitsi.

## Korratavus ja testid

Kui soovid sama tulemust korduvalt saada, anna käivitamisel juhuslikkuse seeme:

        ./gradlew run --args='--seed=1'

Testide käivitamiseks kirjuta:

        ./gradlew test

Testid kontrollivad mandaatide jagamist kolmel viisil. Väikesed näited
kontrollivad lihtkvooti, 0,75 jäägi reeglit, kompensatsioonimandaatide
jagajaid, 5% künnist, isikumandaate, kandidaatide 10% ja 5% nõudeid ning
varureeglit, kui 5% nõudele vastavaid kandidaate ei ole, ning viigireegleid. Kuldsed failid (`src/test/resources/kuldsed/`) kontrollivad, et
sama seemne korral annab simulatsioon sama tulemuse. Test `Rk2023Test`
jagab mandaadid 2023. aasta valimiste tegelike häälte põhjal ja võrdleb
tulemust ametliku tulemusega (vt `src/test/resources/rk2023/LOEMIND.md`).
Kui muudad mandaatide jagamise reegleid teadlikult, salvesta kuldsed
failid uuesti ülaltoodud käsuga.
