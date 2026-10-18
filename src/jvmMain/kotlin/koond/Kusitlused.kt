package koond

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ln
import kotlin.math.exp

/*
 * Küsitluste koondamine. Valikud ja nende põhjendused on failis docs/kusitluste-koond.md; need fikseeriti
 * enne tagasitestimist.
 */

/** küsitluse avaldamise viivitus: küsitlus võib kuupäeva T hinnangusse minna, kui välitöö lõppes hiljemalt T − 3 päeva */
const val VIIVITUS_PÄEVI = 3L

/** küsitleja keskmisesse lähevad küsitlused, mille kuupäev on kuni 60 päeva enne hinnangu kuupäeva */
const val AKEN_PÄEVI = 60L

/** küsitluse kaal väheneb poole võrra iga 14 päevaga */
const val POOLESTUSAEG_PÄEVI = 14.0

/** valimi suurus, millest suuremat ei arvestata, disainiefekt ja tundmatu valimi suurus */
const val VALIMI_ÜLEMPIIR = 2000
const val DISAINIEFEKT = 1.5
const val TUNDMATU_VALIM = 1000

/** väikseim osakaal logaritmi dispersiooni arvutamisel, et väikeste erakondade dispersioon ei kasvaks lõpmatuseni */
const val VÄIKSEIM_OSAKAAL = 0.01

data class Küsitlus(
  val küsitleja: String,
  /** välitöö algus; null, kui teada on ainult üks kuupäev */
  val algus: LocalDate?,
  /** välitöö lõpp; kui teada on ainult kuu, siis kuu viimane päev (vt [ainultKuu]) */
  val lõpp: LocalDate,
  val valim: Int?,
  /** erakonna kood -> osakaal protsentides, normaliseeritud summale 100 */
  val osakaalud: Map<String, Double>,
  /** osakaalude summa enne normaliseerimist */
  val algneSumma: Double,
  /** kas lõpu kuupäevast on teada ainult kuu */
  val ainultKuu: Boolean = false,
) {
  /** küsitluse kuupäev: välitöö keskpunkt, ainus teadaolev kuupäev või ainult kuu teadmisel kuu 15. päev */
  val kuupäev: LocalDate
    get() = when {
      ainultKuu -> lõpp.withDayOfMonth(15)
      algus == null -> lõpp
      else -> algus.plusDays(ChronoUnit.DAYS.between(algus, lõpp) / 2)
    }

  val efektiivneValim: Double
    get() = (valim ?: TUNDMATU_VALIM).coerceAtMost(VALIMI_ÜLEMPIIR) / DISAINIEFEKT

  fun kattub(teine: Küsitlus): Boolean = (algus ?: lõpp) <= teine.lõpp && (teine.algus ?: teine.lõpp) <= lõpp
}

/** veerud, mis ei ole erakonnad */
private val MITTE_ERAKONNAD = setOf(
  "kusitleja", "algus", "lopp", "valim", "alus", "tyyp", "lopp_liik", "valim_tekst", "allikas", "markus",
)

/** CSV-rea väljad; jutumärkides väljas võib olla komasid, kahekordne jutumärk on jutumärk */
fun csvVäljad(rida: String): List<String> {
  val väljad = mutableListOf<String>()
  val praegune = StringBuilder()
  var jutumärkides = false
  var i = 0
  while (i < rida.length) {
    val c = rida[i]
    when {
      jutumärkides && c == '"' && i + 1 < rida.length && rida[i + 1] == '"' -> { praegune.append('"'); i++ }
      c == '"' -> jutumärkides = !jutumärkides
      c == ',' && !jutumärkides -> { väljad.add(praegune.toString().trim()); praegune.clear() }
      else -> praegune.append(c)
    }
    i++
  }
  väljad.add(praegune.toString().trim())
  return väljad
}

/**
 * Loeb küsitlused CSV-failist (vt src/jvmMain/resources/kusitlused/LOEMIND.md). Kui muude osakaal puudub ja
 * erakondade summa on alla 100, on muude osakaal 100 miinus summa; kui küsitluse alus sisaldab otsustamata
 * vastajaid (alus = koik_vastajad), normaliseeritakse rida summale 100.
 */
