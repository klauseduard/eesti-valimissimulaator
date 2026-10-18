package koond

import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Koondmudeli dispersioonid logaritmilisel skaalal (vt docs/kusitluste-koond.md, mudeli punktid 3–4).
 *
 * @param majaefekt - küsitlejate ühine (kokku arvutatud) majaefekti dispersioon
 * @param küsitlejateMajaefekt - küsitleja -> tema majaefekti dispersioon, kahandatud ühise poole
 * @param ühine - kõigi küsitlejate ühise vea dispersioon
 */
data class Mudeliparameetrid(
  val majaefekt: Double,
  val küsitlejateMajaefekt: Map<String, Double>,
  val ühine: Double,
) {
  fun majaefekt(küsitleja: String): Double = küsitlejateMajaefekt[küsitleja] ?: majaefekt
}

enum class Meetod(val kirjeldus: String) {
  VIIMANE_NORSTAT("last Norstat poll"),
  VIIMASED_VÕRDSELT("latest poll of each pollster, equal weights"),
  VÕRDSED_KAALUD("aggregate, equal weights"),
  USALDUSKAALUD("aggregate, trust weights"),
}

/** Hinnang erakondade toetusele: logaritmilise osakaalu keskmine (normaliseeritud) ja standardhälve. */
data class Hinnang(val logKeskmine: Map<String, Double>, val logSd: Map<String, Double>) {
  fun osakaal(erakond: String): Double = exp(logKeskmine[erakond]!!) * 100.0
}

/** normaliseerib logaritmilised keskmised nii, et osakaalude summa on 100% */
private fun normaliseeri(logKeskmine: Map<String, Double>): Map<String, Double> {
  val summa = logKeskmine.values.sumOf { exp(it) }
  return logKeskmine.mapValues { it.value - ln(summa) }
}

/** Hinnang kuupäeval [t] meetodiga [meetod]; null, kui sobivaid küsitlusi pole. */
fun hinnang(küsitlused: List<Küsitlus>, t: LocalDate, meetod: Meetod, p: Mudeliparameetrid): Hinnang? {
  val sobivad = küsitlused.filter {
    !it.lõpp.isAfter(t.minusDays(VIIVITUS_PÄEVI)) && !it.kuupäev.isBefore(t.minusDays(AKEN_PÄEVI))
  }
  if (sobivad.isEmpty()) return null
  val keskmised = mutableMapOf<String, Double>()
  val dispersioonid = mutableMapOf<String, Double>()

  when (meetod) {
    Meetod.VIIMANE_NORSTAT -> {
      val k = sobivad.filter { it.küsitleja == "Norstat" }.maxByOrNull { it.lõpp } ?: return null
      for ((erakond, osakaal) in k.osakaalud) {
        if (osakaal <= 0.0) continue
        keskmised[erakond] = ln(osakaal / 100.0)
        dispersioonid[erakond] = logaritmiDispersioon(osakaal, k.efektiivneValim) + p.majaefekt(k.küsitleja) + p.ühine
      }
    }
    Meetod.VIIMASED_VÕRDSELT -> {
      val viimased = sobivad.groupBy { it.küsitleja }.values.map { it.maxBy { k -> k.lõpp } }
      for (erakond in viimased.flatMap { it.osakaalud.keys }.toSet()) {
        val read = viimased.filter { (it.osakaalud[erakond] ?: 0.0) > 0.0 }
        if (read.isEmpty()) continue
        keskmised[erakond] = read.map { ln(it.osakaalud[erakond]!! / 100.0) }.average()
        dispersioonid[erakond] = read.sumOf {
          logaritmiDispersioon(it.osakaalud[erakond]!!, it.efektiivneValim) + p.majaefekt(it.küsitleja)
        } / (read.size * read.size) + p.ühine
      }
    }
    Meetod.VÕRDSED_KAALUD, Meetod.USALDUSKAALUD -> {
      val valitud = valiKüsitlused(küsitlused, t)
      for (erakond in valitud.flatMap { it.osakaalud.keys }.toSet()) {
        val hinnangud = valitud.groupBy { it.küsitleja }.values.mapNotNull { küsitlejaHinnang(it, erakond, t) }
        if (hinnangud.isEmpty()) continue
        if (meetod == Meetod.VÕRDSED_KAALUD) {
          keskmised[erakond] = hinnangud.map { it.logKeskmine }.average()
          dispersioonid[erakond] = hinnangud.sumOf { it.dispersioon + p.majaefekt(it.küsitleja) } /
            (hinnangud.size * hinnangud.size) + p.ühine
        } else {
          val kaalud = hinnangud.map { 1.0 / (it.dispersioon + p.majaefekt(it.küsitleja)) }
          keskmised[erakond] = hinnangud.indices.sumOf { kaalud[it] * hinnangud[it].logKeskmine } / kaalud.sum()
          dispersioonid[erakond] = 1.0 / kaalud.sum() + p.ühine
        }
      }
    }
  }
  if (keskmised.isEmpty()) return null
  return Hinnang(normaliseeri(keskmised), dispersioonid.mapValues { sqrt(it.value) })
}

