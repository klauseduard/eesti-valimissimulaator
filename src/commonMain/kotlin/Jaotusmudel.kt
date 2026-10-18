import mudel.Kandidaat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Viis, kuidas erakonna hääled ringkonnas kandidaatide vahel jagatakse. Kandidaadid on ringkonnanimekirja
 * järjekorras (kandidaatide failides on see registreerimisnumbrite järjekord).
 */
sealed interface Jaotusmudel {
  /** jagab [hääli] kandidaatide vahel; tulemuse summa on täpselt [hääli] */
  fun jaota(hääli: Int, kandidaadid: List<Kandidaat>, juhus: Random): List<Int>
}

/**
 * Esimene kandidaat saab juhusliku osa häältest (keskmiselt poole), järgmine juhusliku osa ülejäänust jne
 * (vt [jagaJuhuslikeksOsadeks]). Simulaatori esialgne jaotus.
 */
data object JuhuslikKahanev : Jaotusmudel {
  override fun jaota(hääli: Int, kandidaadid: List<Kandidaat>, juhus: Random): List<Int> =
    jagaJuhuslikeksOsadeks(hääli, kandidaadid.size, juhus)
}

/** häälte jaotuse nimed käsureal, URL-is ja veebiliideses */
const val JAOTUS_KALIBREERITUD = "kalibreeritud"
const val JAOTUS_LIHTNE = "lihtne"

fun jaotusmudel(nimi: String): Jaotusmudel = when (nimi) {
  JAOTUS_KALIBREERITUD -> Positsioonimudel.KALIBREERITUD_2023
  JAOTUS_LIHTNE -> JuhuslikKahanev
  else -> throw IllegalArgumentException("Tundmatu häälte jaotus: $nimi")
}

/** erakonna üleriigilise nimekirja esimesed nii mitu kandidaati saavad kaalu lisa */
const val ESINUMBRITE_ARV = 5

/**
 * 2023. aasta valimiste järgi kalibreeritud jaotus (vt docs/kandidaatide-mudel.md): positsiooni p kaal on
 * exp(−b·(p − 1)) + c, üleriigilise nimekirja esiviisikus kandidaadil korrutatuna (1 + d); osakaalud võetakse
 * Dirichlet' jaotusest parameetritega κ · (kaal / kaalude summa).
 */
data class Positsioonimudel(val b: Double, val c: Double, val d: Double, val kappa: Double) : Jaotusmudel {

  companion object {
    /**
     * 2023. aasta kandidaatide häälte järgi sobitatud parameetrid (./gradlew kandidaaditest, aruanne
     * docs/kandidaatide-mudeli-test.md); test KandidaadimudelFailidTest kontrollib, et need vastavad failile
     * src/jvmMain/resources/kandidaadimudel.json.
     */
    val KALIBREERITUD_2023 = Positsioonimudel(b = 0.782995816189366, c = 0.1526631626224763, d = 0.706494140625, kappa = 27.83334569816639)
  }

  fun ootuspärasedOsakaalud(kandidaadid: List<Kandidaat>): List<Double> {
    val kaalud = kandidaadid.mapIndexed { i, k ->
      (exp(-b * i) + c) * (if (k.jrkNr in 1..ESINUMBRITE_ARV) 1 + d else 1.0)
    }
    val summa = kaalud.sum()
    return kaalud.map { it / summa }
  }

  override fun jaota(hääli: Int, kandidaadid: List<Kandidaat>, juhus: Random): List<Int> {
    if (kandidaadid.isEmpty()) return emptyList()
    val gammad = ootuspärasedOsakaalud(kandidaadid).map { gamma(kappa * it, juhus) }
    val summa = gammad.sum()
    val osakaalud = if (summa > 0) gammad.map { it / summa } else ootuspärasedOsakaalud(kandidaadid)
    return suurimaJäägiga(hääli, osakaalud)
  }
}

/** jagab täisarvu osakaalude järgi suurima jäägi meetodil, nii et summa on täpselt [hääli] */
fun suurimaJäägiga(hääli: Int, osakaalud: List<Double>): List<Int> {
  val osad = osakaalud.map { it * hääli }
  val tulemus = osad.map { floor(it).toInt() }.toMutableList()
  val jääk = hääli - tulemus.sum()
  osad.indices.sortedByDescending { osad[it] - floor(osad[it]) }.take(jääk).forEach { tulemus[it] += 1 }
  return tulemus
}

/** standardse normaaljaotusega juhuslik suurus (Box–Mulleri meetod) */
fun normaalne(juhus: Random): Double {
  val u = 1.0 - juhus.nextDouble() // vahemikus (0, 1], et logaritm oleks määratud
  return sqrt(-2.0 * ln(u)) * cos(2.0 * PI * juhus.nextDouble())
}

/** gammajaotusega juhuslik suurus kujuparameetriga [kuju] ja skaalaga 1 (Marsaglia–Tsangi meetod) */
fun gamma(kuju: Double, juhus: Random): Double {
  require(kuju > 0.0) { "Gammajaotuse kuju peab olema positiivne" }
  if (kuju < 1.0) {
    // Gamma(a) = Gamma(a + 1) · U^(1/a)
    return gamma(kuju + 1.0, juhus) * (1.0 - juhus.nextDouble()).pow(1.0 / kuju)
  }
  val dd = kuju - 1.0 / 3.0
  val cc = 1.0 / sqrt(9.0 * dd)
  while (true) {
    var x: Double
    var v: Double
    do {
      x = normaalne(juhus)
      v = 1.0 + cc * x
    } while (v <= 0.0)
    v = v * v * v
    val u = 1.0 - juhus.nextDouble()
    if (ln(u) < 0.5 * x * x + dd - dd * v + dd * ln(v)) return dd * v
  }
}
