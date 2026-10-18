package andmed

import Küsitluseviga
import üleriigilineToetus
import koond.ERAKONNA_KOODID
import koond.Koondmudel
import koond.hinnang
import koond.loeKoondmudel
import koond.loeValimised
import koond.valiKüsitlused
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Kirjutab veebilehe jaoks küsitluste toetusprotsendid ja küsitluse vea kompaktsesse JSON-faili. Norstati
 * failid on reitingud.ee lehe täielikud andmed (umbes 2 MB igaüks); veebileht vajab neist ainult 12 × 8 arvu.
 * Erakonnad on andmeallika järjekorras, et sama seemne korral annaks veebileht sama tulemuse kui
 * käsurea programm.
 *
 * Käivitamine: ./gradlew veebiandmed
 */
fun main(args: Array<String>) {
  require(args.size == 1) { "Kasutamine: VeebiandmedKt <väljundkaust>" }
  val kaust = File(args[0]).apply { mkdirs() }
  File(kaust, "toetused.json").writeText(veebiandmed(AndmeFailideLugeja()))
}

/** 2023. aasta valimiste kuupäev; küsitluse vea horisont on päevad küsitluse kuupäevast valimisteni */
val VALIMISED_2023: LocalDate = LocalDate.parse("2023-03-05")

/**
 * Veebilehe küsitlus: tunnus, küsitleja, kuupäev (välitöö lõpp; Emori märtsi küsitluse täpset kuupäeva
 * andmetes pole, seega on see ligikaudne) ja reitingufail (null: Emori andmed failist Andmed.kt).
 */
data class VeebiKüsitlus(val tunnus: String, val küsitleja: String, val kuupäev: LocalDate, val fail: String?)

val KÜSITLUSED = listOf(
  VeebiKüsitlus("norstat-2023-02-20", "Norstat", LocalDate.parse("2023-02-20"), "reitingud/reitingud-2023-02-20.json"),
  VeebiKüsitlus("norstat-2023-02-27", "Norstat", LocalDate.parse("2023-02-27"), "reitingud/reitingud-2023-02-27.json"),
  VeebiKüsitlus("norstat-2023-03-03", "Norstat", LocalDate.parse("2023-03-03"), "reitingud/reitingud-2023-03-03.json"),
  VeebiKüsitlus("emor-2023-03", "Kantar Emor", LocalDate.parse("2023-03-02"), null),
)

/** koondi kuupäev ja Norstati fail, mille ringkondade profiili koond kasutab (välitöö lõppes enne T − 3 päeva) */
val KOONDI_KUUPÄEV: LocalDate = LocalDate.parse("2023-03-03")
const val KOONDI_PROFIIL = "reitingud/reitingud-2023-02-27.json"

fun horisont(kuupäev: LocalDate): Long = ChronoUnit.DAYS.between(kuupäev, VALIMISED_2023)

/** Koondmudeli parameetrid failist kusitlused/koondmudel.json (loodud käsuga ./gradlew tagasitest). */
fun loeKoondmudel(lugeja: AndmeFailideLugeja): Koondmudel = loeKoondmudel(lugeja.loeRessurss("kusitlused/koondmudel.json"))

/**
 * Küsitluste koond kuupäeval [KOONDI_KUUPÄEV]: üleriigilised osakaalud koondmudelist, ringkondade profiil
 * Norstati küsitlusest. Iga erakonna ringkondade toetus korrutatakse sama teguriga, et üleriigiline toetus
 * oleks koondi oma; seejärel skaleeritakse ringkondade summad Norstati ringkondade summadega võrdeliseks.
 * Tagastab toetuse ja küsitlejad, kelle küsitlused koondis on.
 */
fun koond2023(lugeja: AndmeFailideLugeja, mudel: Koondmudel): Pair<Map<String, Map<Int, Double>>, List<String>> {
  val v2023 = loeValimised(lugeja).single { it.aasta == 2023 }
  val parameetrid = mudel.parameetrid(horisont(KOONDI_KUUPÄEV))
  val h = hinnang(v2023.küsitlused, KOONDI_KUUPÄEV, mudel.meetod, parameetrid)
    ?: throw IllegalStateException("Koondi jaoks pole küsitlusi")
  val küsitlejad = valiKüsitlused(v2023.küsitlused, KOONDI_KUUPÄEV).map { it.küsitleja }.distinct().sorted()

  val profiil = reitingudToetusprotsentidena(loeReitingud(lugeja.loeRessurss(KOONDI_PROFIIL)))
  val kehtivad = kehtivaidHääli
  val sihid = ERAKONNA_KOODID.mapNotNull { (kood, nimi) ->
    if (kood in h.logKeskmine && nimi in profiil) nimi to h.osakaal(kood) else null
  }.toMap()
  val skaleeritud = sihid.mapValues { (nimi, siht) ->
    val praegune = üleriigilineToetus(profiil[nimi]!!, kehtivad)
    profiil[nimi]!!.mapValues { (_, p) -> if (praegune > 0) p * siht / praegune else 0.0 }
  }
  val sihtideSumma = sihid.values.sum()
  val profiiliSumma = sihid.keys.sumOf { üleriigilineToetus(profiil[it]!!, kehtivad) }
  val toetus = skaleeritud.mapValues { (_, ringkonnad) ->
    ringkonnad.mapValues { (r, p) ->
      val norstatiSumma = sihid.keys.sumOf { profiil[it]!![r] ?: 0.0 }
      val uusSumma = skaleeritud.values.sumOf { it[r] ?: 0.0 }
      if (uusSumma > 0) p * (norstatiSumma * sihtideSumma / profiiliSumma) / uusSumma else 0.0
    }
  }
  return toetus to küsitlejad
}

private fun json(tunnus: String, küsitleja: String, kuupäev: LocalDate, toetus: Map<String, Map<Int, Double>>, viga: Küsitluseviga): String {
  val erakonnad = toetus.keys.toList()
  val protsendid = erakonnad.joinToString(",\n      ") { erakond ->
    valimisringkonnad.joinToString(", ", "[", "]") { (toetus[erakond]!![it.nr] ?: 0.0).toString() }
  }
  return """  "$tunnus": {
    "küsitleja": "$küsitleja",
    "kuupäev": "$kuupäev",
    "viga": [${viga.üleriigiline}, ${viga.ringkondlik}],
    "erakonnad": [${erakonnad.joinToString(", ") { "\"$it\"" }}],
    "protsendid": [
      $protsendid
    ]
  }"""
}

fun veebiandmed(lugeja: AndmeFailideLugeja): String {
  val mudel = loeKoondmudel(lugeja)
  val osad = KÜSITLUSED.map { k ->
    val toetus = if (k.fail != null) reitingudToetusprotsentidena(loeReitingud(lugeja.loeRessurss(k.fail)))
      else erakonnad.associate { it.nimetus to it.toetusprotsentValimisringkonnas }
    json(k.tunnus, k.küsitleja, k.kuupäev, toetus, mudel.viga(listOf(k.küsitleja), horisont(k.kuupäev)))
  }.toMutableList()
  val (koond, küsitlejad) = koond2023(lugeja, mudel)
  osad.add(json("koond-2023-03-03", küsitlejad.joinToString(" + "), KOONDI_KUUPÄEV, koond, mudel.viga(küsitlejad, horisont(KOONDI_KUUPÄEV))))
  return "{\n" + osad.joinToString(",\n") + "\n}\n"
}
