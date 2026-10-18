package andmed

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mudel.Kandidaat

/*
 * Andmefailide lugejad. Need saavad sisendiks faili teksti, et sama koodi saaks kasutada nii
 * käsurea programm (failid loetakse JVM-i ressurssidest) kui ka veebileht (failid laaditakse võrgust).
 */

/** programmiga kaasas olev viimane reitingufail */
const val VAIKIMISI_REITINGUFAIL = "reitingud/reitingud-2023-03-03.json"

/** üleriigiliste nimekirjade fail */
const val ÜLERIIGILISTE_NIMEKIRJADE_FAIL = "yleriigiline.txt"

/** ringkonna kandidaatide faili nimi */
fun ringkonnaKandidaatideFail(ringkond: Int): String = "ringkond$ringkond.txt"

/**
 * Loeb ühe ringkonna kandidaadid. Failid on salvestatud Riigikogu valimiste veebilehelt
 * https://rk2023.valimised.ee/et/candidates ja andmed neis on esitatud järgmisel kujul:
 *
 * erakonna nimi
 * Reg nr 1 Nimi Nimi
 * Reg nr 2 Nimi Nimi
 *
 * erakonna nimi
 * Reg nr 3 Nimi Nimi
 *
 * jne.
 */
fun loeRingkonnaKandidaadid(tekst: String, ringkond: Int): List<Kandidaat> {
  val kandidaadid = mutableListOf<Kandidaat>()
  var erakond: String? = null

  for (rida in tekst.lines()) {
    when {
      rida.startsWith("Reg nr") -> {
        val number = rida.substring(7).split(" ").first()
        val nimi = rida.substring(7 + number.length).trim()
        val kandidaadiErakond = erakond ?: throw IllegalArgumentException("Kandidaadil $nimi puudub erakond")
        kandidaadid.add(Kandidaat(nimi, kandidaadiErakond, ringkond, regNr = number.toInt()))
      }
      rida.startsWith("Kandidaate - ") -> {
        // ignoreeri seda rida
      }
      rida.isBlank() -> {
        // ignoreeri seda rida
      }
      else -> {
        // loe erakonna nimi
        erakond = rida.trim()
      }
    }
  }
  return kandidaadid
}

/**
 * Määrab kandidaatidele järjekorranumbri erakonna üleriigilises nimekirjas.
 * Failis on iga erakonna nimekirja ees rida "#erakonna nimi", seejärel kandidaatide nimed järjekorras.
 */
fun määraÜleriigilisedJärjekorranumbrid(kandidaadid: List<Kandidaat>, tekst: String) {
  var erakond: String? = null
  var i = 0

  for (rida in tekst.lines()) {
    when {
      rida.isBlank() -> {
        // ignoreeri seda rida
      }
      rida.startsWith("#") -> {
        erakond = rida.substring(1).trim()
        i = 1
      }
      else -> {
        val nimi = rida.trim()
        if (erakond == null) {
          throw IllegalArgumentException("Kandidaadil $nimi puudub erakond")
        }
        val kandidaat = kandidaadid.find { it.nimi == nimi && it.erakond == erakond }
          ?: throw IllegalArgumentException("Üleriigilise nimekirja kandidaati $nimi ($erakond) ei ole ringkondades")
        kandidaat.jrkNr = i++
      }
    }
  }
}

/**
 * Loeb kõigi ringkondade kandidaadid ja määrab nende järjekorra üleriigilises nimekirjas.
 *
 * @param ringkondadeTekstid - ringkonna number -> ringkonna kandidaatide faili tekst
 * @param üleriigilisteNimekirjadeTekst - üleriigiliste nimekirjade faili tekst
 */
fun loeKandidaadid(ringkondadeTekstid: Map<Int, String>, üleriigilisteNimekirjadeTekst: String): List<Kandidaat> {
  val kandidaadid = ringkondadeTekstid.entries.sortedBy { it.key }
    .flatMap { (ringkond, tekst) -> loeRingkonnaKandidaadid(tekst, ringkond) }
  määraÜleriigilisedJärjekorranumbrid(kandidaadid, üleriigilisteNimekirjadeTekst)
  return kandidaadid
}

/** Erakonna toetusprotsent ühes ringkonnas. */
data class Reiting(val erakonnaNimi: String, val reiting: Double, val ringkond: Int)

/** erakondade järjekord reitingud.ee failides; failis endas erakondade nimesid ei ole */
val REITINGUFAILI_ERAKONNAD = listOf(
  KESKERAKOND,
  KONSERVATIIVID,
  REFORMIERAKOND,
  ISAMAA_ERAKOND,
  SDE,
  ROHELISED,
  E200,
  PAREMPOOLSED,
)

/**
 * Loeb reitingud.ee JSON-failist erakondade toetusprotsendid ringkondades.
 *
 * 1. ringkonna andmed on elemendis .props.chartData.data[16][-1], kus -1 osutab tagantpoolt esimesele
 * elemendile, 2. ringkonna andmed elemendis .props.chartData.data[17][-1] jne kuni 12. ringkonnani
 * elemendis .props.chartData.data[27][-1]. Iga ringkonna andmed on kujul
 * ["24.01-20.02.2023", "14.8%", "32.5%", ...], kus esimene element on kuupäevavahemik ja järgmised
 * kaheksa on erakondade reitingud protsentides järjekorras [REITINGUFAILI_ERAKONNAD].
 * Tühi väärtus tähendab reitingut 0.
 */
fun loeReitingud(json: String): List<Reiting> {
  val andmed = Json.parseToJsonElement(json).jsonObject["props"]!!.jsonObject["chartData"]!!
    .jsonObject["data"]!!.jsonArray

  val reitingud = mutableListOf<Reiting>()
  for (i in 0..11) {
    val ringkonnaReitingud = andmed[i + 16].jsonArray.last().jsonArray.map { it.jsonPrimitive.content }
    for ((j, erakonnaNimi) in REITINGUFAILI_ERAKONNAD.withIndex()) {
      val väärtus = ringkonnaReitingud[j + 1]
      val reiting = if (väärtus.isEmpty()) 0.0 else väärtus.removeSuffix("%").toDouble()
      reitingud.add(Reiting(erakonnaNimi, reiting, i + 1))
    }
  }
  return reitingud
}

/** Reitingud kujul erakonna nimi -> ringkonna number -> toetusprotsent. */
fun reitingudToetusprotsentidena(reitingud: List<Reiting>): Map<String, Map<Int, Double>> =
  reitingud.groupBy { it.erakonnaNimi }
    .mapValues { (_, erakonnaReitingud) -> erakonnaReitingud.associate { it.ringkond to it.reiting } }
