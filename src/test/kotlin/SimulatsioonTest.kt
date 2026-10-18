import andmed.PAREMPOOLSED
import andmed.ROHELISED
import andmed.valimisringkonnad
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

  @Test
  fun `Norstati andmetest loetakse kõigi kaheksa erakonna toetus kõigis ringkondades`() {
    val toetus = loeToetusprotsendid(Küsitleja.NORSTAT)
    assertEquals(8, toetus.size)
    for (erakond in listOf(ROHELISED, PAREMPOOLSED)) {
      assertEquals(valimisringkonnad.map { it.nr }.toSet(), toetus[erakond]!!.keys, erakond)
    }
  }

  @Test
  fun `reitingufaili saab valida`() {
    // 1. ringkonnas oli Keskerakonna toetus 27.02 failis 27,6% ja 03.03 failis 25,6%
    val fail = "src/main/resources/reitingud/reitingud-2023-02-27.json"
    assertEquals(27.6, loeToetusprotsendid(Küsitleja.NORSTAT, fail)[andmed.KESKERAKOND]!![1])
    assertEquals(25.6, loeToetusprotsendid(Küsitleja.NORSTAT)[andmed.KESKERAKOND]!![1])
  }

  @Test
  fun `korduv simulatsioon loeb iga kandidaadi valituks osutumise kordi`() {
    val kokkuvõte = simuleeriKorduvalt(Küsitleja.EMOR, kordi = 3, juhus = Random(1))
    assertEquals(3, kokkuvõte.kordi)
    assertEquals(3 * 101, kokkuvõte.valitudKordi.values.sum())
    assertEquals(true, kokkuvõte.valitudKordi.values.all { it in 1..3 })
    for ((erakond, mandaadid) in kokkuvõte.erakondadeMandaadid) {
      assertEquals(3, mandaadid.size, erakond)
    }
    assertEquals(List(3) { 101 }, (0 until 3).map { i -> kokkuvõte.erakondadeMandaadid.values.sumOf { it[i] } })
  }

  @Test
  fun `korduva simulatsiooni esimene kord on sama mis üksik simulatsioon sama seemnega`() {
    val üksik = simuleeriValimised(Küsitleja.EMOR, Random(1)).kõikMandaadid().map { it.nimi }.toSet()
    val korduv = simuleeriKorduvalt(Küsitleja.EMOR, kordi = 1, juhus = Random(1))
    assertEquals(üksik, korduv.valitudKordi.keys.map { it.nimi }.toSet())
  }

  private fun loeKuldneFail(nimi: String): String =
    SimulatsioonTest::class.java.getResource("/kuldsed/$nimi")!!.readText()
}
