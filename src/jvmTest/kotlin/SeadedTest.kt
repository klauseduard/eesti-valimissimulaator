import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SeadedTest {

  @Test
  fun `ilma argumentideta on üks Norstati simulatsioon juhusliku seemnega`() {
    assertEquals(Seaded(Küsitleja.NORSTAT, seeme = null, kordi = null, reitingufail = null), loeSeaded(emptyArray()))
  }

  @Test
  fun `--runs=1 küsib sageduste faili ühe korduse kohta`() {
    assertEquals(1, loeSeaded(arrayOf("--runs=1")).kordi)
  }

  @Test
  fun `kõik argumendid loetakse`() {
    val seaded = loeSeaded(arrayOf("--pollster=emor", "--seed=7", "--runs=100", "--reitingud=fail.json"))
    assertEquals(Seaded(Küsitleja.EMOR, seeme = 7L, kordi = 100, reitingufail = "fail.json"), seaded)
  }

  @Test
  fun `küsitluse viga on vaikimisi arvestatud ja selle saab välja lülitada`() {
    assertEquals(true, loeSeaded(arrayOf("--runs=10")).arvestaViga)
    assertEquals(false, loeSeaded(arrayOf("--runs=10", "--ilma-veata")).arvestaViga)
  }

  @Test
  fun `häälte jaotuse saab valida`() {
    assertEquals(JAOTUS_KALIBREERITUD, loeSeaded(emptyArray()).jaotus)
    assertEquals(JAOTUS_LIHTNE, loeSeaded(arrayOf("--jaotus=lihtne")).jaotus)
    assertFailsWith<IllegalArgumentException> { loeSeaded(arrayOf("--jaotus=muu")) }
  }

  @Test
  fun `tundmatu argument annab vea`() {
    assertFailsWith<IllegalArgumentException> { loeSeaded(arrayOf("--tundmatu")) }
  }

  @Test
  fun `korduste arv peab olema positiivne`() {
    assertFailsWith<IllegalArgumentException> { loeSeaded(arrayOf("--runs=0")) }
  }
}
