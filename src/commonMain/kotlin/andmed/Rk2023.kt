package andmed

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import mudel.Kandidaat

/** 2023. aasta Riigikogu valimiste ametliku tulemuse allikas */
const val RK2023_ALLIKAS = "https://opendata.valimised.ee/api/RK_2023/RESULTS.xml"

/** 2023. aasta tulemuse fail ressursside kaustas; loodud käsuga ./gradlew ekspordiRk2023 */
const val RK2023_TULEMUSE_FAIL = "rk2023/tulemus.json"

/** Kandidaadi ametlik tulemus 2023. aasta valimistel. */
data class Rk2023Kandidaat(
  val regNr: Int,
  val nimi: String,
  val erakond: String,
  val ringkond: Int,
  val hääli: Int,
  /** valituks osutunud kandidaadi mandaadi liik: PERSONAL, DISTRICT või COMPENSATION; muidu null */
  val mandaat: String?,
)

/** 2023. aasta valimiste ametlik tulemus. */
data class Rk2023Tulemus(
  val kehtivaidHääli: Map<Int, Int>,
  val kandidaadid: List<Rk2023Kandidaat>,
)

/** Loeb faili [RK2023_TULEMUSE_FAIL]. */
fun loeRk2023Tulemus(json: String): Rk2023Tulemus {
  val juur = Json.parseToJsonElement(json).jsonObject
  val kehtivaidHääli = juur["kehtivaidHääli"]!!.jsonObject.entries
    .associate { (ringkond, hääli) -> ringkond.toInt() to hääli.jsonPrimitive.int }
  val kandidaadid = juur["kandidaadid"]!!.jsonArray.map { element ->
    val k = element.jsonObject
    Rk2023Kandidaat(
      regNr = k["regNr"]!!.jsonPrimitive.int,
      nimi = k["nimi"]!!.jsonPrimitive.content,
      erakond = k["erakond"]!!.jsonPrimitive.content,
      ringkond = k["ringkond"]!!.jsonPrimitive.int,
      hääli = k["hääli"]!!.jsonPrimitive.int,
      mandaat = k["mandaat"]!!.jsonPrimitive.contentOrNull,
    )
  }
  return Rk2023Tulemus(kehtivaidHääli, kandidaadid)
}

/**
 * Tagastab kandidaatide koopiad, millel on 2023. aasta valimiste tegelik häälte arv.
 * Kandidaadid seotakse registreerimisnumbri järgi; nimi, erakond ja ringkond peavad kattuma.
 */
fun kandidaadidTegelikeHäältega(kandidaadid: List<Kandidaat>, tulemus: Rk2023Tulemus): List<Kandidaat> {
  val tulemusedRegNrJärgi = tulemus.kandidaadid.associateBy { it.regNr }
  require(tulemusedRegNrJärgi.size == kandidaadid.size) {
    "Kandidaatide arv (${kandidaadid.size}) ja tulemuste arv (${tulemusedRegNrJärgi.size}) erinevad"
  }
  return kandidaadid.map { k ->
    val t = tulemusedRegNrJärgi[k.regNr] ?: throw IllegalArgumentException("Kandidaadi ${k.nimi} (${k.regNr}) tulemus puudub")
    require(t.nimi == k.nimi && t.erakond == k.erakond && t.ringkond == k.valimisringkond) {
      "Kandidaadi ${k.regNr} andmed erinevad: $k vs $t"
    }
    k.copy(hääli = t.hääli)
  }
}

/** Erakondade toetusprotsendid ringkondades 2023. aasta tegelike häälte järgi (üksikkandidaatideta). */
fun rk2023Toetusprotsendid(tulemus: Rk2023Tulemus): Map<String, Map<Int, Double>> =
  tulemus.kandidaadid.filter { it.erakond != ÜKSIKKANDIDAADID }
    .groupBy { it.erakond }
    .mapValues { (_, erakonnaKandidaadid) ->
      tulemus.kehtivaidHääli.mapValues { (ringkond, kehtivaid) ->
        erakonnaKandidaadid.filter { it.ringkond == ringkond }.sumOf { it.hääli } * 100.0 / kehtivaid
      }
    }
