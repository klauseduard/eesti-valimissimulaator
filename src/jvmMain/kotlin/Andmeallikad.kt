import andmed.AndmeFailideLugeja
import andmed.KÜSITLUSED
import andmed.VALIMISED_2023
import andmed.VAIKIMISI_REITINGUFAIL
import andmed.horisont
import andmed.loeKoondmudel
import java.time.LocalDate
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

/**
 * Küsitluse viga käsurea korduvate simulatsioonide jaoks: küsitleja majaefekt ja ühine viga koondmudeli
 * tagasitestist, horisondiga reitingufaili nimes olevast kuupäevast valimisteni (Emori puhul ligikaudne
 * kuupäev, vt andmed/Veebiandmed.kt). Kui kuupäeva ei leia, on horisont 3 päeva.
 */
fun küsitluseViga(küsitleja: Küsitleja, reitingufail: String? = null): Küsitluseviga {
  val nimi = if (küsitleja == Küsitleja.EMOR) "Kantar Emor" else "Norstat"
  val kuupäev = when (küsitleja) {
    Küsitleja.EMOR -> KÜSITLUSED.single { it.küsitleja == "Kantar Emor" }.kuupäev
    Küsitleja.NORSTAT -> Regex("""(\d{4}-\d{2}-\d{2})""").find(reitingufail ?: VAIKIMISI_REITINGUFAIL)
      ?.value?.let(LocalDate::parse) ?: VALIMISED_2023.minusDays(3)
  }
  return loeKoondmudel(AndmeFailideLugeja()).viga(listOf(nimi), horisont(kuupäev).coerceAtLeast(0))
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
