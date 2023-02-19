import mudel.Kandidaat

/** Jagab kandidaatide vahel kompensatsioonimandaadid "modifitseeritud d'Hondti meetodil".
 * <p>
 *   Kompensatsioonimandaadid jaotatakse modifitseeritud d’Hondti jagajate meetodil jagajate jadadega
 *   1, 2^0,9, 3^0,9, 4^0,9 jne. Seejuures jäetakse iga erakonna võrdlusarvude arvutamisel vahele nii
 *   mitu jada esimest elementi, kui mitu mandaati sai erakond valimisringkondades. Kui vähemalt kahe
 *   erakonna võrdlusarvud on võrdsed, saab mandaadi erakond, kelle kandidaadid paiknesid valimisringkonna
 *   kandidaatide koondnimekirjas tagapool.
 * </p>
 *
 * (Vt Riigikogu valimise seadus)
 *
 *  @param kandidaadid - kandidaatide nimekiri
 *  @param jagamataMandaate - mitu mandaati jagada
 *  @param jagatudMandaadidErakondadeKaupa - juba jagatud mandaatide arv erakondade kaupa
 *  @param erakondadeHääledKoguRiigis - erakondade üleriiklik häältesaak
 *  @return jagatud mandaadid
 */
fun arvutaKompensatsioonimandaadid(
  kandidaadid: MutableList<Kandidaat>,
  jagamataMandaate: Int,
  jagatudMandaadidErakondadeKaupa: MutableMap<String, Int>,
  erakondadeHääledKoguRiigis: MutableMap<String, Int>
): List<Kandidaat> {

  val kompensatsiooniMandaadid = mutableListOf<Kandidaat>()
  var loendur = jagamataMandaate

  // iga mandaat arvutatakse ühekaupa lähtuvalt juba välja jagatud mandaatide arvust kuni jagatavate mandaatide arv otsa lõppeb
  while (loendur > 0) {
    val erakondadeVõrdlusarvud = mutableMapOf<String, Double>()

    // arvuta erakondade võrdlusarvud juba saadud mandaatide alusel
    arvutaUuedVõrdlusarvud(jagatudMandaadidErakondadeKaupa, erakondadeVõrdlusarvud, erakondadeHääledKoguRiigis)

    val järgmineMandaadigaKandidaat = jagaJärgmineMandaat(erakondadeVõrdlusarvud, kandidaadid)

    kompensatsiooniMandaadid.add(järgmineMandaadigaKandidaat)

    kandidaadid.remove(järgmineMandaadigaKandidaat)
    jagatudMandaadidErakondadeKaupa[järgmineMandaadigaKandidaat.erakond] = jagatudMandaadidErakondadeKaupa[järgmineMandaadigaKandidaat.erakond]!! + 1;
    loendur = loendur.dec()
  }
  return kompensatsiooniMandaadid;
}

private fun jagaJärgmineMandaat(
  erakondadeVõrdlusarvud: MutableMap<String, Double>,
  kandidaadid: MutableList<Kandidaat>
): Kandidaat {
  val suurimaVõrdlusarvugaErakond = erakondadeVõrdlusarvud.maxBy { it.value }.key
  val kandidaadidErakonnas =
    kandidaadid.filter { it.erakond == suurimaVõrdlusarvugaErakond }.sortedWith(compareBy { it.jrkNr })
  return kandidaadidErakonnas.first()
}

private fun arvutaUuedVõrdlusarvud(
  jagatudMandaadidErakondadeKaupa: MutableMap<String, Int>,
  erakondadeVõrdlusarvud: MutableMap<String, Double>,
  erakondadeHääledKoguRiigis: MutableMap<String, Int>
) {
  for ((erakond, mandaatideArv) in jagatudMandaadidErakondadeKaupa) {
    val jagaja = leiaDHondtiJagaja(mandaatideArv)
    erakondadeVõrdlusarvud[erakond] = erakondadeHääledKoguRiigis[erakond]!!.toDouble() / jagaja
  }
}