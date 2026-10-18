@file:OptIn(ExperimentalJsExport::class)

import andmed.ÜKSIKKANDIDAADID
import andmed.erakonnad
import andmed.kandidaadidTegelikeHäältega
import andmed.loeKandidaadid
import andmed.loeReitingud
import andmed.loeRk2023Tulemus
import andmed.reitingudToetusprotsentidena
import andmed.rk2023Toetusprotsendid
import andmed.valimisringkonnad
import mudel.Kandidaat
import kotlin.random.Random

/*
 * Veebilehe liides. Kõik siin eksporditud klassid sisaldavad ainult massiive, arve ja sõnesid, et neid
 * oleks TypeScriptis lihtne kasutada (vt web/). Vigase sisendi korral visatakse viga (JavaScripti Error),
 * mille teade on eesti keeles.
 */

/** mandaadi liigid */
const val ISIKUMANDAAT = "isikumandaat"
const val RINGKONNAMANDAAT = "ringkonnamandaat"
const val KOMPENSATSIOONIMANDAAT = "kompensatsioonimandaat"

@JsExport
class ErakondJs(
  val nimi: String,
  /** erakonna koht valimisringkonna kandidaatide koondnimekirjas (väikseim registreerimisnumber) */
  val kohtKoondnimekirjas: Int,
)

@JsExport
class RingkondJs(val nr: Int, val nimetus: String, val mandaate: Int, val kehtivaidHääli: Int)

/** Erakondade toetusprotsendid: protsendid[i][j] on erakonna erakonnad[i] toetus ringkonnas j + 1. */
@JsExport
class ToetusJs(val erakonnad: Array<String>, val protsendid: Array<Array<Double>>)

@JsExport
class KandidaatJs(
  val nimi: String,
  val erakond: String,
  val ringkond: Int,
  val hääli: Int,
  /** järjekorranumber erakonna üleriigilises nimekirjas; üksikkandidaatidel 0 */
  val jrkNr: Int,
  val regNr: Int,
  /** [ISIKUMANDAAT], [RINGKONNAMANDAAT], [KOMPENSATSIOONIMANDAAT] või null */
  val mandaat: String?,
)

@JsExport
class ErakonnaRingkonnaArvutusJs(
  val erakond: String,
  val hääli: Int,
  val kvoote: Double,
  val mandaate: Int,
  val lisamandaat: Boolean,
  val valitud: Array<KandidaatJs>,
  val alla10Protsendi: Array<KandidaatJs>,
)

@JsExport
class RingkonnaArvutusJs(
  val ringkond: Int,
  val kehtivaidHääli: Int,
  val mandaate: Int,
  val lihtkvoot: Double,
  val erakonnad: Array<ErakonnaRingkonnaArvutusJs>,
  val isikumandaadid: Array<KandidaatJs>,
)

@JsExport
class KompensatsiooniSammJs(
  /** jagamises osalevad erakonnad; võrdlusarvud ja jubaMandaate on samas järjekorras */
  val erakonnad: Array<String>,
  val võrdlusarvud: Array<Double>,
  val jubaMandaate: Array<Int>,
  val erakond: String,
  val kandidaat: KandidaatJs,
  val viik: Boolean,
  val varureegel: Boolean,
)

@JsExport
class ErakonnaTulemusJs(
  val erakond: String,
  val hääli: Int,
  /** häälte protsent kehtivatest häältest üle riigi */
  val protsent: Double,
  val ületasKünnise: Boolean,
  val ringkonnamandaate: Int,
  val kompensatsioonimandaate: Int,
) {
  val mandaate: Int get() = ringkonnamandaate + kompensatsioonimandaate
}

@JsExport
class TulemusJs(
  /** kõik nimekirjad, kellel on hääli, häälte arvu järgi kahanevalt */
  val erakonnad: Array<ErakonnaTulemusJs>,
  /** kõik valitud kandidaadid erakonna ja üleriigilise nimekirja järjekorras */
  val valitud: Array<KandidaatJs>,
  val ringkonnad: Array<RingkonnaArvutusJs>,
  val kompensatsioon: Array<KompensatsiooniSammJs>,
)

@JsExport
class KandidaadiSagedusJs(val nimi: String, val erakond: String, val ringkond: Int, val kordi: Int)

