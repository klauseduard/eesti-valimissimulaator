import kotlin.math.pow
import kotlin.random.Random

/** meetod, mis arvutab erakonna häälte arvu ringkonnas */
fun arvutaHäälteArv(hääled: Int, toetusprotsent: Double): Int {
  return (hääled * toetusprotsent / 100).toInt()
}

fun leiaDHondtiJagaja(saadudMandaatideArv: Int): Double {
  return (saadudMandaatideArv + 1).toDouble().pow(0.9)
}

/** jaota arv n-iks juhuslikuks osaks */
fun jagadaArvNiksJuhuslikuksOsaks(arv: Int, n: Int, juhus: Random = Random): List<Int> {
  val osad = mutableListOf<Int>()
  var jääk: Int = arv


  while (osad.size < n) {
    if (jääk == 0) {
      osad.add(0)
      continue
    }
    val osa: Int = (1..jääk).random(juhus)
    osad.add(osa)
    jääk -= osa
  }

  // kui jääk > 0 lisa see esimesele osale; kui osi pole, jäävad hääled jagamata
  if (jääk > 0 && osad.isNotEmpty()) {
    osad[0] += jääk
  }
  return osad
}