import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Kuldsed failid on salvestatud käsuga `./gradlew run --args='--seed=N'` (Emori puhul lisaks
 * `--pollster=emor`). Test kontrollib, et sama seemne korral annab programm sama tulemuse.
 * Test käib nii JVM-il kui ka Node'is, seega annab JavaScripti versioon sama tulemuse kui käsurea programm.
 * Kui mandaatide jagamise reeglid muutuvad teadlikult, tuleb failid uuesti salvestada.
 * Viimati salvestatud 2026-10-18 pärast registreerimisnumbri lisamist kandidaadi andmetesse.
 */
class KuldsedFailidTest {

  @Test
  fun `Norstati andmetega simulatsioon annab seemne korral salvestatud tulemuse`() {
    for (seeme in listOf(1L, 2L)) {
      assertEquals(loeKuldneFail("norstat-seed$seeme.txt"), simuleeri(Testiandmed.norstatiToetus(), seeme), "seeme $seeme")
    }
  }

  @Test
  fun `Emori andmetega simulatsioon annab seemne korral salvestatud tulemuse`() {
    for (seeme in listOf(1L, 2L)) {
      assertEquals(loeKuldneFail("emor-seed$seeme.txt"), simuleeri(Testiandmed.emoriToetus, seeme), "seeme $seeme")
    }
  }

  private fun simuleeri(toetus: Map<String, Map<Int, Double>>, seeme: Long): String {
    val kandidaadid = Testiandmed.kandidaadid()
    simuleeriHääled(kandidaadid, toetus, Random(seeme))
    return mandaadidTekstina(jaotaMandaadid(kandidaadid, andmed.valimisringkonnad.toList(), andmed.kehtivaidHääli))
  }

  private fun loeKuldneFail(nimi: String): String = loeProjektiFail("src/commonTest/resources/kuldsed/$nimi")
}
