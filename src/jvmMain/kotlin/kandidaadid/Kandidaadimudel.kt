package kandidaadid

import JuhuslikKahanev
import Jaotusmudel
import Positsioonimudel
import andmed.AndmeFailideLugeja
import andmed.RK2023_TULEMUSE_FAIL
import andmed.ÜKSIKKANDIDAADID
import andmed.kandidaadidTegelikeHäältega
import andmed.loeRk2023Tulemus
import andmed.valimisringkonnad
import lihtkvoot
import mudel.Kandidaat
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.random.Random

/*
 * Kandidaatide häälte jaotuse mudeli sobitamine ja test (vt docs/kandidaatide-mudel.md).
 * Käivitamine: ./gradlew kandidaaditest
 */

fun main(args: Array<String>) {
  require(args.size == 2) { "Kasutamine: KandidaadimudelKt <aruanne.md> <parameetrid.json>" }
  val tulemus = kandidaaditest(loeNimekirjad(AndmeFailideLugeja()))
  File(args[0]).writeText(tulemus.first)
  File(args[1]).writeText(tulemus.second)
}

/** Erakonna nimekiri ühes ringkonnas 2023. aastal: kandidaadid nimekirja järjekorras tegelike häältega. */
data class Nimekiri(
  val erakond: String,
  val ringkond: Int,
  val kandidaadid: List<Kandidaat>,
  /** ringkonna tasandil (isiku- või ringkonnamandaadiga) valitud kandidaatide registreerimisnumbrid */
  val valitud: Set<Int>,
  val lihtkvoot: Double,
) {
  val hääli: Int get() = kandidaadid.sumOf { it.hääli }
}

fun loeNimekirjad(lugeja: AndmeFailideLugeja): List<Nimekiri> {
  val tulemus = loeRk2023Tulemus(lugeja.loeRessurss(RK2023_TULEMUSE_FAIL))
  val mandaadid = tulemus.kandidaadid.associate { it.regNr to it.mandaat }
  val kandidaadid = kandidaadidTegelikeHäältega(lugeja.loeKandidaadikirjed(), tulemus)
  return kandidaadid.filter { it.erakond != ÜKSIKKANDIDAADID }
    .groupBy { it.erakond to it.valimisringkond }
    .map { (võti, k) ->
      val ringkond = valimisringkonnad.single { it.nr == võti.second }
      Nimekiri(
        erakond = võti.first,
        ringkond = võti.second,
        kandidaadid = k.sortedBy { it.regNr },
        valitud = k.filter { mandaadid[it.regNr] == "PERSONAL" || mandaadid[it.regNr] == "DISTRICT" }.map { it.regNr }.toSet(),
        lihtkvoot = lihtkvoot(tulemus.kehtivaidHääli[ringkond.nr]!!, ringkond.mandaatideArv),
      )
    }
}

/** gammafunktsiooni logaritm (Lanczose lähend) */
fun lnGamma(x: Double): Double {
  if (x < 0.5) return ln(PI / sin(PI * x)) - lnGamma(1 - x)
  val g = 7.0
  val c = doubleArrayOf(
    0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
    -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7,
  )
  val xx = x - 1
  var a = c[0]
  val t = xx + g + 0.5
  for (i in 1 until 9) a += c[i] / (xx + i)
  return 0.5 * ln(2 * PI) + (xx + 0.5) * ln(t) - t + ln(a)
}

/** Dirichlet' log-tõepära tegelikel osakaaludel (iga kandidaadi häältele lisatakse 0,5) */
fun logTõepära(mudel: Positsioonimudel, nimekirjad: List<Nimekiri>): Double {
  var summa = 0.0
  for (n in nimekirjad) {
    if (n.kandidaadid.size < 2) continue
    val m = mudel.ootuspärasedOsakaalud(n.kandidaadid)
    val kokku = n.hääli + 0.5 * n.kandidaadid.size
    summa += lnGamma(mudel.kappa)
    n.kandidaadid.forEachIndexed { i, k ->
      val a = mudel.kappa * m[i]
      summa += -lnGamma(a) + (a - 1) * ln((k.hääli + 0.5) / kokku)
    }
  }
  return summa
}

/**
 * Suurima tõepära parameetrid mustriotsinguga: iga parameetrit muudetakse sammu võrra mõlemas suunas; kui
 * ükski muutus tõepära ei paranda, vähendatakse sammu poole võrra.
 */
fun sobita(nimekirjad: List<Nimekiri>): Positsioonimudel {
  // otsing teisendatud ruumis: b, c ja κ logaritmidena, d otse (d ≥ 0)
  var x = doubleArrayOf(ln(0.8), ln(0.05), 0.3, ln(5.0))
  fun mudel(v: DoubleArray) = Positsioonimudel(exp(v[0]), exp(v[1]), maxOf(0.0, v[2]), exp(v[3]))
  var parim = logTõepära(mudel(x), nimekirjad)
  var samm = 0.5
  while (samm > 1e-4) {
    var paranes = false
    for (i in x.indices) for (suund in listOf(1.0, -1.0)) {
      val y = x.copyOf().also { it[i] += suund * samm }
      if (i == 2 && y[2] < 0) continue
      val t = logTõepära(mudel(y), nimekirjad)
      if (t > parim) { parim = t; x = y; paranes = true }
    }
    if (!paranes) samm /= 2
  }
  return mudel(x)
}

