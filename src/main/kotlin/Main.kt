import andmed.AndmeFailideLugeja
import andmed.MANDAATE_KOKKU
import andmed.Reiting
import andmed.erakonnad
import andmed.hääletanud
import andmed.valimisringkonnad
import mudel.Kandidaat

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// andmestruktuur, mis seostab erakonna nimega tema häälte arvu igas ringkonnas
val erakondadeHääledRingkondadeKaupa = mutableMapOf<String, MutableMap<Int, Int>>()

fun main(args: Array<String>) {

  // graafiline kokkuvõte avatakse ainult --gui lipuga, et mitte blokeerida skriptidega tehtavaid hulgikäivitusi
  val näitaGraafikut = args.contains("--gui")

  // kui tugevasti koonduvad erakonna hääled nimekirja esinumbritele (vt jagaHääledKandidaatideVahel)
  val kahanemistegur = args.firstOrNull { it.startsWith("--decay=") }
    ?.substringAfter("=")?.toDoubleOrNull() ?: VAIKIMISI_KAHANEMISTEGUR

  // küsitlusvea standardhälve protsendipunktides; --sigma=0 annab deterministlikud erakondade häälesaagid
  val küsitlusviga = args.firstOrNull { it.startsWith("--sigma=") }
    ?.substringAfter("=")?.toDoubleOrNull() ?: VAIKIMISI_KÜSITLUSVIGA

 // kui programmi käsureaargumendina on antud --pollster=emor, siis jäta järgmised sammud vahele
  if (args.contains("--pollster=emor")) {
    // kasutame Emori andemeid, mis initsialiseeritakse failis Andmed.kt

  } else {
    // trüki instruktsioonid, kuidas kasutada Emori andmeid
    println("Kasutamiseks vali Emori andmed: --pollster=emor")
    // vaikimisi kasutame Norstati andmeid, mis loetakse reitingufailidest
    var reitingud = loeReitingud()

    // uuenda erakondade massiivis toetusprotsentide andmed
    for (erakond in erakonnad) {
      val toetusedRingkondades = mutableMapOf<Int, Double>()
      erakond.toetusprotsentValimisringkonnas = toetusedRingkondades

      reitingud.filter { it.erakonnaNimi == erakond.nimetus }.forEach() {
        toetusedRingkondades[it.ringkond] = it.reiting
      }
    }
  }



  trükiHääletanuteArv()

  val kandidaadid = loeKandidaatideAndmed()

  // leia igale erakonnale igas ringkonnas antud häälte arv
  leiaErakondadeHääledRingkonniti(kandidaadid, kahanemistegur, küsitlusviga)

  // Riigikogu valimise seaduse järgi osalevad nimekirja- ja kompensatsioonimandaatide
  // jagamisel ainult erakonnad, kes kogusid vähemalt 5% häältest üle riigi.
  // (Künnisest sõltumatuid isikumandaate simulatsioon ei modelleeri.)
  val künniseÜletanudErakonnad = leiaKünniseÜletanudErakonnad()
  println("Valimiskünnise (5%) ületasid: $künniseÜletanudErakonnad")

  val erakondadeRingkonnamandaadidRingkondadeKaupa : Map<Int, Map<String, Int>>
      = arvutaRingkonnaMandaadid(künniseÜletanudErakonnad)

  // leia struktuurist Map<Int, Map<String, Int>> sisemise Mapi väärtuste summa
  val jagatudRingkonnamandaatideArv : Int
      = erakondadeRingkonnamandaadidRingkondadeKaupa.values.map { it.values.sum() }.sum()

  println("erakondade ringkonnamandaadid: $jagatudRingkonnamandaatideArv")

  // sorteeri kandidaadid ringkondades ja erakondades häälte arvu järgi
  val kandidaadidSorteeritud = kandidaadid.sortedWith(compareBy({it.valimisringkond}, {it.hääli})).reversed()

  val erakondadeHääledKoguRiigis = mutableMapOf<String, Int>()
  for ((erakond, kandidaadid) in kandidaadid.groupBy { it.erakond }) {
    erakondadeHääledKoguRiigis[erakond] = kandidaadid.map { it.hääli }.sum()
  }

  println("Häälte jaotus üle riigi: " + erakondadeHääledKoguRiigis)

  val ringkonnamandaadid = eraldaKandidaatidestRingkonnamandaadiSaanud(kandidaadidSorteeritud, erakondadeRingkonnamandaadidRingkondadeKaupa, kandidaadid)

  // Edasine arvestus käib tegelikult kohale saanud kandidaatide järgi, mitte
  // jaotustabeli järgi: kui erakonnal ei jagu ringkonnas piisavalt sobivaid
  // kandidaate, liigub täitmata koht kompensatsioonimandaatide hulka ega kao.
  if (ringkonnamandaadid.size < jagatudRingkonnamandaatideArv) {
    println("Täitmata ringkonnamandaate: ${jagatudRingkonnamandaatideArv - ringkonnamandaadid.size} (liiguvad kompensatsioonimandaatide hulka)")
  }

  val erakondadeMandaatideSummaKoguRiigis = mutableMapOf<String, Int>()
  künniseÜletanudErakonnad.forEach { erakondadeMandaatideSummaKoguRiigis[it] = 0 }
  ringkonnamandaadid.forEach {
    erakondadeMandaatideSummaKoguRiigis[it.erakond] = erakondadeMandaatideSummaKoguRiigis[it.erakond]!! + 1
  }

  // leia ülejäänud mandaadid
  val kompensatsiooniMandaatideArv = MANDAATE_KOKKU - ringkonnamandaadid.size


  // jaga kompensatsioonimandaadid üleriiklike nimekirjade vahel d'Hondti meetodil
  var jagatudMandaadidErakondadeKaupa = erakondadeMandaatideSummaKoguRiigis.toMutableMap()
  val kompensatsioonimandaadid = arvutaKompensatsioonimandaadid(kandidaadid, jagamataMandaate = kompensatsiooniMandaatideArv, jagatudMandaadidErakondadeKaupa, erakondadeHääledKoguRiigis)


  val erakondadeKompensatsioonimandaatideArv = mutableMapOf<String, Int>()
  for (kandidaat in kompensatsioonimandaadid) {
    if (erakondadeKompensatsioonimandaatideArv[kandidaat.erakond]==null)
      erakondadeKompensatsioonimandaatideArv[kandidaat.erakond] = 0 // kui erakonnal veel ei ole mandaate, siis lisa 0
    erakondadeKompensatsioonimandaatideArv[kandidaat.erakond] = erakondadeKompensatsioonimandaatideArv[kandidaat.erakond]!! + 1
  }


  println("\nRINGKONNAMANDAADID: \n")
  ringkonnamandaadid.sortedWith(compareBy({it.hääli})).reversed().forEach { println(it) }

  println("\nKOMPENSATSIOONIMANDAADID: \n")
  kompensatsioonimandaadid.forEach { println(it) }


  val k6ikMandaadid = leiaJaJärjestaMandaadidErakondadeKaupa(ringkonnamandaadid, kompensatsioonimandaadid)

  println("\n\nERAKONDADE KAUPA:\n ")
  k6ikMandaadid.forEach({ println(it) })

  kirjutaJagatudMandaadidFaili(k6ikMandaadid)

  val erakondadeMandaatideSumma = trükiMandaatideSummad(erakondadeMandaatideSummaKoguRiigis, erakondadeKompensatsioonimandaatideArv)

  if (näitaGraafikut) {
    MandateVisualization.showResults(erakondadeMandaatideSumma)
  }
}

