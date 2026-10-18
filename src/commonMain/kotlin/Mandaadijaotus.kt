import andmed.MANDAATE_KOKKU
import andmed.ÜKSIKKANDIDAADID
import mudel.Kandidaat
import mudel.Valimisringkond

/** Erakonna ringkonnamandaatide arvutus ühes ringkonnas (RKVS § 62 lg 3); veebilehe selgituse jaoks. */
data class ErakonnaRingkonnaArvutus(
  val erakond: String,
  /** erakonna kandidaatide häälte summa ringkonnas */
  val hääli: Int,
  /** hääled jagatud lihtkvoodiga */
  val kvoote: Double,
  /** mandaatide arv lihtkvoodi ja 0,75 reegli järgi */
  val mandaate: Int,
  /** kas mandaatide arvu suurendati, sest jääk oli vähemalt 75% lihtkvoodist */
  val lisamandaat: Boolean,
  /** valituks osutunud kandidaadid (sh isikumandaadi saanud) */
  val valitud: List<Kandidaat>,
  /** kandidaadid, kes oleksid häälte järgi valituks osutunud, kuid said alla 10% lihtkvoodist */
  val alla10Protsendi: List<Kandidaat>,
)

/** Ühe ringkonna mandaatide arvutus; veebilehe selgituse jaoks. */
data class RingkonnaArvutus(
  val ringkond: Int,
  val kehtivaidHääli: Int,
  val mandaate: Int,
  val lihtkvoot: Double,
  /** 5% künnise ületanud erakonnad häälte arvu järgi kahanevalt */
  val erakonnad: List<ErakonnaRingkonnaArvutus>,
  /** kandidaadid, kelle häälte arv on vähemalt lihtkvoot (RKVS § 62 lg 2), sõltumata nimekirjast */
  val isikumandaadid: List<Kandidaat>,
)

/** Mandaatide jagamise tulemus. */
data class Mandaadijaotus(
  /** ringkonna number -> erakonna nimi -> ringkonnamandaatide arv */
  val ringkonnamandaatideArv: Map<Int, Map<String, Int>>,
  val ringkonnamandaadid: List<Kandidaat>,
  val kompensatsioonimandaadid: List<Kandidaat>,
  /** erakonna nimi -> häälte arv üle riigi (üksikkandidaatideta) */
  val erakondadeHääled: Map<String, Int> = emptyMap(),
  /** 5% künnise ületanud erakonnad */
  val künniseÜletanud: Set<String> = emptySet(),
  val ringkonnaArvutused: List<RingkonnaArvutus> = emptyList(),
  val kompensatsiooniSammud: List<KompensatsiooniSamm> = emptyList(),
) {
  /** kõik mandaadi saanud kandidaadid erakonna ja üleriigilise nimekirja järjekorra järgi */
  fun kõikMandaadid(): List<Kandidaat> =
    (ringkonnamandaadid + kompensatsioonimandaadid).sortedWith(compareBy({ it.erakond }, { it.jrkNr }))
}

/** Jagab mandaadid kandidaatide häälte põhjal.
 * <p>
 *   Kandidaat, kelle häälte arv on vähemalt lihtkvoot, saab isikumandaadi sõltumata nimekirjast.
 *   Ringkonna- ja kompensatsioonimandaatide jagamises osalevad ainult erakonnad, kes said üle riigi
 *   vähemalt 5% kehtivatest häältest. Erakonna hääled ringkonnas on tema kandidaatide häälte summa.
 *   Erakonna ringkonnamandaadid (sh isikumandaadid) saavad erakonna enim hääli saanud kandidaadid
 *   ringkonnas, kelle häälte arv on vähemalt 10% lihtkvoodist. Ülejäänud mandaadid, sh need
 *   ringkonnamandaadid, millele nõuetele vastavat kandidaati ei leitud, jagatakse kompensatsioonimandaatidena.
 *   Üksikkandidaate ei käsitleta erakonna nimekirjana.
 * </p>
 *
 * @param kandidaadid - kandidaadid koos häälte arvu ja üleriigilise nimekirja järjekorranumbriga; ei muudeta
 * @param ringkonnad - valimisringkonnad koos mandaatide arvuga
 * @param kehtivaidHääli - ringkonna number -> kehtivate häälte arv ringkonnas
 * @param mandaateKokku - jagatavate mandaatide koguarv
 */