@JsExport
class KokkuvõteJs(
  val kordi: Int,
  /** erakonnad, kes said vähemalt ühes korduses mandaadi */
  val erakonnad: Array<String>,
  /** mandaadid[i][k] on erakonna erakonnad[i] mandaatide arv korduses k */
  val mandaadid: Array<Array<Int>>,
  /** ringkondadeMandaadid[r][i][k] on erakonna erakonnad[i] ringkonnamandaatide arv ringkonnas r + 1 korduses k */
  val ringkondadeMandaadid: Array<Array<Array<Int>>>,
  /** ringkondadeValitud[r][i][k] on ringkonnast r + 1 valitud erakonna erakonnad[i] kandidaatide arv korduses k (ka kompensatsioonimandaadid) */
  val ringkondadeValitud: Array<Array<Array<Int>>>,
  /** vähemalt korra valitud kandidaadid valituks osutumise kordade järgi kahanevalt */
  val kandidaadid: Array<KandidaadiSagedusJs>,
)

private fun ringkondadeMassiiv(
  andmed: Map<Int, Map<String, List<Int>>>,
  erakonnad: List<String>,
  kordi: Int,
): Array<Array<Array<Int>>> = valimisringkonnad.map { ringkond ->
  val ringkonnaAndmed = andmed[ringkond.nr] ?: emptyMap()
  erakonnad.map { erakond -> (ringkonnaAndmed[erakond] ?: List(kordi) { 0 }).toTypedArray() }.toTypedArray()
}.toTypedArray()

/** Korduv simulatsioon, mida saab jätkata osade kaupa. */
@JsExport
class KorduvSimulatsioonJs internal constructor(private val simulatsioon: KorduvSimulatsioon) {
  val kordi: Int get() = simulatsioon.kordi

  fun korda(lisakordi: Int) {
    require(lisakordi > 0) { "Korduste arv peab olema positiivne" }
    simulatsioon.korda(lisakordi)
  }

  fun kokkuvõte(): KokkuvõteJs {
    val k = simulatsioon.kokkuvõte()
    val erakonnadMandaatidega = k.erakondadeMandaadid.keys.sorted()
    return KokkuvõteJs(
      kordi = k.kordi,
      erakonnad = erakonnadMandaatidega.toTypedArray(),
      mandaadid = erakonnadMandaatidega.map { k.erakondadeMandaadid[it]!!.toTypedArray() }.toTypedArray(),
      ringkondadeMandaadid = ringkondadeMassiiv(k.ringkondadeMandaadid, erakonnadMandaatidega, k.kordi),
      ringkondadeValitud = ringkondadeMassiiv(k.ringkondadeValitud, erakonnadMandaatidega, k.kordi),
      kandidaadid = k.valitudKordi.entries
        .sortedWith(compareBy({ -it.value }, { it.key.nimi }))
        .map { (v, kordi) -> KandidaadiSagedusJs(v.nimi, v.erakond, v.valimisringkond, kordi) }
        .toTypedArray(),
    )
  }
}

/**
 * Simulaator veebilehe jaoks. Sisendiks on samad andmefailid, mida loeb käsurea programm.
 *
 * @param ringkondadeTekstid - ringkondade 1–12 kandidaatide failide (ringkond1.txt jne) tekstid järjekorras
 * @param üleriigilisedNimekirjad - faili yleriigiline.txt tekst
 * @param rk2023Tulemus - faili rk2023/tulemus.json tekst
 */
@JsExport
class Simulaator(ringkondadeTekstid: Array<String>, üleriigilisedNimekirjad: String, rk2023Tulemus: String) {
  private val kandidaadid: List<Kandidaat>
  private val tulemus2023 = loeRk2023Tulemus(rk2023Tulemus)
  private val ringkonnad = valimisringkonnad.toList()

  init {
    require(ringkondadeTekstid.size == ringkonnad.size) {
      "Ootasin ${ringkonnad.size} ringkonna kandidaatide faili, sain ${ringkondadeTekstid.size}"
    }
    kandidaadid = loeKandidaadid(
      ringkonnad.mapIndexed { i, ringkond -> ringkond.nr to ringkondadeTekstid[i] }.toMap(), üleriigilisedNimekirjad
    )
  }

