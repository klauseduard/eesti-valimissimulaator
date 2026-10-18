import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Kuldsed failid on salvestatud käsuga `./gradlew run --args='--seed=N'` (Emori puhul lisaks
 * `--pollster=emor`). Test kontrollib, et sama seemne korral annab programm sama tulemuse.
 * Kui mandaatide jagamise reeglid muutuvad teadlikult, tuleb failid uuesti salvestada.
 * Viimati salvestatud 2026-10-18 pärast registreerimisnumbri lisamist kandidaadi andmetesse.
 */
class SimulatsioonTest {

  @Test
  fun `Norstati andmetega simulatsioon annab seemne korral salvestatud tulemuse`() {
    for (seeme in listOf(1L, 2L)) {
      val jaotus = simuleeriValimised(Küsitleja.NORSTAT, Random(seeme))
      assertEquals(loeKuldneFail("norstat-seed$seeme.txt"), mandaadidTekstina(jaotus), "seeme $seeme")
    }
  }

  @Test
  fun `Emori andmetega simulatsioon annab seemne korral salvestatud tulemuse`() {
    for (seeme in listOf(1L, 2L)) {
      val jaotus = simuleeriValimised(Küsitleja.EMOR, Random(seeme))
      assertEquals(loeKuldneFail("emor-seed$seeme.txt"), mandaadidTekstina(jaotus), "seeme $seeme")
    }
  }

  private fun loeKuldneFail(nimi: String): String =
    SimulatsioonTest::class.java.getResource("/kuldsed/$nimi")!!.readText()
}
