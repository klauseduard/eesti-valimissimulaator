> **Märkus tehisintellekti kohta.** Selle faili teksti kirjutas tehisintellekt (Claude, Anthropic, tööriistas Claude Code) 2026. aasta oktoobris.

# Arvamusküsitlused ja valimistulemused

Selles kaustas on Riigikogu valimiste üleriigilised arvamusküsitlused aastatest
2010–2026 ning valimiste 2011, 2015, 2019 ja 2023 ametlikud tulemused. Andmed on
võetud Vikipeediast ja kontrollitud teise keele Vikipeedia, reitingud.ee
4 nädala libisevate keskmiste ning mõne ERR-i artikli abil. Andmed on mõeldud
küsitlejate süstemaatilise kalde ja vea hindamiseks ning küsitluste
koondamise mudeli tagasitestimiseks. Seepärast eelistasin kogumisel
õigsust täielikkusele: kahtlased read on märgitud, mitte vaikides
parandatud.

| fail | küsitlusi | välitöö lõpp | küsitlejad | valimi suurus olemas |
|---|---|---|---|---|
| `rk2011.csv` | 4 | 2010-12 … 2011-02-25 | Kantar Emor 3, Turu-uuringute AS 1 | 0 |
| `rk2015.csv` | 51 | 2011-04 … 2015-02-26 | Kantar Emor 48, Turu-uuringute AS 3 | 0 |
| `rk2019.csv` | 104 | 2015-03-19 … 2019-03-01 | Turu-uuringute AS 51, Kantar Emor 44, Norstat 8, Faktum & Ariko 1 | 94 (neist 4 ligikaudsed) |
| `rk2023.csv` | 302 | 2019-03-11 … 2023-03-03 | Norstat 201, Kantar Emor 55, Turu-uuringute AS 45, Faktum & Ariko 1 | 300 (neist 160 ligikaudsed) |
| `rk2027.csv` | 263 | 2023-03-13 … 2026-10-07 | Norstat 180, Kantar Emor 44, Turu-uuringute AS 39 | 262 (neist 4 ligikaudsed) |

`tulemused.csv` sisaldab nelja valimise tulemusi erakondade kaupa (41 rida).
Skript `skriptid/laadi_kusitlused.py` laadib lähtetekstid uuesti alla ja koostab kõik CSV-failid
uuesti.

## Allikad ja litsents

Kõik lehed laaditi MediaWiki API kaudu (`action=parse`, `prop=wikitext`)
2026-10-18. Tabelis on lehe versiooni number (`revid`). Sama versiooni saab
avada aadressil `https://<keel>.wikipedia.org/w/index.php?oldid=<revid>`.

