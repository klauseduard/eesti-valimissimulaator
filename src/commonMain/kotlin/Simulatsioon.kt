import andmed.valimisringkonnad
import mudel.Kandidaat
import mudel.Valimisringkond
import kotlin.random.Random

/** Määrab kandidaatidele juhusliku häälte arvu.
 * <p>
 *   Erakonna hääled ringkonnas arvutatakse toetusprotsendi ja ringkonna kehtivate häälte arvu põhjal
 *   ning jagatakse juhuslikult erakonna selle ringkonna kandidaatide vahel: kandidaatide failis esimene
 *   kandidaat saab juhusliku osa (keskmiselt poole) erakonna häältest, järgmine juhusliku osa ülejäänust
 *   jne. Erakondadele, kelle toetusprotsenti ei ole antud, ja üksikkandidaatidele hääli ei jagata.
 * </p>
 *
 * @param toetusprotsendid - erakonna nimi -> ringkonna number -> toetusprotsent
 * @param kehtivaidHääli - ringkonna number -> kehtivate häälte arv ringkonnas
 */
fun simuleeriHääled(
  kandidaadid: List<Kandidaat>,
  toetusprotsendid: Map<String, Map<Int, Double>>,
  juhus: Random,
  kehtivaidHääli: Map<Int, Int> = andmed.kehtivaidHääli,
  ringkonnad: List<Valimisringkond> = valimisringkonnad.toList(),
) {
  for ((erakond, toetus) in toetusprotsendid) {
    for (ringkond in ringkonnad) {
      val toetusprotsent = toetus[ringkond.nr]
        ?: throw IllegalArgumentException("Erakonna $erakond toetus ringkonnas ${ringkond.nr} puudub")
      require(toetusprotsent >= 0.0) { "Erakonna $erakond toetus ringkonnas ${ringkond.nr} on negatiivne" }
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

/** Kandidaadi tunnus, mis ei sõltu simuleeritud häälte arvust. */
data class KandidaadiVõti(val nimi: String, val erakond: String, val valimisringkond: Int)

/** Korduva simulatsiooni kokkuvõte. */
data class Kokkuvõte(
  val kordi: Int,
  /** kandidaat -> mitu korda ta valituks osutus; ainult vähemalt korra valituks osutunud kandidaadid */
  val valitudKordi: Map<KandidaadiVõti, Int>,
  /** erakonna nimi -> erakonna mandaatide arv igas korduses */
  val erakondadeMandaadid: Map<String, List<Int>>,
  /** ringkonna number -> erakonna nimi -> erakonna ringkonnamandaatide arv ringkonnas igas korduses */
  val ringkondadeMandaadid: Map<Int, Map<String, List<Int>>>,
  /**
   * ringkonna number -> erakonna nimi -> ringkonnast valitud kandidaatide arv igas korduses; erinevalt
   * [ringkondadeMandaadid] on siin ka kompensatsioonimandaadi saanud kandidaadid
   */
  val ringkondadeValitud: Map<Int, Map<String, List<Int>>> = emptyMap(),
)

/**
 * Korduv simulatsioon, mida saab jätkata osade kaupa (nt veebilehe taustalõimes 100 kordust korraga).
 * Sama juhuslikkuse allika korral annab kaks korda 50 kordust sama tulemuse kui üks kord 100 kordust.
 */
class KorduvSimulatsioon(
  private val kandidaadid: List<Kandidaat>,
  private val toetusprotsendid: Map<String, Map<Int, Double>>,
  private val juhus: Random,
  private val kehtivaidHääli: Map<Int, Int> = andmed.kehtivaidHääli,
  private val ringkonnad: List<Valimisringkond> = valimisringkonnad.toList(),
  /** küsitluse viga; null tähendab, et toetus on igas korduses täpselt küsitluse oma */
  private val viga: Küsitluseviga? = null,
) {
  private val valitudKordi = mutableMapOf<KandidaadiVõti, Int>()
  private val mandaadidKordusteKaupa = mutableListOf<Map<String, Int>>()
  private val ringkondadeMandaadidKordusteKaupa = mutableListOf<Map<Int, Map<String, Int>>>()
  private val ringkondadeValitudKordusteKaupa = mutableListOf<Map<Int, Map<String, Int>>>()

  val kordi: Int get() = mandaadidKordusteKaupa.size

  /** Kordab simulatsiooni veel [lisakordi] korda. */
  fun korda(lisakordi: Int) {
    repeat(lisakordi) {
      val korduseKandidaadid = kandidaadid.map { it.copy() }
      val korduseToetus = if (viga == null) toetusprotsendid else juhuslikToetus(toetusprotsendid, viga, juhus)
      simuleeriHääled(korduseKandidaadid, korduseToetus, juhus, kehtivaidHääli, ringkonnad)
      val jaotus = jaotaMandaadid(korduseKandidaadid, ringkonnad, kehtivaidHääli)
      val valitud = jaotus.kõikMandaadid()
      for (k in valitud) {
        val võti = KandidaadiVõti(k.nimi, k.erakond, k.valimisringkond)
        valitudKordi[võti] = (valitudKordi[võti] ?: 0) + 1
      }
      mandaadidKordusteKaupa.add(valitud.groupingBy { it.erakond }.eachCount())
      ringkondadeMandaadidKordusteKaupa.add(jaotus.ringkonnamandaatideArv)
      ringkondadeValitudKordusteKaupa.add(
        valitud.groupBy { it.valimisringkond }.mapValues { (_, k) -> k.groupingBy { it.erakond }.eachCount() }
      )
    }
  }

  fun kokkuvõte(): Kokkuvõte {
    val erakonnad = mandaadidKordusteKaupa.flatMap { it.keys }.distinct()
    val erakondadeMandaadid = erakonnad.associateWith { erakond -> mandaadidKordusteKaupa.map { it[erakond] ?: 0 } }
    fun ringkondadeKaupa(kordused: List<Map<Int, Map<String, Int>>>) = ringkonnad.associate { ringkond ->
      val ringkonnaErakonnad = kordused.flatMap { it[ringkond.nr]?.keys ?: emptySet() }.distinct()
      ringkond.nr to ringkonnaErakonnad.associateWith { erakond -> kordused.map { it[ringkond.nr]?.get(erakond) ?: 0 } }
    }
    return Kokkuvõte(
      kordi, valitudKordi.toMap(), erakondadeMandaadid,
      ringkondadeKaupa(ringkondadeMandaadidKordusteKaupa), ringkondadeKaupa(ringkondadeValitudKordusteKaupa)
    )
  }
}

/** Kordab simulatsiooni [kordi] korda. */
fun simuleeriKorduvalt(
  kandidaadid: List<Kandidaat>,
  toetusprotsendid: Map<String, Map<Int, Double>>,
  kordi: Int,
  juhus: Random,
  kehtivaidHääli: Map<Int, Int> = andmed.kehtivaidHääli,
  viga: Küsitluseviga? = null,
): Kokkuvõte {
  val simulatsioon = KorduvSimulatsioon(kandidaadid, toetusprotsendid, juhus, kehtivaidHääli, viga = viga)
  simulatsioon.korda(kordi)
  return simulatsioon.kokkuvõte()
}

/** Erakonna üleriigiline toetusprotsent: ringkondade toetusprotsentide kaalutud keskmine kehtivate häälte järgi. */
fun üleriigilineToetus(toetus: Map<Int, Double>, kehtivaidHääli: Map<Int, Int> = andmed.kehtivaidHääli): Double {
  val kokku = kehtivaidHääli.values.sum().toDouble()
  return kehtivaidHääli.entries.sumOf { (ringkond, hääli) -> (toetus[ringkond] ?: 0.0) * hääli } / kokku
}

/**
 * Muudab erakonna üleriigilist toetust nii, et iga ringkonna profiil säilib.
 * <p>
 *   Erakonna toetus korrutatakse igas ringkonnas sama teguriga, et üleriigiline toetus oleks [uusToetus].
 *   Teiste erakondade toetust vähendatakse või suurendatakse igas ringkonnas võrdeliselt, nii et
 *   erakondade toetuste summa ringkonnas ei muutu. Kui erakonna toetus ületaks ringkonnas summa, on
 *   erakonna toetus selles ringkonnas summa ja teiste erakondade toetus 0; siis jääb üleriigiline toetus
 *   alla [uusToetus].
 * </p>
 */
fun muudaÜleriigilistToetust(
  toetusprotsendid: Map<String, Map<Int, Double>>,
  erakond: String,
  uusToetus: Double,
  kehtivaidHääli: Map<Int, Int> = andmed.kehtivaidHääli,
): Map<String, Map<Int, Double>> {
  val erakonnaToetus = toetusprotsendid[erakond] ?: throw IllegalArgumentException("Erakonna $erakond toetus puudub")
  require(uusToetus >= 0.0) { "Toetus ei saa olla negatiivne" }
  val praegune = üleriigilineToetus(erakonnaToetus, kehtivaidHääli)

  val uued = toetusprotsendid.mapValues { it.value.toMutableMap() }
  for (ringkond in kehtivaidHääli.keys) {
    val summa = toetusprotsendid.values.sumOf { it[ringkond] ?: 0.0 }
    val vana = erakonnaToetus[ringkond] ?: 0.0
    // kui erakonnal pole üldse toetust, jagatakse uus toetus ringkondade vahel võrdselt
    val uus = (if (praegune > 0.0) vana * uusToetus / praegune else uusToetus).coerceAtMost(summa)
    val teisteVana = summa - vana
    val teisteUus = summa - uus
    for ((teine, teiseToetus) in uued) {
      if (teine == erakond) teiseToetus[ringkond] = uus
      else if (teisteVana > 0.0) teiseToetus[ringkond] = (teiseToetus[ringkond] ?: 0.0) * teisteUus / teisteVana
    }
  }
  return uued
}
