import andmed.AndmeFailideLugeja
import andmed.MANDAATE_KOKKU
import andmed.erakonnad
import andmed.hääletanud
import andmed.valimisringkonnad
import mudel.Kandidaat

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// andmestruktuur, mis seostab erakonna nimega tema häälte arvu igas ringkonnas
val erakondadeHääledRingkondadeKaupa = mutableMapOf<String, MutableMap<Int, Int>>()

fun main() {
  trükiHääletanuteArv()

  val kandidaadid = loeKandidaatideAndmed()

  // leia igale erakonnale igas ringkonnas antud häälte arv
  leiaErakondadeHääledRingkonniti(kandidaadid)

  val erakondadeRingkonnamandaadidRingkondadeKaupa : Map<Int, Map<String, Int>>
      = arvutaRingkonnaMandaadid()

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

  // arvuta iga erakonna jaoks ringkondade kaupa võidetud ringkonnamandaatide summa
  val erakondadeMandaatideSummaKoguRiigis = leiaSummaÜleRingkondade(erakondadeRingkonnamandaadidRingkondadeKaupa)

  // leia ülejäänud mandaadid
  val kompensatsiooniMandaatideArv = MANDAATE_KOKKU - jagatudRingkonnamandaatideArv


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

  trükiMandaatideSummad(erakondadeMandaatideSummaKoguRiigis, erakondadeKompensatsioonimandaatideArv)
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
    // leiame iga erakonna ringkonnamandaatide arvu ja vastavalt sellele trükkime välja kandidaadid
    for ((erakond, hääled) in erakondadeRingkonnamandaadidRingkondadeKaupa[ringkond.nr]!!) {
      // arvestades ainult vaadeldava erakonna kandidaate, trüki välja esimesed hääled.size() kandidaati
      println("  $erakond ${kandidaadidRingkonnas.filter { it.erakond == erakond }.take(hääled)}")
      ringkonnamandaadid.addAll(kandidaadidRingkonnas.filter { it.erakond == erakond }.take(hääled))
      // eemalda loendist kandidaadid need hääled.size() kandidaati, mis just välja trükkisime
      kandidaadid.removeAll(kandidaadidRingkonnas.filter { it.erakond == erakond }.take(hääled))
    }
  }
  return ringkonnamandaadid
}

private fun leiaSummaÜleRingkondade(erakondadeRingkonnamandaadidRingkondadeKaupa: Map<Int, Map<String, Int>>): MutableMap<String, Int> {
  val erakondadeMandaatideSummaKoguRiigis = mutableMapOf<String, Int>()
  for ((ringkond, kanded) in erakondadeRingkonnamandaadidRingkondadeKaupa) {
    for ((erakond, mandaadid) in kanded) {
      if (erakondadeMandaatideSummaKoguRiigis[erakond] == null)
        erakondadeMandaatideSummaKoguRiigis[erakond] = 0 // kui erakonnal veel ei ole mandaate, siis lisa 0
      erakondadeMandaatideSummaKoguRiigis[erakond] = erakondadeMandaatideSummaKoguRiigis[erakond]!! + mandaadid
    }
  }
  return erakondadeMandaatideSummaKoguRiigis
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
  file.writeText(k6ikMandaadid.joinToString("\n"))
  // ensure the file ends with EOF
  file.appendText("\n")
  // ensure the file is closed
}

private fun trükiMandaatideSummad(
  erakondadeMandaatideSummaKoguRiigis: MutableMap<String, Int>,
  erakondadeKompensatsioonimandaatideArv: MutableMap<String, Int>
) {
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
}

private fun leiaErakondadeHääledRingkonniti(kandidaadid: MutableList<Kandidaat>) {
  erakonnad.map { erakond ->
    valimisringkonnad.map { ringkond ->
      val hääli = arvutaHäälteArv(hääletanud[ringkond.nr]!!, erakond.toetusprotsentValimisringkonnas[ringkond.nr]!!)
      var currentMap = erakondadeHääledRingkondadeKaupa[erakond.nimetus]
      if (currentMap == null) {
        currentMap = mutableMapOf()
        erakondadeHääledRingkondadeKaupa[erakond.nimetus] = currentMap
      }
      currentMap[ringkond.nr] = hääli

      // jaota hääled erakonna selle ringkonna kandidaatide vahel juhuslikult
      val kandidaadidRingkonnas =
        kandidaadid.filter { it.erakond == erakond.nimetus && it.valimisringkond == ringkond.nr }
      val kandidaatideHääled = jagadaArvNiksJuhuslikuksOsaks(hääli, kandidaadidRingkonnas.size)
      for (i in kandidaadidRingkonnas.indices) {
        kandidaadidRingkonnas[i].hääli = kandidaatideHääled[i]
      }
      println(kandidaadidRingkonnas)


      hääli
    }
  }
}

private fun trükiHääletanuteArv() {
  val valimistestOsavõtjaid = hääletanud.values.sum()
  println("valimistest osavõtnuid: $valimistestOsavõtjaid")
}

private fun loeKandidaatideAndmed(): MutableList<Kandidaat> {
  var andmelugeja = AndmeFailideLugeja()
  return andmelugeja.loeKandidaadikirjed().toMutableList()
}