/** Valimised: kuupäev, tulemus (erakonna kood -> protsent) ja selle valimiste küsitlused. */
data class Valimised(val aasta: Int, val kuupäev: LocalDate, val tulemus: Map<String, Double>, val küsitlused: List<Küsitlus>)

/** hinnatavad erakonnad: tulemus vähemalt 3% (vt docs/kusitluste-koond.md) */
const val HINNATAVA_PIIR = 3.0

/** majaefekti kahandamise kaal vaatlustes (umbes ühe valimise erakonnad) */
const val KAHANDAMISE_KAAL = 6.0

/**
 * Dispersioonikomponentide ühine hinnang (vt docs/kusitluste-koond.md, mudeli punkt 4): iga valimise ja
 * erakonna jaoks küsitlejate keskmiste erinevus tulemusest d. Küsitlejate vaheline hajuvus annab majaefekti
 * dispersiooni, erinevuste keskmise ruut miinus küsitlejate osa annab ühise vea dispersiooni.
 */
fun hindaParameetrid(valimised: List<Valimised>, horisontPäevi: Long): Mudeliparameetrid {
  data class Rühm(val erinevused: List<Pair<String, Double>>, val dispersioonid: List<Double>)
  val rühmad = mutableListOf<Rühm>()
  for (v in valimised) {
    val t = v.kuupäev.minusDays(horisontPäevi)
    val valitud = valiKüsitlused(v.küsitlused, t)
    for ((erakond, tulemus) in v.tulemus) {
      if (tulemus < HINNATAVA_PIIR) continue
      val hinnangud = valitud.groupBy { it.küsitleja }.values.mapNotNull { küsitlejaHinnang(it, erakond, t) }
      if (hinnangud.isEmpty()) continue
      rühmad.add(Rühm(hinnangud.map { it.küsitleja to (it.logKeskmine - ln(tulemus / 100.0)) }, hinnangud.map { it.dispersioon }))
    }
  }

  // majaefekt: küsitlejate vaheline hajuvus miinus valimidispersioon, rühmades, kus on vähemalt kaks küsitlejat
  val mitmega = rühmad.filter { it.erinevused.size >= 2 }
  val majaefekt = if (mitmega.isEmpty()) 0.0 else mitmega.map { r ->
    val d = r.erinevused.map { it.second }
    val keskmine = d.average()
    d.sumOf { (it - keskmine) * (it - keskmine) } / (d.size - 1) - r.dispersioonid.average()
  }.average().coerceAtLeast(0.0)

  // ühine viga: rühma keskmise ruut miinus küsitlejate osa (majaefekt ja valimidispersioon jagatud küsitlejate arvuga)
  val ühine = rühmad.map { r ->
    val keskmine = r.erinevused.map { it.second }.average()
    keskmine * keskmine - (majaefekt + r.dispersioonid.average()) / r.erinevused.size
  }.average().coerceAtLeast(0.0)

  // küsitleja oma majaefekt: jäägid rühma keskmisest, kahandatud ühise majaefekti poole
  val jäägid = mutableMapOf<String, MutableList<Double>>()
  for (r in mitmega) {
    val keskmine = r.erinevused.map { it.second }.average()
    val n = r.erinevused.size
    r.erinevused.forEachIndexed { i, (küsitleja, d) ->
      jäägid.getOrPut(küsitleja) { mutableListOf() }.add((d - keskmine) * (d - keskmine) * n / (n - 1) - r.dispersioonid[i])
    }
  }
  val küsitlejad = jäägid.mapValues { (_, j) ->
    ((j.sum() + KAHANDAMISE_KAAL * majaefekt) / (j.size + KAHANDAMISE_KAAL)).coerceAtLeast(0.0)
  }
  return Mudeliparameetrid(majaefekt, küsitlejad, ühine)
}

/** Ühe hinnangu skoor: hinnatavate erakondade arv, absoluutvigade summa, 90% vahemiku katvus ja log-skoor. */
data class Skoor(val erakondi: Int, val absViga: Double, val kaetud: Int, val logSkoor: Double) {
  val keskmineAbsViga: Double get() = absViga / erakondi
  operator fun plus(teine: Skoor) =
    Skoor(erakondi + teine.erakondi, absViga + teine.absViga, kaetud + teine.kaetud, logSkoor + teine.logSkoor)
}

/** 90% vahemiku z-väärtus */
const val Z90 = 1.6448536269514722

fun skoor(h: Hinnang, tulemus: Map<String, Double>): Skoor {
  var n = 0
  var abs = 0.0
  var kaetud = 0
  var log = 0.0
  for ((erakond, t) in tulemus) {
    if (t < HINNATAVA_PIIR || erakond !in h.logKeskmine) continue
    val m = h.logKeskmine[erakond]!!
    val sd = h.logSd[erakond]!!
    val x = ln(t / 100.0)
    n += 1
    abs += abs(h.osakaal(erakond) - t)
    if (abs(x - m) <= Z90 * sd) kaetud += 1
    log += -0.5 * ln(2 * PI * sd * sd) - (x - m) * (x - m) / (2 * sd * sd)
  }
  return Skoor(n, abs, kaetud, log)
}
