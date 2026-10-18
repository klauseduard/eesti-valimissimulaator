<script lang="ts">
  import { arv } from '../lib/i18n.svelte';
  import { mudeliArvud as m } from '../lib/mudel';
  import { DOKUMENDID as d } from './lingid';
</script>

<p class="märkus" data-testid="ai-märkus">
  Märkus tehisintellekti kohta. Selle lehe teksti kirjutas tehisintellekt (Claude, Anthropic, tööriistas Claude Code)
  2026. aasta oktoobris. Vigade suurused ja kandidaatide jaotuse arvud tulevad programmi loodud failidest.
</p>

<h1>Metoodika</h1>

<p>
  Simulaator ei ennusta valimistulemust. See jagab mandaadid Riigikogu valimise seaduse järgi nii, nagu küsitluste
  toetus oleks valimistulemus, ja kordab seda 1000 korda juhusliku küsitluse veaga. Seepärast on peamised tulemused
  vahemikud ja tõenäosused: erakonna mandaatide 90% vahemik ja mediaan ning see, kui sageli erakond või koalitsioon
  mingi piiri ületab. Poolring näitab korduste keskmist koosseisu ega ole ühegi korduse tulemus. Kõik andmed on
  2023. aasta valimiste omad, seega kirjeldab simulaator praegu 2023. aasta valimisi nii, nagu need paistsid viimaste
  küsitluste põhjal.
</p>

<h2>Andmed</h2>
<p>
  Kandidaadid ja nende järjekord ringkonnanimekirjades, ringkondade mandaatide arv ja kehtivate häälte arv
  ringkonniti on võetud 2023. aasta valimiste ametlikest andmetest. Erakondade toetus ringkonniti tuleb Norstati
  andmetest lehel reitingud.ee (kaheksa erakonda) ja Kantar Emori märtsi küsitlusest (kuus erakonda). Küsitluste
  koond ja küsitlejate täpsuse hinnang kasutavad üleriigilisi küsitlusi aastatest 2011–2023, mis on võetud
  Vikipeediast. Erakonnad, kelle toetust andmetes ei ole, ja üksikkandidaadid hääli ei saa.
</p>

<h2>Küsitluste koond</h2>
<p>
  Vaikimisi eelseadistus on küsitluste koond, mis ühendab kõigi küsitlejate viimased küsitlused nii, et ühe küsitleja
  korduvad küsitlused ei loe sõltumatute küsitlustena. Iga küsitleja kohta arvutatakse kaalutud keskmine tema
  küsitlustest 60 päeva jooksul enne kuupäeva. Küsitluse kaal kasvab valimi suurusega ja väheneb poole võrra iga 14
  päevaga, ja küsitlus loetakse kättesaadavaks 3 päeva pärast välitöö lõppu. Seejärel saab iga küsitleja võrdse kaalu.
  Kaalumist küsitlejate varasema täpsuse järgi katsetati, kuid see ei olnud tagasitestis täpsem, seega seda ei
  kasutata. Koond annab üleriigilise toetuse. Ringkondade jaotus tuleb Norstati 27. veebruari andmetest, mida
  skaleeritakse nii, et iga erakonna üleriigiline toetus vastab koondile. Mudeli valikud ja nende vastuvõtmise
  tingimused pandi kirja enne tagasitesti (<a href={d.koond}>kirjeldus</a>,
  <a href={d.tagasitest}>tagasitesti aruanne</a>).
</p>

<h2>Küsitluse viga</h2>
<p>
  Iga kordus korrutab erakonna toetuse juhusliku teguriga, mille keskväärtus on 1. Teguril on kaks osa. Üleriigiline
  osa on erakonnal kõigis ringkondades sama, sest küsitlus eksib erakonna toetuses enamasti kõikjal samas suunas.
  Ringkondlik osa on igas ringkonnas eraldi. Seejärel skaleeritakse iga ringkonna erakondade toetus tagasi esialgse
  summani, sest ühe erakonna alahindamine tähendab teiste ülehindamist.
