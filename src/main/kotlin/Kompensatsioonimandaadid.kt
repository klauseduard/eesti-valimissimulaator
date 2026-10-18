import mudel.Kandidaat

/** Jagab kandidaatide vahel kompensatsioonimandaadid "modifitseeritud d'Hondti meetodil".
 * <p>
 *   Kompensatsioonimandaadid jaotatakse modifitseeritud d’Hondti jagajate meetodil jagajate jadadega
 *   1, 2^0,9, 3^0,9, 4^0,9 jne. Seejuures jäetakse iga erakonna võrdlusarvude arvutamisel vahele nii
 *   mitu jada esimest elementi, kui mitu mandaati sai erakond valimisringkondades. Kui vähemalt kahe
 *   erakonna võrdlusarvud on võrdsed, saab mandaadi erakond, kelle kandidaadid paiknesid valimisringkonna
 *   kandidaatide koondnimekirjas tagapool.
 * </p>
 * <p>
 *   Erakonna mandaadi saab üleriigilises nimekirjas eespool olev kandidaat, kelle poolt antud häälte arv
 *   on vähemalt 5% tema valimisringkonna lihtkvoodist. Kui selliseid kandidaate nimekirjas enam ei ole,
 *   saab mandaadi sama nimekirja kandidaat, kes sai suurima osa oma ringkonna lihtkvoodist; võrdse osa
 *   korral nimekirjas eespool olev kandidaat.
 * </p>
 * <p>
 *   Erakond ei või saada rohkem mandaate, kui on tema nimekirjas kandidaate, seega jäetakse erakond,
 *   kelle nimekirjas pole enam kandidaate, jagamisest välja.
 * </p>
 *
 * (Vt Riigikogu valimise seadus § 62 lg 4–8)
 *
 *  @param kandidaadid - kandidaadid, kes ei ole veel mandaati saanud
 *  @param jagamataMandaate - mitu mandaati jagada
 *  @param jagatudMandaadidErakondadeKaupa - jagamises osalevad erakonnad ja nende juba saadud mandaatide arv
 *  @param erakondadeHääledKoguRiigis - erakondade üleriiklik häältesaak
 *  @param lihtkvoodid - ringkonna number -> ringkonna lihtkvoot
 *  @param erakondadeKohtKoondnimekirjas - erakonna nimi -> koht kandidaatide koondnimekirjas; suurem arv on tagapool
 *  @return jagatud mandaadid
 */
fun arvutaKompensatsioonimandaadid(
  kandidaadid: List<Kandidaat>,
  jagamataMandaate: Int,
  jagatudMandaadidErakondadeKaupa: Map<String, Int>,
  erakondadeHääledKoguRiigis: Map<String, Int>,
  lihtkvoodid: Map<Int, Double>,
  erakondadeKohtKoondnimekirjas: Map<String, Int>
): List<Kandidaat> {

  // sisendeid ei muudeta, töötame koopiatega
  val vabadKandidaadid = kandidaadid.toMutableList()
  val jagatudMandaadid = jagatudMandaadidErakondadeKaupa.toMutableMap()

  val kompensatsiooniMandaadid = mutableListOf<Kandidaat>()
  var loendur = jagamataMandaate

  // iga mandaat arvutatakse ühekaupa lähtuvalt juba välja jagatud mandaatide arvust kuni jagatavate mandaatide arv otsa lõppeb
  while (loendur > 0) {
    // jagamises osalevad erakonnad, kelle nimekirjas on veel kandidaate
    val osalevadErakonnad = jagatudMandaadid.filterKeys { erakond -> vabadKandidaadid.any { it.erakond == erakond } }
    if (osalevadErakonnad.isEmpty()) break

    // arvuta erakondade võrdlusarvud juba saadud mandaatide alusel
    val erakondadeVõrdlusarvud = arvutaVõrdlusarvud(osalevadErakonnad, erakondadeHääledKoguRiigis)

    val järgmineMandaadigaKandidaat =
      jagaJärgmineMandaat(erakondadeVõrdlusarvud, vabadKandidaadid, lihtkvoodid, erakondadeKohtKoondnimekirjas)

    kompensatsiooniMandaadid.add(järgmineMandaadigaKandidaat)

    vabadKandidaadid.remove(järgmineMandaadigaKandidaat)
    jagatudMandaadid[järgmineMandaadigaKandidaat.erakond] = jagatudMandaadid[järgmineMandaadigaKandidaat.erakond]!! + 1
    loendur = loendur.dec()
  }
  return kompensatsiooniMandaadid
}

private fun jagaJärgmineMandaat(
  erakondadeVõrdlusarvud: Map<String, Double>,
  kandidaadid: List<Kandidaat>,
  lihtkvoodid: Map<Int, Double>,
  erakondadeKohtKoondnimekirjas: Map<String, Int>
): Kandidaat {
  // võrdsete võrdlusarvude korral saab mandaadi erakond, kelle kandidaadid on koondnimekirjas tagapool
  val suurimaVõrdlusarvugaErakond = erakondadeVõrdlusarvud.entries
    .maxWith(compareBy({ it.value }, { erakondadeKohtKoondnimekirjas[it.key]!! })).key
  val kandidaadidErakonnas = kandidaadid.filter { it.erakond == suurimaVõrdlusarvugaErakond }
  fun kvoodiOsakaal(k: Kandidaat) = k.hääli / lihtkvoodid[k.valimisringkond]!!

  return kandidaadidErakonnas.filter { kvoodiOsakaal(it) >= 0.05 }.minByOrNull { it.jrkNr }
    ?: kandidaadidErakonnas.maxWith(compareBy({ kvoodiOsakaal(it) }, { -it.jrkNr }))
}

private fun arvutaVõrdlusarvud(
  jagatudMandaadidErakondadeKaupa: Map<String, Int>,
  erakondadeHääledKoguRiigis: Map<String, Int>
): Map<String, Double> {
  return jagatudMandaadidErakondadeKaupa.mapValues { (erakond, mandaatideArv) ->
    erakondadeHääledKoguRiigis[erakond]!!.toDouble() / leiaDHondtiJagaja(mandaatideArv)
  }
}
