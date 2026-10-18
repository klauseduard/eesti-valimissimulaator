import mudel.Kandidaat
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JaotusmudelTest {

  private fun nimekiri(n: Int, juhte: Int = 0) =
    (1..n).map { Kandidaat("k$it", "A", 1, jrkNr = if (it <= juhte) it else 10 + it) }

  @Test
  fun `positsioonimudeli ootuspärased osakaalud kahanevad ja summa on 1`() {
    val m = Positsioonimudel(b = 0.8, c = 0.05, d = 0.3, kappa = 10.0).ootuspärasedOsakaalud(nimekiri(6, juhte = 1))
    assertTrue(abs(m.sum() - 1.0) < 1e-12)
    assertTrue(m.zipWithNext().all { (a, b) -> a > b })
  }

  @Test
  fun `riikliku nimekirja esiviisikus kandidaat saab suurema kaalu`() {
    val mudel = Positsioonimudel(b = 0.8, c = 0.05, d = 0.5, kappa = 10.0)
    val ilma = mudel.ootuspärasedOsakaalud(nimekiri(5, juhte = 0))[0]
    val juhiga = mudel.ootuspärasedOsakaalud(nimekiri(5, juhte = 1))[0]
    assertTrue(juhiga > ilma)
  }

  @Test
  fun `jaotatud häälte summa on täpselt erakonna hääled`() {
    for (mudel in listOf(JuhuslikKahanev, Positsioonimudel(0.8, 0.05, 0.3, 10.0))) {
      val juhus = Random(1)
      repeat(50) {
        val hääled = mudel.jaota(12345, nimekiri(9, juhte = 2), juhus)
        assertEquals(12345, hääled.sum(), mudel.toString())
        assertEquals(9, hääled.size)
        assertTrue(hääled.all { it >= 0 })
      }
    }
  }

  @Test
  fun `positsioonimudeli keskmised osakaalud vastavad ootusele`() {
    val mudel = Positsioonimudel(b = 0.8, c = 0.05, d = 0.0, kappa = 8.0)
    val kandidaadid = nimekiri(5)
    val oodatud = mudel.ootuspärasedOsakaalud(kandidaadid)
    val juhus = Random(2)
    val summad = DoubleArray(5)
    val kordi = 4000
    repeat(kordi) { mudel.jaota(100000, kandidaadid, juhus).forEachIndexed { i, h -> summad[i] += h / 100000.0 } }
    for (i in 0 until 5) assertTrue(abs(summad[i] / kordi - oodatud[i]) < 0.01, "positsioon ${i + 1}: ${summad[i] / kordi} vs ${oodatud[i]}")
  }

  @Test
  fun `vana jaotus on endine`() {
    // kuldsed failid sõltuvad sellest, et JuhuslikKahanev annab sama tulemuse kui jagaJuhuslikeksOsadeks
    val a = JuhuslikKahanev.jaota(1000, nimekiri(5), Random(7))
    val b = jagaJuhuslikeksOsadeks(1000, 5, Random(7))
    assertEquals(b, a)
  }
}
