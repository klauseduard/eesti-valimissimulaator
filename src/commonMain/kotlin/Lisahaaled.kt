import mudel.Kandidaat
import mudel.Valimisringkond

/*
 * Mandaatide jagamine, kui tegelikele häältele lisatakse valitud kandidaatidele hääli. Lisatud hääled on uued
 * valijad, seega kasvab ka ringkonna kehtivate häälte arv ja koos sellega lihtkvoot. Veebiliides avab selle
 * välistele programmidele, nt taktikalise hääletamise mängule (eraldi projekt).
 */

/**
 * Jagab mandaadid, kui [kandidaadid] (tegelike häältega) saavad lisaks hääli [lisad] järgi
 * (registreerimisnumber -> lisatud hääled). Lisatud hääled suurendavad oma ringkonna kehtivate häälte arvu.
 */
fun jaotaLisatudHäältega(
  kandidaadid: List<Kandidaat>,
  ringkonnad: List<Valimisringkond>,
  kehtivaidHääli: Map<Int, Int>,
  lisad: Map<Int, Int>,
): Mandaadijaotus {
  require(lisad.values.all { it >= 0 }) { "Lisatud hääli ei saa olla negatiivselt" }
  val regNrJärgi = kandidaadid.associateBy { it.regNr }
  for (regNr in lisad.keys) require(regNr in regNrJärgi) { "Tundmatu kandidaat $regNr" }
  val muudetud = kandidaadid.map { k -> lisad[k.regNr]?.let { k.copy(hääli = k.hääli + it) } ?: k }
  val lisatudRingkonniti = lisad.entries.groupBy({ regNrJärgi[it.key]!!.valimisringkond }, { it.value })
    .mapValues { (_, hääled) -> hääled.sum() }
  val kehtivaid = kehtivaidHääli.mapValues { (ringkond, hääli) -> hääli + (lisatudRingkonniti[ringkond] ?: 0) }
  return jaotaMandaadid(muudetud, ringkonnad, kehtivaid)
}

/** kõigi mandaadi saanud kandidaatide registreerimisnumbrid */
fun valitudRegNrid(jaotus: Mandaadijaotus): Set<Int> = jaotus.kõikMandaadid().map { it.regNr }.toSet()
