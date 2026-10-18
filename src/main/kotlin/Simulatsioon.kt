import andmed.AndmeFailideLugeja
import andmed.erakonnad
import andmed.hääletanud
import andmed.valimisringkonnad
import mudel.Kandidaat
import kotlin.random.Random

/** Küsitleja, kelle reitingute põhjal hääled simuleeritakse. */
enum class Küsitleja {
  /** reitingud loetakse reitingud.ee JSON-failist */
  NORSTAT,
  /** reitingud on kirjas failis Andmed.kt */
  EMOR,
}

/** erakonna nimi -> ringkonna number -> toetusprotsent */
fun loeToetusprotsendid(küsitleja: Küsitleja): Map<String, Map<Int, Double>> = when (küsitleja) {
  Küsitleja.EMOR -> erakonnad.associate { it.nimetus to it.toetusprotsentValimisringkonnas }
  Küsitleja.NORSTAT -> {
    val reitingud = AndmeFailideLugeja().loeJSONiFailistReitingud()
    erakonnad.associate { erakond ->
      erakond.nimetus to reitingud.filter { it.erakonnaNimi == erakond.nimetus }.associate { it.ringkond to it.reiting }
    }
  }
}

/** Määrab kandidaatidele juhusliku häälte arvu.
 * <p>
 *   Erakonna hääled ringkonnas arvutatakse toetusprotsendi ja hääletanute arvu põhjal ning jagatakse
 *   juhuslikult erakonna selle ringkonna kandidaatide vahel. Erakondadele, kelle toetusprotsenti ei
 *   ole antud, ja üksikkandidaatidele hääli ei jagata.
 * </p>
 */
fun simuleeriHääled(kandidaadid: List<Kandidaat>, toetusprotsendid: Map<String, Map<Int, Double>>, juhus: Random) {
  for (erakond in erakonnad) {
    for (ringkond in valimisringkonnad) {
      val hääli = arvutaHäälteArv(hääletanud[ringkond.nr]!!, toetusprotsendid[erakond.nimetus]!![ringkond.nr]!!)
      val kandidaadidRingkonnas =
        kandidaadid.filter { it.erakond == erakond.nimetus && it.valimisringkond == ringkond.nr }
      val kandidaatideHääled = jagadaArvNiksJuhuslikuksOsaks(hääli, kandidaadidRingkonnas.size, juhus)
      for (i in kandidaadidRingkonnas.indices) {
        kandidaadidRingkonnas[i].hääli = kandidaatideHääled[i]
      }
    }
  }
}

/** Simuleerib ühed valimised: loeb kandidaadid, simuleerib hääled ja jagab mandaadid. */
fun simuleeriValimised(küsitleja: Küsitleja, juhus: Random): Mandaadijaotus {
  val kandidaadid = AndmeFailideLugeja().loeKandidaadikirjed()
  simuleeriHääled(kandidaadid, loeToetusprotsendid(küsitleja), juhus)
  return jaotaMandaadid(kandidaadid, valimisringkonnad.toList(), hääletanud)
}