</p>
<p>
  Üleriigilise osa suurus on hinnatud nelja valimise (2011, 2015, 2019 ja 2023) küsitluste ja tulemuste põhjal. See
  koosneb küsitleja majaefektist, mis on küsitleja süstemaatiline kalle, ja kõigi
  küsitlejate ühisest veast (tüüpiliselt ±{m.ühineViga}%). Tüüpiline viga on koondi puhul ±{m.vigaKoond}% erakonna
  toetusest, ühe Norstati küsitluse puhul ±{m.vigaNorstat}% ja Emori küsitluse puhul ±{m.vigaEmor}%. Ringkondlik osa
  (±{m.vigaRingkond}%) on mõõdetud 2023. aasta valimistel, sest ringkondade tulemused on avaandmetena olemas ainult
  selle aasta kohta. Viga kehtib küsitlustele, mis on tehtud kuni 14 päeva enne valimisi. Pikema ajavahe viga oli
  tagasitestis ebausaldusväärne, sest selle määras peamiselt üks sündmus, Vabaerakonna tõus 2015. aastal.
</p>

<h2>Hääled kandidaatidele</h2>
<p>
  Erakonna hääled ringkonnas jagatakse kandidaatide vahel juhuslikult, kuid kandidaadi oodatav osa sõltub tema kohast
  ringkonnanimekirjas. Kümneliikmelise nimekirja esimene saab keskmiselt {m.esimene}% erakonna häältest. Kui ta kuulub
  erakonna üleriigilise nimekirja esiviisikusse, on tema oodatav osa {m.esimeneEsiviisikus}%. Igas korduses kõiguvad
  osad oodatava ümber Dirichlet' jaotuse järgi. Jaotuse parameetrid on sobitatud 2023. aasta kandidaatide häältele.
</p>
<p>
  Testis, kus iga ringkond jäeti kordamööda sobitamisest välja, nimetas see jaotus ringkonna tasandil valitud
  {m.kohti} kandidaadist õigesti keskmiselt {arv(m.kalibreeritud, 1)}. Simulaatori esialgne lihtne jaotus, kus esimene
  kandidaat saab keskmiselt poole erakonna häältest, nimetas õigesti {arv(m.lihtne, 1)}. Lihtsa jaotuse saab valida
  kandidaatide tabeli juures. Mudel ja test pandi kirja enne testi käivitamist (<a href={d.kandidaadid}>kirjeldus</a>,
  <a href={d.kandidaaditest}>testi aruanne</a>).
</p>

<h2>Mandaatide jagamine</h2>
<p>
  Mandaadid jagatakse Riigikogu valimise seaduse §&nbsp;62 järgi kolmes etapis. Lihtkvoot on ringkonna kehtivate häälte
  arv jagatud ringkonna mandaatide arvuga.
</p>
<ol>
  <li>
    Isikumandaadi saab kandidaat, kelle häälte arv on vähemalt lihtkvoot. See kehtib ka üksikkandidaatidele ja 5%
    künnise alla jäänud erakondade kandidaatidele.
  </li>
  <li>
    Ringkonnamandaate saavad erakonnad, kelle üleriigiline toetus on vähemalt 5%. Erakond saab nii mitu mandaati, mitu
    lihtkvooti tema hääled sisaldavad, ja veel ühe, kui jääk on vähemalt 0,75 lihtkvoodist. Mandaadid saavad erakonna
    enim hääli saanud kandidaadid, kellel on vähemalt 10% lihtkvoodist. Täitmata jäänud kohad lähevad
    kompensatsioonimandaatideks.
  </li>
  <li>
    Kompensatsioonimandaadid jagatakse 5% künnise ületanud erakondade vahel modifitseeritud d'Hondti meetodil
    jagajatega 1, 2<sup>0,9</sup>, 3<sup>0,9</sup> jne, arvestades erakonna juba saadud ringkonnamandaate. Mandaadi saab
    erakonna üleriigilises nimekirjas eespool olev veel valimata kandidaat, kellel on vähemalt 5% lihtkvoodist.
  </li>
</ol>
<p>
  Viigi korral otsustab kandidaatide puhul üleriigilise nimekirja järjekord. Erakondade viigi korral saab mandaadi
  erakond, kelle kandidaadid on kandidaatide koondnimekirjas tagapool (seaduse §&nbsp;62 lõige 5). Rakenduse osad „Kuidas
  ringkonnas mandaadid jagunesid“ ja „Kompensatsioonimandaadid“ näitavad iga sammu ühe korduse kohta.
</p>