fun loeKüsitlused(csv: String): List<Küsitlus> {
  val read = csv.lines().filter { it.isNotBlank() }
  val päis = csvVäljad(read.first())
  return read.drop(1).map { rida ->
    val väljad = csvVäljad(rida)
    require(väljad.size == päis.size) { "Vale väljade arv (${väljad.size}, oodatud ${päis.size}) real: $rida" }
    val väli = päis.zip(väljad).toMap()
    val osakaalud = päis.filter { it !in MITTE_ERAKONNAD }
      .mapNotNull { erakond -> väli[erakond]?.takeIf { it.isNotEmpty() }?.let { erakond to it.toDouble() } }
      .toMap().toMutableMap()
    val summa = osakaalud.values.sum()
    require(summa > 0) { "Osakaalud puuduvad real: $rida" }
    val kõikVastajad = väli["alus"] == "koik_vastajad"
    if ("MUUD" !in osakaalud && summa < 100.0 && !kõikVastajad) osakaalud["MUUD"] = 100.0 - summa
    val uusSumma = osakaalud.values.sum()
    val lõpuTekst = väli["lopp"]!!
    val ainultKuu = Regex("""\d{4}-\d{2}""").matches(lõpuTekst)
    Küsitlus(
      küsitleja = väli["kusitleja"]!!,
      algus = väli["algus"]?.takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
      lõpp = if (ainultKuu) java.time.YearMonth.parse(lõpuTekst).atEndOfMonth() else LocalDate.parse(lõpuTekst),
      valim = väli["valim"]?.takeIf { it.isNotEmpty() }?.toInt(),
      osakaalud = osakaalud.mapValues { it.value * 100.0 / uusSumma },
      algneSumma = summa,
      ainultKuu = ainultKuu,
    )
  }
}

/**
 * Küsitlused, mis lähevad kuupäeva [t] hinnangusse: välitöö lõppes hiljemalt [t] − viivitus, kuupäev on akna
 * sees, ja sama küsitleja kattuvatest küsitlustest jääb alles uusim (vt docs/kusitluste-koond.md, punkt 4).
 */
fun valiKüsitlused(küsitlused: List<Küsitlus>, t: LocalDate): List<Küsitlus> {
  val sobivad = küsitlused.filter {
    !it.lõpp.isAfter(t.minusDays(VIIVITUS_PÄEVI)) && !it.kuupäev.isBefore(t.minusDays(AKEN_PÄEVI))
  }
  return sobivad.groupBy { it.küsitleja }.values.flatMap { küsitleja ->
    val valitud = mutableListOf<Küsitlus>()
    for (k in küsitleja.sortedByDescending { it.lõpp }) {
      if (valitud.none { it.kattub(k) }) valitud.add(k)
    }
    valitud
  }
}

/** Ühe küsitleja hinnang erakonnale: logaritmilise osakaalu kaalutud keskmine ja selle valimidispersioon. */
data class KüsitlejaHinnang(val küsitleja: String, val logKeskmine: Double, val dispersioon: Double)

fun logaritmiDispersioon(osakaal: Double, efektiivneValim: Double): Double {
  val p = (osakaal / 100.0).coerceAtLeast(VÄIKSEIM_OSAKAAL)
  return (1 - p) / (efektiivneValim * p)
}

/** küsitleja hinnang erakonnale; null, kui küsitleja seda erakonda ei küsinud */
fun küsitlejaHinnang(küsitlused: List<Küsitlus>, erakond: String, t: LocalDate): KüsitlejaHinnang? {
  val read = küsitlused.filter { (it.osakaalud[erakond] ?: 0.0) > 0.0 }
  if (read.isEmpty()) return null
  val kaalud = read.map { k ->
    val vanus = ChronoUnit.DAYS.between(k.kuupäev, t).toDouble()
    k.efektiivneValim * exp(-vanus * ln(2.0) / POOLESTUSAEG_PÄEVI)
  }
  val kaalSumma = kaalud.sum()
  val keskmine = read.indices.sumOf { kaalud[it] * ln(read[it].osakaalud[erakond]!! / 100.0) } / kaalSumma
  val dispersioon = read.indices.sumOf {
    kaalud[it] * kaalud[it] * logaritmiDispersioon(read[it].osakaalud[erakond]!!, read[it].efektiivneValim)
  } / (kaalSumma * kaalSumma)
  return KüsitlejaHinnang(read.first().küsitleja, keskmine, dispersioon)
}