  /** erakondade nimekirjad (üksikkandidaatideta) kandidaatide koondnimekirja järjekorras */
  fun erakonnad(): Array<ErakondJs> =
    kandidaadid.filter { it.erakond != ÜKSIKKANDIDAADID }
      .groupBy { it.erakond }
      .map { (nimi, erakonnaKandidaadid) -> ErakondJs(nimi, erakonnaKandidaadid.minOf { it.regNr }) }
      .sortedBy { it.kohtKoondnimekirjas }
      .toTypedArray()

  fun ringkonnad(): Array<RingkondJs> =
    ringkonnad.map { RingkondJs(it.nr, it.nimetus, it.mandaatideArv, andmed.kehtivaidHääli[it.nr]!!) }.toTypedArray()

  /** Norstati reitingud reitingud.ee JSON-failist */
  fun norstatiToetus(json: String): ToetusJs = toetusJs(reitingudToetusprotsentidena(loeReitingud(json)))

  /** Emori reitingud failist Andmed.kt */
  fun emoriToetus(): ToetusJs = toetusJs(erakonnad.associate { it.nimetus to it.toetusprotsentValimisringkonnas })

  /** 2023. aasta valimiste tegelikud häälte osakaalud */
  fun rk2023Toetus(): ToetusJs = toetusJs(rk2023Toetusprotsendid(tulemus2023))

  /** erakonna üleriigiline toetus (ringkondade toetuste kaalutud keskmine kehtivate häälte järgi) */
  fun üleriigilineToetus(toetus: ToetusJs, erakond: String): Double =
    üleriigilineToetus(toetusMap(toetus)[erakond] ?: throw IllegalArgumentException("Erakonna $erakond toetus puudub"))

  /** Muudab erakonna üleriigilist toetust; vt [muudaÜleriigilistToetust]. */
  fun muudaÜleriigilistToetust(toetus: ToetusJs, erakond: String, uusToetus: Double): ToetusJs =
    toetusJs(muudaÜleriigilistToetust(toetusMap(toetus), erakond, uusToetus), toetus.erakonnad.toList())

  /**
   * Kontrollib toetusprotsente. Tagastab vigade loendi (tühi, kui vigu pole): negatiivne toetus või
   * ringkonnas erakondade toetuste summa üle 100%.
   */
  fun kontrolliToetust(toetus: ToetusJs): Array<String> {
    val vead = mutableListOf<String>()
    for ((j, ringkond) in ringkonnad.withIndex()) {
      val summa = toetus.protsendid.sumOf { it[j] }
      if (summa > 100.0 + 1e-9) vead.add("Ringkonnas ${ringkond.nr} on toetuste summa ${(summa * 10).toInt() / 10.0}%")
      for ((i, erakond) in toetus.erakonnad.withIndex()) {
        if (toetus.protsendid[i][j] < 0.0) vead.add("Erakonna $erakond toetus ringkonnas ${ringkond.nr} on negatiivne")
      }
    }
    return vead.toTypedArray()
  }

  /** Simuleerib hääled ja jagab mandaadid. */
  fun jaota(toetus: ToetusJs, seeme: Int): TulemusJs {
    kontrolli(toetus)
    val korduseKandidaadid = kandidaadid.map { it.copy() }
    simuleeriHääled(korduseKandidaadid, toetusMap(toetus), Random(seeme))
    return tulemusJs(jaotaMandaadid(korduseKandidaadid, ringkonnad, andmed.kehtivaidHääli), andmed.kehtivaidHääli)
  }

  /** Jagab mandaadid 2023. aasta valimiste tegelike häälte järgi. */
  fun jaotaTegelikeHäältega(): TulemusJs =
    tulemusJs(
      jaotaMandaadid(kandidaadidTegelikeHäältega(kandidaadid, tulemus2023), ringkonnad, tulemus2023.kehtivaidHääli),
      tulemus2023.kehtivaidHääli
    )

  /** Alustab korduvat simulatsiooni; kordused lisatakse meetodiga [KorduvSimulatsioonJs.korda]. */
  fun korduv(toetus: ToetusJs, seeme: Int): KorduvSimulatsioonJs {
    kontrolli(toetus)
    return KorduvSimulatsioonJs(KorduvSimulatsioon(kandidaadid, toetusMap(toetus), Random(seeme)))
  }

  private fun kontrolli(toetus: ToetusJs) {
    val vead = kontrolliToetust(toetus)
    require(vead.isEmpty()) { vead.joinToString("; ") }
  }