/**
 * Kui mitu ringkonna tasandil tegelikult valitud kandidaati mudel õigesti nimetab: iga nimekiri saab oma
 * tegelikud hääled ja tegeliku kohtade arvu; kohad saavad enim hääli saanud kandidaadid, kellel on vähemalt
 * 10% lihtkvoodist. Tagastab korduste keskmise.
 */
fun õigeidValituid(mudel: Jaotusmudel, nimekirjad: List<Nimekiri>, kordi: Int, juhus: Random): Double {
  var kokku = 0L
  repeat(kordi) {
    for (n in nimekirjad) {
      if (n.valitud.isEmpty()) continue
      val hääled = mudel.jaota(n.hääli, n.kandidaadid, juhus)
      val järjestatud = n.kandidaadid.indices
        .filter { hääled[it] >= 0.10 * n.lihtkvoot }
        .sortedWith(compareBy({ -hääled[it] }, { n.kandidaadid[it].jrkNr }))
        .take(n.valitud.size)
      kokku += järjestatud.count { n.kandidaadid[it].regNr in n.valitud }
    }
  }
  return kokku.toDouble() / kordi
}

private fun f(x: Double, kohti: Int = 2) = String.format(Locale.ROOT, "%.${kohti}f", x)

/** Jäta-üks-ringkond-välja test ja lõplikud parameetrid; tagastab aruande ja parameetrite JSON-i. */
fun kandidaaditest(nimekirjad: List<Nimekiri>, kordi: Int = 200): Pair<String, String> {
  val r = StringBuilder()
  r.appendLine("> **Note on AI authorship.** This report was generated by a program written by AI (Claude by")
  r.appendLine("> Anthropic, in Claude Code); the surrounding text was written by AI as well.")
  r.appendLine()
  r.appendLine("# Candidate vote model test")
  r.appendLine()
  r.appendLine("Generated by `./gradlew kandidaaditest`. The model and the criterion were fixed beforehand in")
  r.appendLine("`docs/kandidaatide-mudel.md`. Score: candidates elected at the district stage in 2023 whom the model")
  r.appendLine("names correctly, given each list's actual votes and seats; mean over $kordi runs per district.")
  r.appendLine()
  r.appendLine("| District | Seats at district stage | Stick-breaking | Fitted model | Fitted parameters (b, c, d, κ) |")
  r.appendLine("|---|---|---|---|---|")
  var vana = 0.0
  var uus = 0.0
  var kohti = 0
  for (ringkond in valimisringkonnad) {
    val välja = nimekirjad.filter { it.ringkond == ringkond.nr }
    val mudel = sobita(nimekirjad.filter { it.ringkond != ringkond.nr })
    val juhus = Random(ringkond.nr)
    val a = õigeidValituid(JuhuslikKahanev, välja, kordi, juhus)
    val b = õigeidValituid(mudel, välja, kordi, Random(ringkond.nr))
    val k = välja.sumOf { it.valitud.size }
    vana += a; uus += b; kohti += k
    r.appendLine("| ${ringkond.nr} | $k | ${f(a, 1)} | ${f(b, 1)} | ${f(mudel.b)}, ${f(mudel.c, 3)}, ${f(mudel.d)}, ${f(mudel.kappa, 1)} |")
  }
  r.appendLine("| **Total** | $kohti | ${f(vana, 1)} | ${f(uus, 1)} | |")
  r.appendLine()
  val võetakse = uus >= vana + 1.0
  r.appendLine("Under the criterion fixed beforehand (at least one more correct candidate), the fitted model " +
    (if (võetakse) "**becomes the default**." else "**does not become the default**; stick-breaking stays."))
  r.appendLine()
  val lõplik = sobita(nimekirjad)
  r.appendLine("## Final parameters (all districts)")
  r.appendLine()
  r.appendLine("b = ${f(lõplik.b, 3)}, c = ${f(lõplik.c, 4)}, d = ${f(lõplik.d, 3)}, κ = ${f(lõplik.kappa, 2)}.")
  r.appendLine()
  r.appendLine("| Position | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |")
  r.appendLine("|---|---|---|---|---|---|---|---|---|---|---|")
  val näide = (1..10).map { Kandidaat("k$it", "A", 1, jrkNr = 100 + it) }
  r.appendLine("| Expected share, list of 10, no national top five | " +
    lõplik.ootuspärasedOsakaalud(näide).joinToString(" | ") { f(it, 3) } + " |")
  r.appendLine()
  val json = "{\n  \"vaikimisi\": $võetakse,\n  \"b\": ${lõplik.b},\n  \"c\": ${lõplik.c},\n  \"d\": ${lõplik.d},\n  \"kappa\": ${lõplik.kappa}\n}\n"
  return r.toString() to json
}