| kasutus | leht | revid |
|---|---|---|
| küsitlused 2011, tulemused 2011 | [en: 2011 Estonian parliamentary election](https://en.wikipedia.org/wiki/2011_Estonian_parliamentary_election) | 1366534953 |
| küsitlused 2015, tulemused 2015 | [en: 2015 Estonian parliamentary election](https://en.wikipedia.org/wiki/2015_Estonian_parliamentary_election) | 1366534818 |
| küsitlused 2019 (põhiallikas) | [et: 2019. aasta Riigikogu valimised](https://et.wikipedia.org/wiki/2019._aasta_Riigikogu_valimised) | 7205073 |
| küsitlused 2019 (ristkontroll) | [en: Opinion polling for the 2019 Estonian parliamentary election](https://en.wikipedia.org/wiki/Opinion_polling_for_the_2019_Estonian_parliamentary_election) | 1306346084 |
| tulemused 2019 | [en: 2019 Estonian parliamentary election](https://en.wikipedia.org/wiki/2019_Estonian_parliamentary_election) | 1374695601 |
| küsitlused 2023 (põhiallikas) | [en: Opinion polling for the 2023 Estonian parliamentary election](https://en.wikipedia.org/wiki/Opinion_polling_for_the_2023_Estonian_parliamentary_election) | 1375859751 |
| küsitlused 2023 (ristkontroll) | [et: Valimistevaheliste arvamusküsitluste tulemused aastatel 2019–2023](https://et.wikipedia.org/wiki/Valimistevaheliste_arvamusk%C3%BCsitluste_tulemused_aastatel_2019%E2%80%932023) | 6670803 |
| tulemused 2023 | [en: 2023 Estonian parliamentary election](https://en.wikipedia.org/wiki/2023_Estonian_parliamentary_election) | 1374857369 |
| küsitlused 2027 (põhiallikas) | [en: Opinion polling for the next Estonian parliamentary election](https://en.wikipedia.org/wiki/Opinion_polling_for_the_next_Estonian_parliamentary_election) | 1378822267 |
| küsitlused 2027 (ristkontroll) | [et: Valimistevaheliste arvamusküsitluste tulemused aastatel 2023–2027](https://et.wikipedia.org/wiki/Valimistevaheliste_arvamusk%C3%BCsitluste_tulemused_aastatel_2023%E2%80%932027) | 7251364 |

Lehte „Opinion polling for the 2027 Estonian parliamentary election“ ei ole
olemas; praeguse valimistsükli küsitlused on lehel „Opinion polling for the
next Estonian parliamentary election“. Vaatasin läbi ka eestikeelsed artiklid
„2011. aasta Riigikogu valimised“ (revid 7185263) ja „2015. aasta Riigikogu
valimised“ (revid 7184392). Neid ei kasutatud, sest 2011. aasta artiklis on
küsitluste kohta üks lause ja 2015. aasta tabelis on ainult päev ja kuu ilma
valimi suuruseta.

Vikipeedia tekst on litsentsitud tingimustel
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). Küsitluste
arvud on faktid, kuid nende kogumise eest tuleb tänada Vikipeedia
kaastöötajaid. Andmete kasutamisel viidake Vikipeedia lehtedele ülaltoodud
versioonide kaupa. Küsitluste esmaallikad (ERR, Postimees, reitingud.ee,
küsitlusfirmade kodulehed) on viidatud Vikipeedia tabelites.

Kontrolliks kasutasin veel kolme allikat, mida CSV-failid otse ei sisalda.
Need on hoidla fail `../reitingud/reitingud-2023-03-03.json` (Norstati 4 nädala
libisevad keskmised 2019–2023), ERR-i artikkel
[Norstat: Eesti 200 tõusis nädala arvestuses teiseks, EKRE langes neljandaks](https://www.err.ee/1608915224/norstat-eesti-200-tousis-nadala-arvestuses-teiseks-ekre-langes-neljandaks)
(14.03.2023) ja ERR-i artikkel
[Faktum & Ariko prognoos: Reformierakonna edu EKRE ees tuleb napp](https://www.err.ee/1608901760/faktum-ariko-prognoos-reformierakonna-edu-ekre-ees-tuleb-napp)
(02.03.2023).

## Küsitluste failide veerud

Kõik viis faili on UTF-8 kodeeringus, komaga eraldatud ja ühesuguse päisega:

```
kusitleja,algus,lopp,valim,REF,KESK,EKRE,ISAMAA,SDE,E200,VABA,ROH,PP,EUVP,RAHVALIIT,MUUD,KOOS,ERK,alus,tyyp,lopp_liik,valim_tekst,allikas,markus
```

Esimesed 16 veergu on ülesandes määratud veerud samas järjekorras; lisaveerud
on nende järel. Veergudes `valim_tekst` ja `markus` võib olla komasid ja
jutumärke, need väljad on CSV reeglite järgi jutumärkides. Iga rida on üks
küsitlus. Read on järjestatud välitöö lõpu järgi, uusim
esimesena. Valimistulemuse read, mandaatide read ja keskmiste read on välja
jäetud.

| veerg | sisu |
|---|---|
| `kusitleja` | `Norstat`, `Kantar Emor` (ka „TNS Emor“, „Emor“, „Kantar-Emor“), `Turu-uuringute AS` (ka „Turu-Uuringute“, „Turu-uuringud“) või `Faktum & Ariko` (ka „OÜ Faktum & Ariko“, „RAIT Faktum & Ariko“). |
| `algus` | Välitöö algus kujul `YYYY-MM-DD`. Tühi, kui allikas annab ainult ühe kuupäeva või kuu. |
| `lopp` | Välitöö lõpp kujul `YYYY-MM-DD`. Kui allikas annab ainult kuu, on väärtus kujul `YYYY-MM`. Päevi ei ole juurde mõeldud. Kui veerus `lopp_liik` on `avaldamine`, on see avaldamise kuupäev, mitte välitöö lõpp. |
| `valim` | Valimi suurus täisarvuna. Kui allikas annab vahemiku või alampiiri (`600–700`, `≥1,000`, `>1000`, `~1000`), on siin alampiir ja algne tekst on veerus `valim_tekst`. Tühi, kui suurus on teadmata. |
| `REF` … `MUUD` | Erakonna toetus protsentides, punkt kümnendmärgina, nii nagu allikas selle annab (täisarvud jäävad täisarvudeks). Tühi tähendab, et erakonda ei küsitud, teda ei olnud olemas või tabelis on kriips. `0` tähendab, et erakonda küsiti ja toetus oli null. |
| `alus` | Tühi: osakaalud erakonna eelistusega vastajatest. `koik_vastajad`: osakaalud kõigist vastajatest, otsustamata vastajad on tabelist puudu. Seda on üks rida (Turu-uuringute AS, jaanuar 2011, summa 78). |
| `tyyp` | Tühi: tavaline küsitlus. `nadal`: Norstati ühe nädala tulemus (umbes 1000 vastajat). `nadal_tuletatud`: nädala tulemus, mille Vikipeedia arvutas libisevatest keskmistest. `prognoos`: Faktum & Ariko valimistulemuse prognoos, mis arvestab valimas käimise tõenäosust. |
| `lopp_liik` | Tühi: `lopp` on välitöö lõpp. `avaldamine`: allikas andis ainult avaldamise kuupäeva (10 Turu-uuringute rida failis `rk2023.csv`). |
| `valim_tekst` | Valimi suuruse algne tekst, kui see ei olnud täisarv. |
| `allikas` | `enwiki` või `etwiki`. `enwiki+etwiki` tähendab, et arvud on inglise Vikipeediast ja välitöö kuupäevad eesti Vikipeediast. |
| `markus` | Vabas vormis märkused, eraldatud semikooloniga: parandused, teise Vikipeedia erinevad arvud või valimi suurused, avaldamise kuupäevad. |

## Erakondade koodid

| kood | erakond | märkus |
|---|---|---|
| `REF` | Eesti Reformierakond | |
| `KESK` | Eesti Keskerakond | |
| `EKRE` | Eesti Konservatiivne Rahvaerakond | `rk2015.csv` ridadel aprillist 2011 kuni veebruarini 2012 on see Eestimaa Rahvaliit, millest EKRE 2012. aasta märtsis moodustati. Vikipeedia tabel paneb need arvud EKRE veergu. |
| `ISAMAA` | Isamaa; 2011–2018 Isamaa ja Res Publica Liit (IRL) | |
| `SDE` | Sotsiaaldemokraatlik Erakond | |
| `E200` | Eesti 200 | |
| `VABA` | Eesti Vabaerakond; 2020. aasta augustist Eesti Tulevikuerakond (TULE) | TULE moodustati Vabaerakonna ja Elurikkuse Erakonna ühinemisel. 2021. aastast on TULE en-vikis veerus „Others“. |
| `ROH` | Erakond Eestimaa Rohelised | |
| `PP` | Erakond Parempoolsed | |
| `EUVP` | Eestimaa Ühendatud Vasakpartei; 2024. aastast Eesti Vasakliit | Aastatel 2019–2023 sisaldub `MUUD` veerus. |
| `RAHVALIIT` | Eestimaa Rahvaliit | Ainult `rk2011.csv`. |
| `KOOS` | Erakond KOOS | Ainult `rk2027.csv`. |
| `ERK` | Eesti Rahvuslased ja Konservatiivid | Ainult `rk2027.csv`, alates 2024. aastast. |
| `MUUD` | Tabeli veerg „Others“ / „Muud“ | Sisaldab mõnikord ka üksikkandidaate. Veergu ei ole arvutatud, see on alati allika enda arv. |

## Tulemuste fail

`tulemused.csv` veerud on `aasta,erakond,protsent,haali`. `haali` on erakonna
hääled ingliskeelse valimisartikli tabelist „Election results“. `protsent` on
arvutatud kui hääled jagatud kehtivate häälte summaga (kõik nimekirjad ja
üksikkandidaadid kokku), kahe komakohaga. Kehtivaid hääli oli 575 133 (2011),
574 150 (2015), 561 141 (2019) ja 610 299 (2023). Lisaks küsitluste koodidele
on kasutatud väikeste nimekirjade koode `VENE` (Vene Erakond Eestis),
`KRISTDEM` (Eesti Kristlikud Demokraadid), `ISESEISVUS` (Eesti
Iseseisvuspartei), `RAHVAYHTSUS` (Rahva Ühtsuse Erakond), `ELURIKKUS`
(Elurikkuse Erakond) ja `YKSIK` (üksikkandidaadid kokku). 2023. aasta hääled
ühtivad täpselt failiga `../rk2023/tulemus.json`, kui summeerida kandidaatide
hääled erakonna kaupa: REF 31,24, EKRE 16,05, KESK 15,28, E200 13,33,
SDE 9,27, ISAMAA 8,21.

## Allika valik valimistsükli kaupa

Iga faili read on ühest põhiallikast. Teisest Vikipeediast on võetud ainult
käsitsi kontrollitud parandused ja üksikud puuduvad read, need on märgitud
veergudes `allikas` ja `markus`.

- **2011.** Ingliskeelses artiklis on 4 küsitlust ilma valimi suuruseta ja
  ainult kuu täpsusega (välja arvatud 25.02.2011). Eestikeelses artiklis on
  ainult üks lause TNS Emori veebruariprognoosi kohta. Rohkem ei leidnud ma
  2011. aasta kohta usaldusväärseid andmeid. 2011. aasta fail sobib seega
  ainult illustreerimiseks, mitte küsitlejate kalde hindamiseks.
- **2015.** Ingliskeelne artikkel: TNS Emori igakuised küsitlused aprillist 2011
  ja kolm Turu-uuringute küsitlust detsembrist 2014. Valimi suurust ei ole
  ühelgi real ja 49 real on ainult kuu.
- **2019.** Eestikeelne artikkel, sest selles on valimi suurus 94 real
  104-st, välitöö täpsed kuupäevad ja Norstati nädala tulemused. Ingliskeelsel
  lehel on Norstati asemel 4 nädala libisevad keskmised, valimi suurust ei ole
  ja enamikul ridadel on ainult kuu. Turu-uuringute 7.–20.02.2019 küsitlusest
  on alles jäetud koondtulemus (valim 4700); veebi- ja näost-näkku alamtulemus
  on välja jäetud. Ingliskeelsest lehest on lisatud üks küsitlus, mida
  eestikeelses artiklis ei ole (Turu-uuringute AS 17.–22.05.2017).
- **2023.** Ingliskeelne leht, sest selles on valimi suurus peaaegu igal real ja
  Kantar Emori 2023. aasta veebruari küsitlustest on seal erakondade reiting.
  Eestikeelne leht kasutab neil nädalatel Kantar Emori kandidaatidega
  küsitluse arve. Eestikeelsest lehest on lisatud kaks Turu-uuringute
  küsitlust, mida ingliskeelsel lehel ei ole (2.–15.04.2019 ja 2.–10.02.2023).
- **2027.** Ingliskeelne leht. Eestikeelsest lehest on lisatud 9 küsitlust,
  mida ingliskeelsel lehel ei ole: 8 küsitlust ajavahemikust 11.08.–07.10.2026
  ja Kantar Emor 27.05.–02.06.2024. Eestikeelses tabelis puuduvad veerud KOOS,
  ERK ja Muud, nii et neil ridadel on need tühjad.

## Norstati read ja hoidla reitingud.ee andmed

Norstat küsitleb iga nädal umbes 1000 inimest ja avaldab 4 nädala libiseva
keskmise. Hoidla failid `../reitingud/reitingud-*.json` sisaldavad neid
libisevaid keskmisi. `rk2023.csv` ja `rk2027.csv` Norstati read on aga
enamasti ühe nädala tulemused. Seepärast ei ole viimane Norstati rida enne
2023. aasta valimisi (28.02.–03.03.2023: REF 33,1, KESK 14,9, EKRE 15,6)
sama mis reitingud.ee viimane libisev keskmine 07.02.–03.03.2023 (REF 28,6,
EKRE 20,9, KESK 18,1). Selle rea arvutas Vikipeedia libisevatest keskmistest
valemiga 4 × keskmine − kolme eelmise nädala tulemus. Kontrollisin, et
valemi keskmised on täpselt reitingud.ee väärtused. Valem korrutab
ümardusvea neljaga, seega on rea viga kuni umbes ±0,35 punkti suurem kui
avaldatud arvul. Eestikeelne leht annab samale nädalale teised arvud
(REF 32,5, KESK 15,4, EKRE 16,2, E200 14,5). Ma ei leidnud, kumb on Norstati
avaldatud nädala tulemus.

Nädala ridu kontrollisin nii: iga reitingud.ee 4 nädala akna kohta võtsin
nelja nädala rea keskmise ja võrdlesin seda libiseva keskmisega. 194 aknast
erines keskmine enam kui 0,3 punkti 33 aknas. Enamik erinevustest on 0,3–0,5
punkti ja esineb ka eestikeelse lehe andmetes. Tõenäoline põhjus on see, et
Norstat kaalub nädalaid nende valimi suuruse järgi. Kaks erinevust olid
selgelt ingliskeelse lehe sisestusvead (vt allpool) ja need on parandatud.

Ingliskeelsel 2023 lehel oli üks 4 nädala libisev keskmine (18.03.–15.04.2019,
valim 4014). See on välja jäetud, sest samad vastajad on nädala ridades.
Failides ei ole seega ühtegi libisevat keskmist.

## Parandused

Need parandused on skriptis `skriptid/laadi_kusitlused.py` loendis `PARANDUSED`. Iga parandus on
ka vastava rea veerus `markus`.

| fail | rida | parandus | põhjendus |
|---|---|---|---|
| `rk2023.csv` | Norstat 01.–07.12.2021 | REF 22,3 → 25,8 | eestikeelne leht; reitingud.ee keskmised; ingliskeelse rea summa oli 96,5 |
| `rk2023.csv` | Norstat 08.–14.03.2022 | E200 18,8 → 16,2 | eestikeelne leht; reitingud.ee keskmised; ingliskeelse rea summa oli 102,7 |
| `rk2023.csv` | Faktum & Ariko 2023 | algus 20.02. → 10.02. | ERR-i artikkel ja eestikeelne leht |
| `rk2027.csv` | Norstat 2024 | algus 23.07. → 29.07. | ingliskeelsel lehel „23-4 Aug“ (13 päeva); eestikeelne leht |
| `rk2027.csv` | Norstat 2026 | algus 13.02. → 23.02. | ingliskeelsel lehel „13-1 March“ (17 päeva); eestikeelne leht |

## Teadaolevad lüngad ja kahtlased read

- **Valimi suurus puudub** kõigil 2011. ja 2015. aasta ridadel. 2019. aastal
  puudub see 10 real ja 2023. aastal 2 real (Kantar Emor jaanuar ja veebruar
  2020). Norstati nädala valim on 2019–2021 antud kui „≥1,000“; veerus
  `valim` on siis 1000. Turu-uuringute 2022.–2023. aasta küsitluste valim on
  antud kui „600–700“; veerus `valim` on siis 600.
- **Kuu täpsusega kuupäevad:** 3 rida 2011. aastal, 49 rida 2015. aastal,
  8 rida 2019. aastal ja 2 rida 2023. aastal (Kantar Emor jaanuar ja juuni
  2020). Ingliskeelne leht annab jaanuari küsitlusele kuupäevad „1–31 Jan“;
  see on tõenäoliselt kuu, mitte välitöö, ja on salvestatud kuuna. Kantar
  Emori rida „1–22 Feb 2020“ on jäetud kuupäevadega, kuid eestikeelne leht
  annab ka sellele ainult kuu, nii et need päevad võivad olla ebatäpsed.
- **Avaldamise kuupäevad:** 11 Turu-uuringute rida (avaldatud 18.02.2022
  kuni 27.01.2023) andis ingliskeelne leht avaldamise kuupäevaga. Ühe rea (avaldatud 27.01.2023) välitöö aeg on
  eestikeelsest lehest (12.–20.01.2023). Ülejäänud 10 real on `lopp`
  avaldamise kuupäev ja `algus` on tühi. Eestikeelne leht annab neile ainult
  kuu, mis näib olevat samuti avaldamise kuu.
- **Summa alla 95:** 37 Turu-uuringute rida failis `rk2019.csv`
  (summad 86–95), sest eestikeelne tabel ei anna neile „Muud“ väärtust.
  Ingliskeelne leht annab neile muud erakonnad kui jäägi, kuid neid väärtusi ei
  ole lisatud. Lisaks: Kantar Emor 10.–16.05.2018 (94,0), Kantar Emor
  20.–23.05.2019 (92,1, puuduvad ROH ja muud), Kantar Emor 18.–24.07.2019
  (94,7, puuduvad ROH ja muud), Turu-uuringute AS 06.–17.02.2020 (92,0) ja üks
  eestikeelsest lehest lisatud Turu-uuringute rida 01.–07.10.2026 (92,0,
  puudub Muud). Suurim summa on 103,5.
- **Kantar Emor 2023. aasta veebruaris** avaldas kaks versiooni: erakondade
  reitingu ja küsitluse, kus vastajale näidati kandidaate. Failis on
  erakondade reiting; eestikeelse lehe kandidaatidega arvud on veerus
  `markus`. Viimase küsitluse (28.02.–02.03.2023: REF 29,0, KESK 16,0,
  EKRE 14,3) ja 13.–16.02.2023 küsitluse kontrollisin ERR-i artiklitest.
- **Faktum & Ariko** ridadel (2019 ja 2023) on prognoos, mis arvestab valimas
  käimise tõenäosust (`tyyp=prognoos`), mitte toetus.
- **Eestikeelse 2023–2027 lehe vead:** märtsi ja aprilli 2023 Norstati
  ridadel on KESK ja EKRE vahetuses. ERR-i 14.03.2023 artikkel kinnitab
  ingliskeelse lehe arve (7.–13.03.2023: KESK 14,9, EKRE 13,5). Erinevus on
  märgitud veerus `markus`. Eestikeelse 2019–2023 lehe kaks Turu-uuringute
  küsitlust on aastaga 2021, kuigi viidatud ERR-i artiklid on 2022. aastast;
  neid ridu ei ole lisatud.
- **Valimi suuruse erinevused:** 16 real annavad kaks Vikipeediat erineva
  valimi suuruse (näiteks Kantar Emor 05.–12.12.2019: 1407 ja 1093). Failis
  on põhiallika arv, teine on veerus `markus`. Ingliskeelsel 2023 lehel on
  Kantar Emori valim 1268 neljal 2020. aasta real (mai, juuni, august,
  oktoober); see võib olla kopeerimisviga, sest eestikeelne leht annab
  oktoobrile 1030.
- **Puuduvad küsitlused:** Saar Polli küsitlusi ei ole üheski failis.
  Aastatel 2011–2014 on ainult TNS Emori küsitlused, Turu-uuringute omad
  algavad detsembrist 2014. 2011. aasta failis on 4 küsitlust; Vikipeedia
  2007–2011 tsükli küsitlusi ei sisalda.

## Failide uuesti koostamine

`skriptid/laadi_kusitlused.py` vajab ainult Python 3 standardteeki. Vaikimisi laadib see alla
täpselt ülaltoodud lehtede versioonid, nii et väljund on sama:

```
python3 skriptid/laadi_kusitlused.py
```

Lehtede uusimate versioonide kasutamiseks:

```
python3 skriptid/laadi_kusitlused.py --uusim
```

Uusima versiooni puhul teatab skript, kui mõni parandus või väljajätmine ei
leidnud enam oma rida, ja loetleb read, mis on ainult ühes Vikipeedias ja mida
ei ole loendis `LISATUD`. Need tuleb käsitsi üle vaadata.

Parsimine on automaatne: tabelid valitakse lõigu pealkirja järgi, veerud päise
sildi järgi ja tundmatu silt või arv peatab skripti. Automaatse parsimise
tulemust kontrollisin käsitsi. Igast failist võrdlesin vikiteksti lahtritega
6 juhuslikku rida (2011. aastast kõik 4) ja 2015. aasta failist veel 4 rida.
Failide 2019, 2023 ja 2027 kõik read võrdlesin teise keele Vikipeediaga ning
2019–2023 Norstati nädala ridu reitingud.ee libisevate keskmistega.