  private fun toetusMap(toetus: ToetusJs): Map<String, Map<Int, Double>> {
    require(toetus.protsendid.size == toetus.erakonnad.size) { "Toetuste ja erakondade arv erinevad" }
    return toetus.erakonnad.withIndex().associate { (i, erakond) ->
      val protsendid = toetus.protsendid[i]
      require(protsendid.size == ringkonnad.size) { "Erakonna $erakond toetus peab olema antud kõigis ringkondades" }
      erakond to ringkonnad.mapIndexed { j, ringkond -> ringkond.nr to protsendid[j] }.toMap()
    }
  }

  /**
   * Erakonnad jäävad andmeallika järjekorda. Häälte simuleerimine kasutab juhuslikkust erakondade kaupa
   * selles järjekorras, seega annab sama seeme sama tulemuse kui käsurea programm.
   */
  private fun toetusJs(toetus: Map<String, Map<Int, Double>>, järjekord: List<String>? = null): ToetusJs {
    val nimed = järjekord ?: toetus.keys.toList()
    return ToetusJs(
      nimed.toTypedArray(),
      nimed.map { erakond -> ringkonnad.map { toetus[erakond]?.get(it.nr) ?: 0.0 }.toTypedArray() }.toTypedArray()
    )
  }

  private fun tulemusJs(jaotus: Mandaadijaotus, kehtivaidHääli: Map<Int, Int>): TulemusJs {
    val ringkonnamandaadid = jaotus.ringkonnamandaadid.toSet()
    val isikumandaadid = jaotus.ringkonnaArvutused.flatMap { it.isikumandaadid }.toSet()
    fun liik(k: Kandidaat): String? = when (k) {
      in isikumandaadid -> ISIKUMANDAAT
      in ringkonnamandaadid -> RINGKONNAMANDAAT
      in jaotus.kompensatsioonimandaadid -> KOMPENSATSIOONIMANDAAT
      else -> null
    }
    fun js(k: Kandidaat) = KandidaatJs(k.nimi, k.erakond, k.valimisringkond, k.hääli, k.jrkNr, k.regNr, liik(k))

    val kehtivaidKokku = kehtivaidHääli.values.sum().toDouble()
    val nimekirjad = (jaotus.erakondadeHääled.keys + jaotus.kõikMandaadid().map { it.erakond })
      .distinct()
      .filter { (jaotus.erakondadeHääled[it] ?: 0) > 0 || jaotus.kõikMandaadid().any { k -> k.erakond == it } }
    val erakonnad = nimekirjad.map { erakond ->
      val hääli = jaotus.erakondadeHääled[erakond] ?: 0
      ErakonnaTulemusJs(
        erakond = erakond,
        hääli = hääli,
        protsent = hääli * 100.0 / kehtivaidKokku,
        ületasKünnise = erakond in jaotus.künniseÜletanud,
        ringkonnamandaate = jaotus.ringkonnamandaadid.count { it.erakond == erakond },
        kompensatsioonimandaate = jaotus.kompensatsioonimandaadid.count { it.erakond == erakond },
      )
    }.sortedByDescending { it.hääli }

    return TulemusJs(
      erakonnad = erakonnad.toTypedArray(),
      valitud = jaotus.kõikMandaadid().map(::js).toTypedArray(),
      ringkonnad = jaotus.ringkonnaArvutused.map { r ->
        RingkonnaArvutusJs(
          r.ringkond, r.kehtivaidHääli, r.mandaate, r.lihtkvoot,
          r.erakonnad.map { e ->
            ErakonnaRingkonnaArvutusJs(
              e.erakond, e.hääli, e.kvoote, e.mandaate, e.lisamandaat,
              e.valitud.map(::js).toTypedArray(), e.alla10Protsendi.map(::js).toTypedArray()
            )
          }.toTypedArray(),
          r.isikumandaadid.map(::js).toTypedArray(),
        )
      }.toTypedArray(),
      kompensatsioon = jaotus.kompensatsiooniSammud.map { s ->
        val nimed = s.võrdlusarvud.keys.toList()
        KompensatsiooniSammJs(
          nimed.toTypedArray(),
          nimed.map { s.võrdlusarvud[it]!! }.toTypedArray(),
          nimed.map { s.jubaMandaate[it] ?: 0 }.toTypedArray(),
          s.erakond, js(s.kandidaat), s.viik, s.varureegel,
        )
      }.toTypedArray(),
    )
  }
}