fun jaotaMandaadid(
  kandidaadid: List<Kandidaat>,
  ringkonnad: List<Valimisringkond>,
  kehtivaidHääli: Map<Int, Int>,
  mandaateKokku: Int = MANDAATE_KOKKU
): Mandaadijaotus {
  val lihtkvoodid = ringkonnad.associate { it.nr to lihtkvoot(kehtivaidHääli[it.nr]!!, it.mandaatideArv) }

  // 5% künnise ületanud erakonnad
  val nimekirjadeKandidaadid = kandidaadid.filter { it.erakond != ÜKSIKKANDIDAADID }
  val erakondadeHääledKoguRiigis = nimekirjadeKandidaadid
    .groupBy { it.erakond }
    .mapValues { (_, erakonnaKandidaadid) -> erakonnaKandidaadid.sumOf { it.hääli } }
  val kehtivaidHääliKoguRiigis = kehtivaidHääli.values.sum()
  val künniseÜletanud = erakondadeHääledKoguRiigis.filterValues { it >= 0.05 * kehtivaidHääliKoguRiigis }.keys

  // kandidaadid ringkondade kaupa häälte arvu järgi kahanevalt; võrdse häälte arvu korral on eespool
  // üleriigilises nimekirjas eespool olev kandidaat (RKVS § 62 lg 3)
  val kandidaadidSorteeritud = kandidaadid
    .sortedWith(compareBy({ it.valimisringkond }, { -it.hääli }, { it.jrkNr }))

  val ringkonnamandaatideArv = mutableMapOf<Int, Map<String, Int>>()
  val ringkonnamandaadid = mutableListOf<Kandidaat>()
  val ringkonnaArvutused = mutableListOf<RingkonnaArvutus>()
  for (ringkond in ringkonnad) {
    val kvoot = lihtkvoodid[ringkond.nr]!!
    val kandidaadidRingkonnas = kandidaadidSorteeritud.filter { it.valimisringkond == ringkond.nr }
    val valitudRingkonnas = mutableListOf<Kandidaat>()

    // künnise ületanud erakondade ringkonnamandaadid; isikumandaadi saanud on nende hulgas esimesed
    val erakondadeHääled = kandidaadidRingkonnas
      .filter { it.erakond in künniseÜletanud }
      .groupBy { it.erakond }
      .mapValues { (_, erakonnaKandidaadid) -> erakonnaKandidaadid.sumOf { it.hääli } }
      .toList().sortedByDescending { (_, hääled) -> hääled }.toMap()
    val erakondadeMandaadid = arvutaRingkonnaMandaadid(erakondadeHääled, kehtivaidHääli[ringkond.nr]!!, ringkond.mandaatideArv)
    val erakondadeArvutused = mutableListOf<ErakonnaRingkonnaArvutus>()
    for ((erakond, mandaate) in erakondadeMandaadid) {
      val erakonnaKandidaadid = kandidaadidRingkonnas.filter { it.erakond == erakond }
      val valitud = erakonnaKandidaadid.filter { it.hääli >= 0.10 * kvoot }.take(mandaate)
      valitudRingkonnas.addAll(valitud)
      val hääli = erakondadeHääled[erakond]!!
      erakondadeArvutused.add(
        ErakonnaRingkonnaArvutus(
          erakond = erakond,
          hääli = hääli,
          kvoote = hääli / kvoot,
          mandaate = mandaate,
          lisamandaat = mandaate > (hääli / kvoot).toInt(),
          valitud = valitud,
          alla10Protsendi = erakonnaKandidaadid.take(mandaate).filter { it.hääli < 0.10 * kvoot },
        )
      )
    }

    // isikumandaadid üksikkandidaatidele ja künnist mitte ületanud nimekirjade kandidaatidele
    valitudRingkonnas.addAll(kandidaadidRingkonnas.filter { it.erakond !in künniseÜletanud && it.hääli >= kvoot })

    ringkonnamandaatideArv[ringkond.nr] = valitudRingkonnas.groupingBy { it.erakond }.eachCount()
    ringkonnamandaadid.addAll(valitudRingkonnas)
    ringkonnaArvutused.add(
      RingkonnaArvutus(
        ringkond = ringkond.nr,
        kehtivaidHääli = kehtivaidHääli[ringkond.nr]!!,
        mandaate = ringkond.mandaatideArv,
        lihtkvoot = kvoot,
        erakonnad = erakondadeArvutused,
        isikumandaadid = kandidaadidRingkonnas.filter { it.hääli >= kvoot },
      )
    )
  }

  // erakonna koht valimisringkonna kandidaatide koondnimekirjas: kandidaadid on seal erakondade kaupa
  // registreerimisnumbrite järjekorras (RKVS § 33 lg 2)
  val erakondadeKohtKoondnimekirjas = nimekirjadeKandidaadid
    .groupBy { it.erakond }
    .mapValues { (_, erakonnaKandidaadid) -> erakonnaKandidaadid.minOf { it.regNr } }

  val jagatudMandaadidErakondadeKaupa = künniseÜletanud.associateWith { erakond ->
    ringkonnamandaadid.count { it.erakond == erakond }
  }

  val kompensatsiooniSammud = arvutaKompensatsiooniSammud(
    nimekirjadeKandidaadid.filter { it.erakond in künniseÜletanud } - ringkonnamandaadid.toSet(),
    jagamataMandaate = mandaateKokku - ringkonnamandaadid.size,
    jagatudMandaadidErakondadeKaupa,
    erakondadeHääledKoguRiigis,
    lihtkvoodid,
    erakondadeKohtKoondnimekirjas
  )

  return Mandaadijaotus(
    ringkonnamandaatideArv, ringkonnamandaadid, kompensatsiooniSammud.map { it.kandidaat },
    erakondadeHääledKoguRiigis, künniseÜletanud, ringkonnaArvutused, kompensatsiooniSammud
  )
}

/** mandaadi saanud kandidaadid tekstina, üks kandidaat reas; see on ka runs/ kausta failide vorming */
fun mandaadidTekstina(jaotus: Mandaadijaotus): String = jaotus.kõikMandaadid().joinToString("\n") + "\n"
