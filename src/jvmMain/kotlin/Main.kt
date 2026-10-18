import andmed.VAIKIMISI_REITINGUFAIL
import andmed.kehtivaidHääli
import andmed.valimisringkonnad

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  val seaded = try {
    loeSeaded(args)
  } catch (e: IllegalArgumentException) {
    System.err.println(e.message)
    System.err.println(KASUTUSJUHEND)
    exitProcess(2)
  }

  // --seed=N teeb häälte juhusliku jaotuse korratavaks
  val juhus = if (seaded.seeme != null) Random(seaded.seeme) else Random

  // kaust, kuhu tulemused kirjutatakse
  File("runs").mkdirs()

  if (seaded.kordi != null) {
    simuleeriJaKirjutaSagedused(seaded, seaded.kordi, juhus)
  } else {
    simuleeriÜksKord(seaded, juhus)
  }
}

private fun simuleeriÜksKord(seaded: Seaded, juhus: Random) {
  trükiKehtivateHäälteArv()

  val jaotus = simuleeriValimised(seaded.küsitleja, juhus, seaded.reitingufail)

  trükiRingkonnamandaadid(jaotus)

  println("\nRINGKONNAMANDAADID: \n")
  jaotus.ringkonnamandaadid.sortedWith(compareBy({it.hääli})).reversed().forEach { println(it) }

  println("\nKOMPENSATSIOONIMANDAADID: \n")
  jaotus.kompensatsioonimandaadid.forEach { println(it) }

  println("\n\nERAKONDADE KAUPA:\n ")
  jaotus.kõikMandaadid().forEach({ println(it) })

  kirjutaJagatudMandaadidFaili(jaotus)

  trükiMandaatideSummad(jaotus)
}

private fun simuleeriJaKirjutaSagedused(seaded: Seaded, kordi: Int, juhus: Random) {
  val viga = if (seaded.arvestaViga) Küsitluseviga.KALIBREERITUD else null
  val kokkuvõte = simuleeriKorduvalt(seaded.küsitleja, kordi, juhus, seaded.reitingufail, viga)

  val andmed = when (seaded.küsitleja) {
    Küsitleja.NORSTAT -> "Norstat, reitingufail ${seaded.reitingufail ?: VAIKIMISI_REITINGUFAIL}"
    Küsitleja.EMOR -> "Emor, andmed failist Andmed.kt"
  }
  val päis = mutableListOf(
    "Kandidaatide valituks osutumise sagedused",
    "",
    "andmed: $andmed",
    "kordusi: ${kokkuvõte.kordi}, seeme: ${seaded.seeme ?: "juhuslik"}",
    if (viga != null) "küsitluse viga: üleriigiline ${viga.üleriigiline}, ringkondlik ${viga.ringkondlik} (2023. aasta küsitluste järgi)"
    else "küsitluse viga: arvestamata",
    "",
    "erakondade mandaadid (vähim–suurim, keskmine):",
  )
  for ((erakond, mandaadid) in kokkuvõte.erakondadeMandaadid.entries.sortedByDescending { it.value.average() }) {
    päis.add("  $erakond: ${mandaadid.min()}–${mandaadid.max()}, keskmiselt ${"%.1f".format(Locale.ROOT, mandaadid.average())}")
  }

  val read = kokkuvõte.valitudKordi.entries
    .sortedWith(compareBy({ -it.value }, { it.key.nimi }))
    .map { (k, kordi) -> "%7d Kandidaat(nimi=%s, erakond=%s, valimisringkond=%d".format(kordi, k.nimi, k.erakond, k.valimisringkond) }

  val sisu = päis.joinToString("\n") { "# $it".trimEnd() } + "\n#\n" + "#".repeat(47) + "\n" + read.joinToString("\n") + "\n"
  val failinimi = "runs/" + ajatempel() + "-sagedused.txt"
  File(failinimi).writeText(sisu)

  päis.drop(6).forEach { println(it) }
  println("\nKandidaatide valituks osutumise sagedused on failis $failinimi")
}

private fun trükiRingkonnamandaadid(jaotus: Mandaadijaotus) {
  for (ringkond in valimisringkonnad) {
    println("Ringkond ${ringkond.nr} (${ringkond.nimetus})")
    for ((erakond, mandaate) in jaotus.ringkonnamandaatideArv[ringkond.nr]!!.filterValues { it > 0 }) {
      val valitud = jaotus.ringkonnamandaadid.filter { it.valimisringkond == ringkond.nr && it.erakond == erakond }
      println("  $erakond: $mandaate $valitud")
    }
  }
}

private fun ajatempel(): String = SimpleDateFormat("yyyyMMddHHmmss").format(Date())

private fun kirjutaJagatudMandaadidFaili(jaotus: Mandaadijaotus) {
  val filename = "runs/" + ajatempel() + "-mandaadid.txt"
  File(filename).writeText(mandaadidTekstina(jaotus))
}

private fun trükiMandaatideSummad(jaotus: Mandaadijaotus) {
  val ringkonnamandaadid = jaotus.ringkonnamandaadid.groupingBy { it.erakond }.eachCount()
  val kompensatsioonimandaadid = jaotus.kompensatsioonimandaadid.groupingBy { it.erakond }.eachCount()
  println("RINGKONNAMANDAATIDE JAOTUS: $ringkonnamandaadid")
  println("KOMPENSATSIOONIMANDAATIDE JAOTUS: $kompensatsioonimandaadid")

  // arvuta kompensatsioonimandaatide ja ringkonnamandaatide summa erakondade kaupa
  val erakondadeMandaatideSumma = jaotus.kõikMandaadid().groupingBy { it.erakond }.eachCount()
  println("ERAKONDADE MANDAATIDE KOGUSUMMA: $erakondadeMandaatideSumma")
}

private fun trükiKehtivateHäälteArv() {
  println("kehtivaid hääli (2023. aasta valimiste järgi): ${kehtivaidHääli.values.sum()}")
}