private fun eraldaKandidaatidestRingkonnamandaadiSaanud(
  kandidaadidSorteeritud: List<Kandidaat>,
  erakondadeRingkonnamandaadidRingkondadeKaupa: Map<Int, Map<String, Int>>,
  kandidaadid: MutableList<Kandidaat>
): MutableList<Kandidaat> {
  val ringkonnamandaadid = mutableListOf<Kandidaat>()


  // trüki iga ringkonna ringkonnamandaadi saanud kandidaadid võttes arvesse erakonnale antud ringkonnamandaatide arvu
  // eemalda kandidaadinimistust juba mandaadi saanud kandidaadid
  for (ringkond in valimisringkonnad) {
    println("Ringkond ${ringkond.nr} (${ringkond.nimetus})")
    val kandidaadidRingkonnas = kandidaadidSorteeritud.filter { it.valimisringkond == ringkond.nr }
    // seaduse järgi saab nimekirjamandaadi ainult kandidaat, kes kogus vähemalt 10% lihtkvoodist
    val kandidaadiKünnis = 0.1 * hääletanud[ringkond.nr]!! / ringkond.mandaatideArv
    // leiame iga erakonna ringkonnamandaatide arvu ja vastavalt sellele mandaadi saanud kandidaadid
    for ((erakond, mandaate) in erakondadeRingkonnamandaadidRingkondadeKaupa[ringkond.nr]!!) {
      val mandaadiSaajad = kandidaadidRingkonnas
        .filter { it.erakond == erakond && it.hääli >= kandidaadiKünnis }
        .take(mandaate)
      println("  $erakond $mandaadiSaajad")
      ringkonnamandaadid.addAll(mandaadiSaajad)
      // eemalda kandidaatide loendist mandaadi saanud kandidaadid
      kandidaadid.removeAll(mandaadiSaajad)
    }
  }
  return ringkonnamandaadid
}

