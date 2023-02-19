import kotlin.math.pow

/** meetod, mis arvutab erakonna häälte arvu ringkonnas */
fun arvutaHäälteArv(hääled: Int, toetusprotsent: Double): Int {
  return (hääled * toetusprotsent / 100).toInt()
}

fun leiaDHondtiJagaja(saadudMandaatideArv: Int): Double {
  return (saadudMandaatideArv + 1).toDouble().pow(0.9)
}

/** jaota arv n-iks juhuslikuks osaks */
fun jagadaArvNiksJuhuslikuksOsaks(arv: Int, n: Int): List<Int> {
  val osad = mutableListOf<Int>()
  var jääk: Int = arv


  while (osad.size < n) {
    if (jääk == 0) {
      osad.add(0)
      continue
    }
    val osa: Int = (1..jääk).random()
    osad.add(osa)
    jääk -= osa
  }
  return osad
}