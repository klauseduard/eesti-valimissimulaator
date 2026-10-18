> **Märkus tehisintellekti kohta.** Selle faili teksti kirjutas
> tehisintellekt (Claude, Anthropic, tööriistas Claude Code) 2026. aasta
> oktoobris.

RESULTS.xml on 2023. aasta Riigikogu valimiste ametlik tulemuste fail.

Allikas: https://opendata.valimised.ee/api/RK_2023/RESULTS.xml
Alla laaditud: 2026-10-18 (faili sisene `generated` väli: 2026-10-15T19:13:24+03:00)
Vorming: https://opendata.valimised.ee skeem `election-result/rk/v2`

Testis kasutatavad väljad:

- `votesAndMandates/party/candidates/candidate` ja
  `votesAndMandates/independentCandidates/candidates/candidate`:
  kandidaadi nimi, ringkond, hääled, lihtkvoodi osakaal (`quota`) ja
  valituks osutunud kandidaadi mandaadi liik (`mandateType`:
  PERSONAL, DISTRICT või COMPENSATION).
- `districts/district/statistics/votes`: kehtivate häälte arv ringkonnas.

Üleriigiliste nimekirjade järjekord võetakse failist
`src/jvmMain/resources/yleriigiline.txt`. See on ühekordse skriptiga võrreldud
ametliku failiga https://opendata.valimised.ee/api/RK_2023/ELECTION_CANDIDATES.xml
(väli `sequenceNumberInAdminUnit`): kõik 958 kirjet kattusid. Kandidaatide
faili ennast hoidlasse ei lisatud, sest see sisaldab kandidaatide isikuandmeid
(sünnikuupäev, sugu, haridus).

Failist RESULTS.xml on käsuga `./gradlew ekspordiRk2023` loodud kompaktne fail
`src/jvmMain/resources/rk2023/tulemus.json` (kandidaatide hääled ja mandaadi liik,
ringkondade kehtivad hääled). Seda loevad test `Rk2023Test` (JVM-il ja Node'is) ja
veebileht. Test `Rk2023EksportTest` kontrollib, et fail vastab XML-failile.
