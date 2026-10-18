import andmed.AndmeFailideLugeja
import andmed.ÜKSIKKANDIDAADID
import andmed.valimisringkonnad
import mudel.Kandidaat
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Võrdleb mandaatide jagamist 2023. aasta Riigikogu valimiste ametliku tulemusega.
 * Sisendiks on kandidaatide tegelikud hääled; vt src/test/resources/rk2023/LOEMIND.md.
 */
class Rk2023Test {

  private val jaotus by lazy {
    jaotaMandaadid(Rk2023.kandidaadid, valimisringkonnad.toList(), Rk2023.kehtivaidHääli)
  }

  @Test
  fun `lihtkvoot vastab ametlikus failis kandidaatidele arvutatud osakaalule`() {
    for (k in Rk2023.tulemused) {
      val ringkond = valimisringkonnad.single { it.nr == k.kandidaat.valimisringkond }
      val osakaal = k.kandidaat.hääli / lihtkvoot(Rk2023.kehtivaidHääli[ringkond.nr]!!, ringkond.mandaatideArv)
      assertTrue(abs(osakaal - k.kvoodiOsakaal) < 0.00005, "${k.kandidaat}: $osakaal != ${k.kvoodiOsakaal}")
    }
  }

  @Test
  fun `ringkonnamandaatide arv erakonniti vastab ametlikule tulemusele`() {
    val ametlik = Rk2023.tulemused
      .filter { it.mandaadiLiik == "PERSONAL" || it.mandaadiLiik == "DISTRICT" }
      .groupingBy { "ringkond ${it.kandidaat.valimisringkond}: ${it.kandidaat.erakond}" }.eachCount()
      .map { (võti, mandaate) -> "$võti = $mandaate" }.toSet()
    val arvutatud = jaotus.ringkonnamandaatideArv
      .flatMap { (ringkond, erakonnad) -> erakonnad.filterValues { it > 0 }.map { (erakond, mandaate) ->
        "ringkond $ringkond: $erakond = $mandaate" } }
      .toSet()
    assertSamaSisu(ametlik, arvutatud)
  }

  @Test
  fun `ringkonnamandaadi saanud kandidaadid vastavad ametlikule tulemusele`() {
    val ametlik = Rk2023.tulemused
      .filter { it.mandaadiLiik == "PERSONAL" || it.mandaadiLiik == "DISTRICT" }
      .map { it.kandidaat.võti() }.toSet()
    assertSamaSisu(ametlik, jaotus.ringkonnamandaadid.map { it.võti() }.toSet())
  }

  @Test
  fun `kompensatsioonimandaatide arv erakonniti vastab ametlikule tulemusele`() {
    val ametlik = Rk2023.tulemused
      .filter { it.mandaadiLiik == "COMPENSATION" }
      .groupingBy { it.kandidaat.erakond }.eachCount()
      .map { (erakond, mandaate) -> "$erakond = $mandaate" }.toSet()
    val arvutatud = jaotus.kompensatsioonimandaadid.groupingBy { it.erakond }.eachCount()
      .map { (erakond, mandaate) -> "$erakond = $mandaate" }.toSet()
    assertSamaSisu(ametlik, arvutatud)
  }

  @Test
  fun `kompensatsioonimandaadi saanud kandidaadid vastavad ametlikule tulemusele`() {
    val ametlik = Rk2023.tulemused
      .filter { it.mandaadiLiik == "COMPENSATION" }
      .map { it.kandidaat.võti() }.toSet()
    assertSamaSisu(ametlik, jaotus.kompensatsioonimandaadid.map { it.võti() }.toSet())
  }

  private fun Kandidaat.võti() = "$nimi ($erakond, ringkond $valimisringkond)"

  /** Võrdleb hulki ja näitab vea korral ainult erinevusi. */
  private fun assertSamaSisu(ametlik: Set<String>, arvutatud: Set<String>) {
    val puudu = (ametlik - arvutatud).sorted()
    val üleliigsed = (arvutatud - ametlik).sorted()
    assertTrue(puudu.isEmpty() && üleliigsed.isEmpty(),
      "\nametlikus tulemuses, aga arvutatud tulemusest puudu:\n  " + puudu.joinToString("\n  ") +
      "\narvutatud tulemuses, aga ametlikus tulemuses puudu:\n  " + üleliigsed.joinToString("\n  "))
  }
}

/** Ametliku tulemuse rida: kandidaat koos häältega ja valituks osutumise korral mandaadi liik. */
data class AmetlikTulemus(val kandidaat: Kandidaat, val kvoodiOsakaal: Double, val mandaadiLiik: String?)

object Rk2023 {
  val tulemused: List<AmetlikTulemus>
  val kehtivaidHääli: Map<Int, Int>

  val kandidaadid: List<Kandidaat> get() = tulemused.map { it.kandidaat.copy() }

  init {
    val dokument = Rk2023::class.java.getResourceAsStream("/rk2023/RESULTS.xml")!!.use {
      DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().parse(it)
    }
    val koond = dokument.documentElement.esimene("data").esimene("electionResult")

    val ridu = mutableListOf<AmetlikTulemus>()
    val häältejaotus = koond.esimene("votesAndMandates")
    for (nimekiri in häältejaotus.lapsed()) {
      val erakond = when (nimekiri.localName) {
        "party" -> nimekiri.tekst("name")
        "independentCandidates" -> ÜKSIKKANDIDAADID
        else -> continue
      }
      for (k in nimekiri.esimene("candidates").lapsed("candidate")) {
        val kandidaat = Kandidaat(
          nimi = k.tekst("forename") + " " + k.tekst("surname"),
          erakond = erakond,
          valimisringkond = k.tekst("districtNumber").toInt(),
          hääli = k.tekst("votes").toInt(),
          regNr = k.tekst("registrationNumber").toInt(),
        )
        val liik = k.lapsed("mandateType").firstOrNull()?.textContent
        ridu.add(AmetlikTulemus(kandidaat, k.tekst("quota").toDouble(), liik))
      }
    }

    val kandidaadid = ridu.map { it.kandidaat }
    AndmeFailideLugeja().määraÜleriigilisedJärjekorranumbrid(kandidaadid)
    val järjekorrata = kandidaadid.filter { it.erakond != ÜKSIKKANDIDAADID && it.jrkNr == 0 }
    check(järjekorrata.isEmpty()) { "Üleriigilisest nimekirjast puuduvad: $järjekorrata" }
    check(ridu.size == 968) { "Ootasin 968 kandidaati, leidsin ${ridu.size}" }
    check(ridu.count { it.mandaadiLiik != null } == 101) { "Ootasin 101 valitud kandidaati" }
    tulemused = ridu

    kehtivaidHääli = koond.esimene("districts").lapsed("district").associate {
      it.tekst("number").toInt() to it.esimene("statistics").tekst("votes").toInt()
    }
  }

  private fun Element.lapsed(nimi: String? = null): List<Element> =
    (0 until childNodes.length).map { childNodes.item(it) }
      .filterIsInstance<Element>()
      .filter { nimi == null || it.localName == nimi }

  private fun Element.esimene(nimi: String): Element = lapsed(nimi).first()

  private fun Element.tekst(nimi: String): String = esimene(nimi).textContent.trim()
}
