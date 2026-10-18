import mudel.Kandidaat
import kotlin.math.pow

/** modifitseeritud d'Hondti jagaja: (juba saadud mandaatide arv + 1)^0,9 */
fun leiaDHondtiJagaja(saadudMandaatideArv: Int): Double {
  return (saadudMandaatideArv + 1).toDouble().pow(0.9)
}

/** Ühe kompensatsioonimandaadi jagamise samm; veebilehe selgituse jaoks. */
data class KompensatsiooniSamm(
  /** erakonna nimi -> võrdlusarv selles sammus (ainult jagamises osalevad erakonnad) */
  val võrdlusarvud: Map<String, Double>,
  /** erakonna nimi -> enne seda sammu saadud mandaatide arv; sellest sõltub jagaja */
  val jubaMandaate: Map<String, Int>,
  /** mandaadi saanud erakond */
  val erakond: String,
  /** mandaadi saanud kandidaat */
  val kandidaat: Kandidaat,
  /** kas suurim võrdlusarv oli mitmel erakonnal (RKVS § 62 lg 5 viimane lause) */
  val viik: Boolean,
  /** kas kandidaat valiti varureegli järgi, sest 5% nõudele vastavaid kandidaate polnud (RKVS § 62 lg 7) */
  val varureegel: Boolean,
)

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
): List<Kandidaat> = arvutaKompensatsiooniSammud(
  kandidaadid, jagamataMandaate, jagatudMandaadidErakondadeKaupa, erakondadeHääledKoguRiigis, lihtkvoodid,
  erakondadeKohtKoondnimekirjas
).map { it.kandidaat }

/** Nagu [arvutaKompensatsioonimandaadid], kuid tagastab iga mandaadi jagamise sammu. */
fun arvutaKompensatsiooniSammud(
  kandidaadid: List<Kandidaat>,
  jagamataMandaate: Int,
  jagatudMandaadidErakondadeKaupa: Map<String, Int>,
  erakondadeHääledKoguRiigis: Map<String, Int>,
  lihtkvoodid: Map<Int, Double>,
  erakondadeKohtKoondnimekirjas: Map<String, Int>
): List<KompensatsiooniSamm> {

  // sisendeid ei muudeta, töötame koopiatega
  val vabadKandidaadid = kandidaadid.toMutableList()
  val jagatudMandaadid = jagatudMandaadidErakondadeKaupa.toMutableMap()

  val sammud = mutableListOf<KompensatsiooniSamm>()
  var loendur = jagamataMandaate

  // iga mandaat arvutatakse ühekaupa lähtuvalt juba välja jagatud mandaatide arvust kuni jagatavate mandaatide arv otsa lõppeb
  while (loendur > 0) {
    // jagamises osalevad erakonnad, kelle nimekirjas on veel kandidaate
    val osalevadErakonnad = jagatudMandaadid.filterKeys { erakond -> vabadKandidaadid.any { it.erakond == erakond } }
    if (osalevadErakonnad.isEmpty()) break

    // arvuta erakondade võrdlusarvud juba saadud mandaatide alusel
    val erakondadeVõrdlusarvud = arvutaVõrdlusarvud(osalevadErakonnad, erakondadeHääledKoguRiigis)

    val samm = jagaJärgmineMandaat(
      erakondadeVõrdlusarvud, osalevadErakonnad, vabadKandidaadid, lihtkvoodid, erakondadeKohtKoondnimekirjas
    )
    val järgmineMandaadigaKandidaat = samm.kandidaat

    sammud.add(samm)

    vabadKandidaadid.remove(järgmineMandaadigaKandidaat)
    jagatudMandaadid[järgmineMandaadigaKandidaat.erakond] = jagatudMandaadid[järgmineMandaadigaKandidaat.erakond]!! + 1
    loendur = loendur.dec()
  }
  return sammud
}

private fun jagaJärgmineMandaat(
  erakondadeVõrdlusarvud: Map<String, Double>,
  jubaMandaate: Map<String, Int>,
  kandidaadid: List<Kandidaat>,
  lihtkvoodid: Map<Int, Double>,
  erakondadeKohtKoondnimekirjas: Map<String, Int>
): KompensatsiooniSamm {
  // võrdsete võrdlusarvude korral saab mandaadi erakond, kelle kandidaadid on koondnimekirjas tagapool
  val suurimaVõrdlusarvugaErakond = erakondadeVõrdlusarvud.entries
    .maxWith(compareBy({ it.value }, { erakondadeKohtKoondnimekirjas[it.key]!! })).key
  val suurimVõrdlusarv = erakondadeVõrdlusarvud[suurimaVõrdlusarvugaErakond]!!
  val kandidaadidErakonnas = kandidaadid.filter { it.erakond == suurimaVõrdlusarvugaErakond }
  fun kvoodiOsakaal(k: Kandidaat) = k.hääli / lihtkvoodid[k.valimisringkond]!!

  val nõuetele = kandidaadidErakonnas.filter { kvoodiOsakaal(it) >= 0.05 }.minByOrNull { it.jrkNr }
  val kandidaat = nõuetele ?: kandidaadidErakonnas.maxWith(compareBy({ kvoodiOsakaal(it) }, { -it.jrkNr }))
  return KompensatsiooniSamm(
    võrdlusarvud = erakondadeVõrdlusarvud,
    jubaMandaate = jubaMandaate,
    erakond = suurimaVõrdlusarvugaErakond,
    kandidaat = kandidaat,
    viik = erakondadeVõrdlusarvud.values.count { it == suurimVõrdlusarv } > 1,
    varureegel = nõuetele == null,
  )
}

private fun arvutaVõrdlusarvud(
  jagatudMandaadidErakondadeKaupa: Map<String, Int>,
  erakondadeHääledKoguRiigis: Map<String, Int>
): Map<String, Double> {
  return jagatudMandaadidErakondadeKaupa.mapValues { (erakond, mandaatideArv) ->
    erakondadeHääledKoguRiigis[erakond]!!.toDouble() / leiaDHondtiJagaja(mandaatideArv)
  }
}
