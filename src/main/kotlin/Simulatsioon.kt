import andmed.AndmeFailideLugeja
import andmed.erakonnad
import andmed.kehtivaidHääli
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

/** erakonna nimi -> ringkonna number -> toetusprotsent
 *
 * @param reitingufail - Norstati reitingute JSON-fail; null tähendab programmiga kaasas olevat viimast faili
 */
fun loeToetusprotsendid(küsitleja: Küsitleja, reitingufail: String? = null): Map<String, Map<Int, Double>> =
  when (küsitleja) {
    Küsitleja.EMOR -> erakonnad.associate { it.nimetus to it.toetusprotsentValimisringkonnas }
    Küsitleja.NORSTAT -> AndmeFailideLugeja().loeJSONiFailistReitingud(reitingufail)
      .groupBy { it.erakonnaNimi }
      .mapValues { (_, reitingud) -> reitingud.associate { it.ringkond to it.reiting } }
  }

/** Määrab kandidaatidele juhusliku häälte arvu.
 * <p>
 *   Erakonna hääled ringkonnas arvutatakse toetusprotsendi ja ringkonna kehtivate häälte arvu põhjal
 *   ning jagatakse juhuslikult erakonna selle ringkonna kandidaatide vahel: kandidaatide failis esimene
 *   kandidaat saab juhusliku osa (keskmiselt poole) erakonna häältest, järgmine juhusliku osa ülejäänust
 *   jne. Erakondadele, kelle toetusprotsenti ei ole antud, ja üksikkandidaatidele hääli ei jagata.
 * </p>
 */
fun simuleeriHääled(kandidaadid: List<Kandidaat>, toetusprotsendid: Map<String, Map<Int, Double>>, juhus: Random) {
  for ((erakond, toetus) in toetusprotsendid) {
    for (ringkond in valimisringkonnad) {
      val toetusprotsent = toetus[ringkond.nr]
        ?: throw IllegalArgumentException("Erakonna $erakond toetus ringkonnas ${ringkond.nr} puudub")
      val hääli = arvutaHäälteArv(kehtivaidHääli[ringkond.nr]!!, toetusprotsent)
      val kandidaadidRingkonnas =
        kandidaadid.filter { it.erakond == erakond && it.valimisringkond == ringkond.nr }
      val kandidaatideHääled = jagadaArvNiksJuhuslikuksOsaks(hääli, kandidaadidRingkonnas.size, juhus)
      for (i in kandidaadidRingkonnas.indices) {
        kandidaadidRingkonnas[i].hääli = kandidaatideHääled[i]
      }
    }
  }
}

/** Simuleerib ühed valimised: loeb kandidaadid, simuleerib hääled ja jagab mandaadid. */
fun simuleeriValimised(küsitleja: Küsitleja, juhus: Random, reitingufail: String? = null): Mandaadijaotus {
  val kandidaadid = AndmeFailideLugeja().loeKandidaadikirjed()
  simuleeriHääled(kandidaadid, loeToetusprotsendid(küsitleja, reitingufail), juhus)
  return jaotaMandaadid(kandidaadid, valimisringkonnad.toList(), kehtivaidHääli)
}

/** Kandidaadi tunnus, mis ei sõltu simuleeritud häälte arvust. */
data class KandidaadiVõti(val nimi: String, val erakond: String, val valimisringkond: Int)

/** Korduva simulatsiooni kokkuvõte. */
data class Kokkuvõte(
  val kordi: Int,
  /** kandidaat -> mitu korda ta valituks osutus; ainult vähemalt korra valituks osutunud kandidaadid */
  val valitudKordi: Map<KandidaadiVõti, Int>,
  /** erakonna nimi -> erakonna mandaatide arv igas korduses */
  val erakondadeMandaadid: Map<String, List<Int>>,
)

/** Kordab simulatsiooni [kordi] korda; sisendandmed loetakse üks kord. */
fun simuleeriKorduvalt(küsitleja: Küsitleja, kordi: Int, juhus: Random, reitingufail: String? = null): Kokkuvõte {
  val kandidaadid = AndmeFailideLugeja().loeKandidaadikirjed()
  val toetusprotsendid = loeToetusprotsendid(küsitleja, reitingufail)

  val valitudKordi = mutableMapOf<KandidaadiVõti, Int>()
  val mandaadidKordusteKaupa = mutableListOf<Map<String, Int>>()
  repeat(kordi) {
    val korduseKandidaadid = kandidaadid.map { it.copy() }
    simuleeriHääled(korduseKandidaadid, toetusprotsendid, juhus)
    val valitud = jaotaMandaadid(korduseKandidaadid, valimisringkonnad.toList(), kehtivaidHääli).kõikMandaadid()
    for (k in valitud) {
      val võti = KandidaadiVõti(k.nimi, k.erakond, k.valimisringkond)
      valitudKordi[võti] = (valitudKordi[võti] ?: 0) + 1
    }
    mandaadidKordusteKaupa.add(valitud.groupingBy { it.erakond }.eachCount())
  }

  val erakonnadMandaatidega = mandaadidKordusteKaupa.flatMap { it.keys }.distinct()
  val erakondadeMandaadid = erakonnadMandaatidega.associateWith { erakond ->
    mandaadidKordusteKaupa.map { it[erakond] ?: 0 }
  }
  return Kokkuvõte(kordi, valitudKordi, erakondadeMandaadid)
}
