import andmed.PAREMPOOLSED
import andmed.ROHELISED
import andmed.valimisringkonnad
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** Käsurea programmi andmeallikate ja korduva simulatsiooni testid (kuldsed failid: KuldsedFailidTest). */
class SimulatsioonTest {

  @Test
  fun `küsitluse viga on usaldusväärne kuni kahe nädala horisondini`() {
    assertEquals(true, küsitluseViga(Küsitleja.NORSTAT, "reitingud-2023-02-20.json").üleriigiline > 0.0)
    kotlin.test.assertFailsWith<IllegalArgumentException> { küsitluseViga(Küsitleja.NORSTAT, "reitingud-2023-01-15.json") }
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
    val fail = "src/jvmMain/resources/reitingud/reitingud-2023-02-27.json"
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
}