<h2>Korduvad simulatsioonid</h2>
<p>
  Rakendus teeb taustal 1000 kordust. Iga kordus kõigutab kõigepealt toetust küsitluse vea võrra. Seejärel arvutab
  see erakonna hääled ringkonnas toetusprotsendist ja 2023. aasta kehtivate häälte arvust, jagab hääled kandidaatide
  vahel ja jagab mandaadid. Korduste põhjal näitab rakendus järgmisi tulemusi:
</p>
<ul>
  <li>
    erakonna mandaatide 90% vahemik (5. kuni 95. protsentiil), mediaan ja korduste jaotus;
  </li>
  <li>kui suures osas kordustest sai erakond vähemalt ühe mandaadi;</li>
  <li>
    kui sageli koalitsioonil on vähemalt 51 kohta, mis on ainult kohtade arv, mitte hinnang koalitsiooni poliitilisele
    võimalikkusele;
  </li>
  <li>kui sageli iga kandidaat valituks osutus.</li>
</ul>
<p>
  Näidiskordus on üks kordus valitud seemnega ilma küsitluse veata. See selgitab mandaatide jagamise samme, kuid ei ole
  kõige tõenäolisem tulemus.
</p>

<h2>Kontrollid</h2>
<p>
  Mandaatide jagamise reegleid kontrollib test, mis annab 2023. aasta kandidaatide tegelike häältega täpselt ametliku
  tulemuse: samad 101 kandidaati samade mandaadiliikidega. See kontrollib reegleid, mitte ebakindlust.
</p>
<p>
  Küsitluse vea üleriigilist osa kontrollib tagasitest, kus iga valimise jaoks hinnati parameetrid ainult teiste
  valimiste põhjal. Kolm päeva enne valimisi jäi erakonna tegelik häälte osakaal koondi 90% vahemikku 22 juhul 23-st
  (erakonnad, kelle tulemus oli vähemalt 3%, valimistel 2011–2023). Teine kontroll võrdleb simulatsiooni
  hajuvust tagasitesti hajuvusega ja nõuab, et need erineksid kõige rohkem 15%.
</p>
<p>
  Kõik simulatsiooni osad, nii üleriigiline ja ringkondlik viga kui ka kandidaatide jaotus, on hinnatud andmetel, mis
  sisaldavad 2023. aasta valimisi, mida simuleeritakse. Seepärast
  ei tõenda see, et 2023. aasta tegelik mandaatide arv jäi kõigil kuuel erakonnal simulatsiooni 90% vahemikku,
  mudeli kalibreeritust. See on ainult mõistlikkuse kontroll.
</p>

<h2>Mida ei modelleerita</h2>
<ul>
  <li>
    Valimisaktiivsus: kehtivate häälte arv igas ringkonnas on 2023. aasta oma. Aktiivsuse muutust ja selle erinevust
    erakonniti ei arvestata, kuigi lihtkvoot sõltub sellest otse.
  </li>
  <li>Kandidaadi isiklik populaarsus: häälte jaotus sõltub ainult kohast nimekirjas.</li>
  <li>
    Uued valimised: ringkondade mandaatide arv ja kandidaatide nimekirjad on 2023. aasta omad. Iga valimise jaoks
    jaotatakse mandaadid ringkondade vahel uuesti (seaduse §&nbsp;7), seega 2027. aasta valimisi simulaator veel ei toeta.
  </li>
  <li>Küsitlused rohkem kui 14 päeva enne valimisi: nende jaoks ei ole usaldusväärset vea hinnangut.</li>
  <li>
    Erakondade vigade seos: valijad liiguvad sarnaste erakondade vahel, kuid seost hinnanud mudel ei olnud
    tagasitestis parem. Seepärast loositakse iga erakonna viga eraldi ja ainus seos tuleb ringkonna toetuse
    skaleerimisest tagasi esialgse summani.
  </li>
  <li>
    Otsustamata vastajad: küsitluse protsendid loetakse otsustanud valijate toetuseks; otsustamata vastajate jagunemist
    ei modelleerita.
  </li>
  <li>Uued ja ühinevad erakonnad.</li>
</ul>

<h2>Lisateave</h2>
<p>
  Programmi kirjeldus ja käsurea kasutamine on failis <a href={d.readme}>README</a>. Mandaatide jagamise reeglid on
  <a href="https://www.riigiteataja.ee/akt/124052024010">Riigikogu valimise seaduses</a>.
</p>
