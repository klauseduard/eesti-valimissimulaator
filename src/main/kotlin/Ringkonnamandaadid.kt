import andmed.erakonnad
import andmed.hääletanud
import andmed.valimisringkonnad
import mudel.Valimisringkond


/** arvuta ringkonnamandaadid; nimekirjamandaate jagatakse ainult valimiskünnise ületanud erakondadele */
fun arvutaRingkonnaMandaadid(künniseÜletanudErakonnad: Set<String>) : Map<Int, Map<String, Int>> {
  val mandaadidRingkondadeKaupa = mutableMapOf<Int, Map<String, Int>>()

  for (ringkond in valimisringkonnad) {
    arvutaRingkonnaMandaadid(ringkond, mandaadidRingkondadeKaupa, künniseÜletanudErakonnad)
  }
  return mandaadidRingkondadeKaupa
}

private fun arvutaRingkonnaMandaadid(
  ringkond: Valimisringkond,
  mandaadidRingkondadeKaupa: MutableMap<Int, Map<String, Int>>,
  künniseÜletanudErakonnad: Set<String>
) {
  val erakondadeHääledSorteeritult = järjestaErakonnadHäälteArvuJärgi(ringkond, künniseÜletanudErakonnad)
  println("Ringkond ${ringkond.nr} (${erakondadeHääledSorteeritult})")

  val ringkonnamandaadidErakondadeKaupa = mutableMapOf<String, Int>()

  val lihtkvoot = hääletanud[ringkond.nr]!! / ringkond.mandaatideArv.toDouble();

  arvutaMandaadid(erakondadeHääledSorteeritult, ringkonnamandaadidErakondadeKaupa, lihtkvoot)

  trükiRingkonnamandaadid(ringkond, ringkonnamandaadidErakondadeKaupa)

  mandaadidRingkondadeKaupa[ringkond.nr] = ringkonnamandaadidErakondadeKaupa
}

private fun trükiRingkonnamandaadid(
  ringkond: Valimisringkond,
  ringkonnamandaadidErakondadeKaupa: MutableMap<String, Int>
) {
  println("Ringkond ${ringkond.nr} (${ringkond.nimetus})")
  for (erakond in ringkonnamandaadidErakondadeKaupa) {
    println("  ${erakond.key}: ${erakond.value}")
  }
}

private fun arvutaMandaadid(
  erakondadeHääledSorteeritult: List<Pair<String, Int>>,
  ringkonnamandaadidErakondadeKaupa: MutableMap<String, Int>,
  lihtkvoot: Double
) {
  for ((erakond, hääled) in erakondadeHääledSorteeritult) {
    ringkonnamandaadidErakondadeKaupa[erakond] = (hääled / lihtkvoot).toInt()
    if (((hääled % lihtkvoot) / lihtkvoot) >= 0.75) {
      ringkonnamandaadidErakondadeKaupa[erakond] = ringkonnamandaadidErakondadeKaupa[erakond]!! + 1
    }
  }
}

private fun järjestaErakonnadHäälteArvuJärgi(
  ringkond: Valimisringkond,
  künniseÜletanudErakonnad: Set<String>
): List<Pair<String, Int>> {
  val ringkonnaHääledErakondadeKaupa = mutableMapOf<String, Int>()

  for (erakond in erakonnad) {
    if (erakond.nimetus !in künniseÜletanudErakonnad) continue
    ringkonnaHääledErakondadeKaupa[erakond.nimetus] = erakondadeHääledRingkondadeKaupa[erakond.nimetus]!![ringkond.nr]!!
  }

  return ringkonnaHääledErakondadeKaupa.toList().sortedByDescending { (_, value) -> value }
}