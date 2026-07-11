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

  arvutaMandaadid(erakondadeHääledSorteeritult, ringkonnamandaadidErakondadeKaupa, lihtkvoot, ringkond.mandaatideArv)

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
  lihtkvoot: Double,
  mandaatideArv: Int
) {
  var jagatud = 0

  // kõigepealt täiskvootidele vastavad mandaadid; suurema häältesaagiga erakonnad on järjekorras eespool
  for ((erakond, hääled) in erakondadeHääledSorteeritult) {
    val täiskvoote = minOf((hääled / lihtkvoot).toInt(), mandaatideArv - jagatud)
    ringkonnamandaadidErakondadeKaupa[erakond] = täiskvoote
    jagatud += täiskvoote
  }

  // seejärel 0.75 reegli ümardusmandaadid jäägi suuruse järjekorras, kuid ainult
  // seni, kuni ringkonnas kohti jätkub -- muidu võiks ringkond üle jaotada
  val ümardusteJärjekord = erakondadeHääledSorteeritult
    .map { (erakond, hääled) -> erakond to (hääled % lihtkvoot) / lihtkvoot }
    .filter { it.second >= 0.75 }
    .sortedByDescending { it.second }
  for ((erakond, _) in ümardusteJärjekord) {
    if (jagatud >= mandaatideArv) break
    ringkonnamandaadidErakondadeKaupa[erakond] = ringkonnamandaadidErakondadeKaupa[erakond]!! + 1
    jagatud++
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