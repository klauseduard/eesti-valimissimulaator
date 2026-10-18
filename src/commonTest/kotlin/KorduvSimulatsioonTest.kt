import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class KorduvSimulatsioonTest {

  private val kokkuvõte by lazy {
    simuleeriKorduvalt(Testiandmed.kandidaadid(), Testiandmed.emoriToetus, kordi = 5, juhus = Random(3))
  }

  @Test
  fun `ringkondade kaupa valitud kandidaatide summa on igas korduses erakonna mandaatide arv`() {
    for ((erakond, mandaadid) in kokkuvõte.erakondadeMandaadid) {
      for (k in 0 until kokkuvõte.kordi) {
        val ringkondadest = kokkuvõte.ringkondadeValitud.values.sumOf { it[erakond]?.get(k) ?: 0 }
        assertEquals(mandaadid[k], ringkondadest, "$erakond korduses $k")
      }
    }
  }

  @Test
  fun `ringkonnast valitute hulgas on vähemalt ringkonnamandaadid`() {
    for ((ringkond, erakonnad) in kokkuvõte.ringkondadeMandaadid) {
      for ((erakond, mandaadid) in erakonnad) {
        for (k in 0 until kokkuvõte.kordi) {
          val valitud = kokkuvõte.ringkondadeValitud[ringkond]?.get(erakond)?.get(k) ?: 0
          assertEquals(true, valitud >= mandaadid[k], "ringkond $ringkond, $erakond, kordus $k")
        }
      }
    }
  }
}
