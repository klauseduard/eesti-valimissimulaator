import andmed.AndmeFailideLugeja
import andmed.erakonnad
import andmed.kehtivaidHääli
import andmed.reitingudToetusprotsentidena
import andmed.valimisringkonnad
import kotlin.random.Random

/** Küsitleja, kelle reitingute põhjal hääled simuleeritakse. */
enum class Küsitleja {
  /** reitingud loetakse reitingud.ee JSON-failist */
  NORSTAT,
  /** reitingud on kirjas failis Andmed.kt */
  EMOR,
}

/** erakonna nimi -> ringkonna number -> toetusprotsent
 *
 * @param reitingufail - Norstati reitingute JSON-fail; null tähendab programmiga kaasas olevat viimast faili
 */
fun loeToetusprotsendid(küsitleja: Küsitleja, reitingufail: String? = null): Map<String, Map<Int, Double>> =
  when (küsitleja) {
    Küsitleja.EMOR -> erakonnad.associate { it.nimetus to it.toetusprotsentValimisringkonnas }
    Küsitleja.NORSTAT -> reitingudToetusprotsentidena(AndmeFailideLugeja().loeJSONiFailistReitingud(reitingufail))
  }

/** Simuleerib ühed valimised: loeb kandidaadid, simuleerib hääled ja jagab mandaadid. */
fun simuleeriValimised(küsitleja: Küsitleja, juhus: Random, reitingufail: String? = null): Mandaadijaotus {
  val kandidaadid = AndmeFailideLugeja().loeKandidaadikirjed()
  simuleeriHääled(kandidaadid, loeToetusprotsendid(küsitleja, reitingufail), juhus)
  return jaotaMandaadid(kandidaadid, valimisringkonnad.toList(), kehtivaidHääli)
}

/** Kordab simulatsiooni [kordi] korda; sisendandmed loetakse üks kord. */
fun simuleeriKorduvalt(
  küsitleja: Küsitleja,
  kordi: Int,
  juhus: Random,
  reitingufail: String? = null,
  viga: Küsitluseviga? = null,
): Kokkuvõte =
  simuleeriKorduvalt(
    AndmeFailideLugeja().loeKandidaadikirjed(), loeToetusprotsendid(küsitleja, reitingufail), kordi, juhus, viga = viga
  )
