import kotlin.math.pow
import kotlin.random.Random

/** meetod, mis arvutab erakonna häälte arvu ringkonnas */
fun arvutaHäälteArv(hääled: Int, toetusprotsent: Double): Int {
  return (hääled * toetusprotsent / 100).toInt()
}

/** Vaikimisi küsitlusvea standardhälve protsendipunktides (vt [lisaKüsitlusviga]). */
const val VAIKIMISI_KÜSITLUSVIGA = 2.0

private val gaussiGeneraator = java.util.Random()

/**
 * Lisab toetusprotsendile normaaljaotusega küsitlusvea, et korduvad käivitused
 * peegeldaksid küsitluste määramatust ka erakondade tasandil. Ilma selleta on
 * erakondade häältesaak (valimisaktiivsus x toetusprotsent) igal käivitusel
 * identne. Standardhälve on protsendipunktides; 0 lülitab vea lisamise välja.
 */
fun lisaKüsitlusviga(toetusprotsent: Double, standardhälve: Double): Double {
  if (standardhälve <= 0.0) return toetusprotsent
  return maxOf(0.0, toetusprotsent + gaussiGeneraator.nextGaussian() * standardhälve)
}

fun leiaDHondtiJagaja(saadudMandaatideArv: Int): Double {
  return (saadudMandaatideArv + 1).toDouble().pow(0.9)
}

/** Vaikimisi kahanemistegur häälte jagamisel nimekirja järjestuse alusel (vt [jagaHääledKandidaatideVahel]). */
const val VAIKIMISI_KAHANEMISTEGUR = 0.7

/**
 * Jaotab erakonna ringkonnahääled kandidaatide vahel kaalutud multinomiaalselt.
 *
 * Kandidaadi kaal nimekirja positsioonil i on kahanemistegur^(i-1): esinumber saab
 * suurima oodatava osa ja tagumised järjest väiksema, mis jäljendab häälte
 * koondumist nimekirja etteotsa. Iga hääl loositakse kaalude järgi eraldi,
 * nii et tulemus kõigub käivituste vahel oodatava jaotuse ümber.
 *
 * Kahanemistegur 1.0 tähendab ühtlast jaotust; mida väiksem tegur, seda
 * tugevamini koonduvad hääled esinumbritele.
 */
fun jagaHääledKandidaatideVahel(hääli: Int, n: Int, kahanemistegur: Double = VAIKIMISI_KAHANEMISTEGUR): List<Int> {
  if (n == 0) return emptyList()

  val kaalud = DoubleArray(n) { kahanemistegur.pow(it) }
  val kaaludeSumma = kaalud.sum()
  val kumulatiivsedOsakaalud = DoubleArray(n)
  var summa = 0.0
  for (i in 0 until n) {
    summa += kaalud[i] / kaaludeSumma
    kumulatiivsedOsakaalud[i] = summa
  }

  val osad = IntArray(n)
  repeat(hääli) {
    val loos = Random.nextDouble()
    var i = kumulatiivsedOsakaalud.indexOfFirst { loos < it }
    if (i < 0) i = n - 1 // kaitse ujukomavigade vastu skaala ülemises otsas
    osad[i]++
  }
  return osad.toList()
}