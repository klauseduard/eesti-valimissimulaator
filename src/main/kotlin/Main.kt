import andmed.erakonnad
import andmed.hääletanud
import andmed.valimisringkonnad

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

fun main(args: Array<String>) {

  // --seed=N teeb häälte juhusliku jaotuse korratavaks
  val seemneArgument = args.find { it.startsWith("--seed=") }
  val juhus = if (seemneArgument != null) Random(seemneArgument.removePrefix("--seed=").toLong()) else Random

  // kui programmi käsureaargumendina on antud --pollster=emor, kasutame Emori andmeid failist Andmed.kt,
  // vaikimisi Norstati andmeid reitingufailist
  val küsitleja = if (args.contains("--pollster=emor")) Küsitleja.EMOR else Küsitleja.NORSTAT
  if (küsitleja == Küsitleja.NORSTAT) {
    // trüki instruktsioonid, kuidas kasutada Emori andmeid
    println("Kasutamiseks vali Emori andmed: --pollster=emor")
  }

  trükiHääletanuteArv()

  val jaotus = simuleeriValimised(küsitleja, juhus)

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

private fun trükiRingkonnamandaadid(jaotus: Mandaadijaotus) {
  for (ringkond in valimisringkonnad) {
    println("Ringkond ${ringkond.nr} (${ringkond.nimetus})")
    for ((erakond, mandaate) in jaotus.ringkonnamandaatideArv[ringkond.nr]!!.filterValues { it > 0 }) {
      val valitud = jaotus.ringkonnamandaadid.filter { it.valimisringkond == ringkond.nr && it.erakond == erakond }
      println("  $erakond: $mandaate $valitud")
    }
  }
}

private fun kirjutaJagatudMandaadidFaili(jaotus: Mandaadijaotus) {
  val timestamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
  val filename = "runs/" + timestamp + "-mandaadid.txt"
  File(filename).writeText(mandaadidTekstina(jaotus))
}

private fun trükiMandaatideSummad(jaotus: Mandaadijaotus) {
  val ringkonnamandaadid = jaotus.ringkonnamandaadid.groupingBy { it.erakond }.eachCount()
  val kompensatsioonimandaadid = jaotus.kompensatsioonimandaadid.groupingBy { it.erakond }.eachCount()
  println("RINGKONNAMANDAATIDE JAOTUS: $ringkonnamandaadid")
  println("KOMPENSATSIOONIMANDAATIDE JAOTUS: $kompensatsioonimandaadid")

  // arvuta kompensatsioonimandaatide ja ringkonnamandaatide summa erakondade kaupa
  val erakondadeMandaatideSumma = erakonnad.associate {
    it.nimetus to (ringkonnamandaadid[it.nimetus] ?: 0) + (kompensatsioonimandaadid[it.nimetus] ?: 0)
  }
  println("ERAKONDADE MANDAATIDE KOGUSUMMA: $erakondadeMandaatideSumma")
}

private fun trükiHääletanuteArv() {
  val valimistestOsavõtjaid = hääletanud.values.sum()
  println("valimistest osavõtnuid: $valimistestOsavõtjaid")
}