private fun leiaJaJärjestaMandaadidErakondadeKaupa(
  ringkonnamandaadid: MutableList<Kandidaat>,
  kompensatsioonimandaadid: List<Kandidaat>
): MutableList<Kandidaat> {
  val k6ikMandaadid = mutableListOf<Kandidaat>()
  k6ikMandaadid.addAll(ringkonnamandaadid)
  k6ikMandaadid.addAll(kompensatsioonimandaadid)
  // sorteeri k6ikMandaadid erakonna ja jrkNr järgi
  k6ikMandaadid.sortWith(compareBy({ it.erakond }, { it.jrkNr }))
  return k6ikMandaadid
}

private fun kirjutaJagatudMandaadidFaili(k6ikMandaadid: MutableList<Kandidaat>) {
  val timestamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
  val filename = "runs/" + timestamp + "-mandaadid.txt"
  val file = File(filename)
  file.parentFile.mkdirs()
  file.writeText(k6ikMandaadid.joinToString("\n"))
  // ensure the file ends with EOF
  file.appendText("\n")
  // ensure the file is closed
}

private fun trükiMandaatideSummad(
  erakondadeMandaatideSummaKoguRiigis: MutableMap<String, Int>,
  erakondadeKompensatsioonimandaatideArv: MutableMap<String, Int>
): Map<String, Int> {
  println("RINGKONNAMANDAATIDE JAOTUS: $erakondadeMandaatideSummaKoguRiigis")
  println("KOMPENSATSIOONIMANDAATIDE JAOTUS: $erakondadeKompensatsioonimandaatideArv")

  // arvuta kompensatsioonimandaatide ja ringkonnamandaatide summa erakondade kaupa
  val erakondadeMandaatideSumma = mutableMapOf<String, Int>()
  for (erakond in erakonnad) {
    val a = erakondadeMandaatideSummaKoguRiigis[erakond.nimetus]
    val b = erakondadeKompensatsioonimandaatideArv[erakond.nimetus];
    erakondadeMandaatideSumma[erakond.nimetus] = (a ?: 0) + (b ?: 0)
  }

  println("ERAKONDADE MANDAATIDE KOGUSUMMA: $erakondadeMandaatideSumma")
  return erakondadeMandaatideSumma
}

private fun leiaErakondadeHääledRingkonniti(kandidaadid: MutableList<Kandidaat>, kahanemistegur: Double, küsitlusviga: Double) {
  erakonnad.map { erakond ->
    valimisringkonnad.map { ringkond ->
      val toetus = lisaKüsitlusviga(erakond.toetusprotsentValimisringkonnas[ringkond.nr]!!, küsitlusviga)
      val hääli = arvutaHäälteArv(hääletanud[ringkond.nr]!!, toetus)
      var currentMap = erakondadeHääledRingkondadeKaupa[erakond.nimetus]
      if (currentMap == null) {
        currentMap = mutableMapOf()
        erakondadeHääledRingkondadeKaupa[erakond.nimetus] = currentMap
      }
      currentMap[ringkond.nr] = hääli

      // jaota hääled erakonna selle ringkonna kandidaatide vahel: nimekirja ees paiknevad
      // kandidaadid saavad suurema oodatava osa, kuid iga hääl loositakse eraldi
      val kandidaadidRingkonnas =
        kandidaadid.filter { it.erakond == erakond.nimetus && it.valimisringkond == ringkond.nr }
      val kandidaatideHääled = jagaHääledKandidaatideVahel(hääli, kandidaadidRingkonnas.size, kahanemistegur)
      for (i in kandidaadidRingkonnas.indices) {
        kandidaadidRingkonnas[i].hääli = kandidaatideHääled[i]
      }
      println(kandidaadidRingkonnas)


      hääli
    }
  }
}

/** Leiab erakonnad, kes kogusid vähemalt 5% kõigist antud häältest üle riigi. */
private fun leiaKünniseÜletanudErakonnad(): Set<String> {
  val hääliKokku = hääletanud.values.sum()
  return erakondadeHääledRingkondadeKaupa
    .filterValues { it.values.sum() >= 0.05 * hääliKokku }
    .keys
}

private fun trükiHääletanuteArv() {
  val valimistestOsavõtjaid = hääletanud.values.sum()
  println("valimistest osavõtnuid: $valimistestOsavõtjaid")
}

private fun loeKandidaatideAndmed(): MutableList<Kandidaat> {
  var andmelugeja = AndmeFailideLugeja()
  return andmelugeja.loeKandidaadikirjed().toMutableList()
}

private fun loeReitingud(): MutableList<Reiting> {
  var andmelugeja = AndmeFailideLugeja()
  return andmelugeja.loeJSONiFailistReitingud()
}






