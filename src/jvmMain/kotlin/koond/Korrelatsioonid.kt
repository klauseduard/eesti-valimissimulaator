package koond

import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sqrt

/*
 * Erakondade vigade korrelatsioonide katse (vt docs/kusitluste-koond.md, vastuvõtmise kriteeriumid):
 * korrelatsioonid võetakse kasutusele ainult siis, kui need parandavad jäta-üks-välja log-skoori.
 */

/** korrelatsioonimaatriksi kahandamine nulli poole: R' = λ·R + (1 − λ)·I */
const val KORRELATSIOONI_KAHANDUS = 0.5

/** standardiseeritud vead (tulemus − hinnang) / hälve ühe valimise ja horisondi kohta, erakonnad -> väärtus */
typealias Vead = Map<String, Double>

/** Pearsoni korrelatsioonid erakonnapaaride vahel üle vaatluste (iga vaatlus on üks valimine ja horisont). */
fun korrelatsioonid(vaatlused: List<Vead>, erakonnad: List<String>): Array<DoubleArray> {
  val n = erakonnad.size
  val r = Array(n) { i -> DoubleArray(n) { j -> if (i == j) 1.0 else 0.0 } }
  for (i in 0 until n) for (j in i + 1 until n) {
    val paarid = vaatlused.mapNotNull { v -> v[erakonnad[i]]?.let { a -> v[erakonnad[j]]?.let { b -> a to b } } }
    if (paarid.size < 3) continue
    val ma = paarid.map { it.first }.average()
    val mb = paarid.map { it.second }.average()
    val sab = paarid.sumOf { (it.first - ma) * (it.second - mb) }
    val saa = paarid.sumOf { (it.first - ma) * (it.first - ma) }
    val sbb = paarid.sumOf { (it.second - mb) * (it.second - mb) }
    if (saa <= 0 || sbb <= 0) continue
    val c = KORRELATSIOONI_KAHANDUS * sab / sqrt(saa * sbb)
    r[i][j] = c
    r[j][i] = c
  }
  return r
}

/** standardiseeritud vigade mitmemõõtmelise normaaljaotuse log-tihedus korrelatsioonimaatriksiga [r] (Cholesky) */
fun logTihedus(z: DoubleArray, r: Array<DoubleArray>): Double? {
  val n = z.size
  val l = Array(n) { DoubleArray(n) }
  for (i in 0 until n) for (j in 0..i) {
    var s = r[i][j]
    for (k in 0 until j) s -= l[i][k] * l[j][k]
    if (i == j) {
      if (s <= 0) return null
      l[i][i] = sqrt(s)
    } else l[i][j] = s / l[j][j]
  }
  // y = L⁻¹ z
  val y = DoubleArray(n)
  for (i in 0 until n) {
    var s = z[i]
    for (k in 0 until i) s -= l[i][k] * y[k]
    y[i] = s / l[i][i]
  }
  val logDet = 2 * (0 until n).sumOf { ln(l[it][it]) }
  return -0.5 * (n * ln(2 * PI) + logDet + y.sumOf { it * it })
}
