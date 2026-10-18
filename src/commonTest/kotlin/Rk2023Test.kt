import andmed.kandidaadidTegelikeHäältega
import andmed.valimisringkonnad
import mudel.Kandidaat
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Võrdleb mandaatide jagamist 2023. aasta Riigikogu valimiste ametliku tulemusega.
 * Sisendiks on kandidaatide tegelikud hääled failist src/jvmMain/resources/rk2023/tulemus.json,
 * mis on loodud ametlikust failist RESULTS.xml (vt src/jvmTest/resources/rk2023/LOEMIND.md).
 * Test käib nii JVM-il kui ka Node'is, seega kontrollib see ka JavaScripti versiooni.
 */
class Rk2023Test {

  private val tulemus = Testiandmed.rk2023
  private val jaotus by lazy {
    jaotaMandaadid(
      kandidaadidTegelikeHäältega(Testiandmed.kandidaadid(), tulemus), valimisringkonnad.toList(), tulemus.kehtivaidHääli
    )
  }

  private fun ametlikud(vararg liigid: String) = tulemus.kandidaadid.filter { it.mandaat in liigid }

  @Test
  fun `ringkonnamandaatide arv erakonniti vastab ametlikule tulemusele`() {
    val ametlik = ametlikud("PERSONAL", "DISTRICT")
      .groupingBy { "ringkond ${it.ringkond}: ${it.erakond}" }.eachCount()
      .map { (võti, mandaate) -> "$võti = $mandaate" }.toSet()
    val arvutatud = jaotus.ringkonnamandaatideArv
      .flatMap { (ringkond, erakonnad) -> erakonnad.filterValues { it > 0 }.map { (erakond, mandaate) ->
        "ringkond $ringkond: $erakond = $mandaate" } }
      .toSet()
    assertSamaSisu(ametlik, arvutatud)
  }

  @Test
  fun `ringkonnamandaadi saanud kandidaadid vastavad ametlikule tulemusele`() {
    val ametlik = ametlikud("PERSONAL", "DISTRICT").map { "${it.nimi} (${it.erakond}, ringkond ${it.ringkond})" }.toSet()
    assertSamaSisu(ametlik, jaotus.ringkonnamandaadid.map { it.võti() }.toSet())
  }

  @Test
  fun `isikumandaadi saanud kandidaadid vastavad ametlikule tulemusele`() {
    val kvoodid = valimisringkonnad.associate { it.nr to lihtkvoot(tulemus.kehtivaidHääli[it.nr]!!, it.mandaatideArv) }
    val ametlik = ametlikud("PERSONAL").map { "${it.nimi} (${it.erakond}, ringkond ${it.ringkond})" }.toSet()
    val arvutatud = jaotus.ringkonnamandaadid.filter { it.hääli >= kvoodid[it.valimisringkond]!! }.map { it.võti() }.toSet()
    assertSamaSisu(ametlik, arvutatud)
  }

  @Test
  fun `kompensatsioonimandaatide arv erakonniti vastab ametlikule tulemusele`() {
    val ametlik = ametlikud("COMPENSATION").groupingBy { it.erakond }.eachCount()
      .map { (erakond, mandaate) -> "$erakond = $mandaate" }.toSet()
    val arvutatud = jaotus.kompensatsioonimandaadid.groupingBy { it.erakond }.eachCount()
      .map { (erakond, mandaate) -> "$erakond = $mandaate" }.toSet()
    assertSamaSisu(ametlik, arvutatud)
  }

  @Test
  fun `kompensatsioonimandaadi saanud kandidaadid vastavad ametlikule tulemusele`() {
    val ametlik = ametlikud("COMPENSATION").map { "${it.nimi} (${it.erakond}, ringkond ${it.ringkond})" }.toSet()
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
