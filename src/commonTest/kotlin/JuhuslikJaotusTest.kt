import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class JuhuslikJaotusTest {

  @Test
  fun `arv jagatakse osadeks, mille summa on algne arv`() {
    val osad = jagaJuhuslikeksOsadeks(1000, 5, Random(1))
    assertEquals(5, osad.size)
    assertEquals(1000, osad.sum())
  }

  @Test
  fun `null osaks jagamine annab tühja loendi`() {
    assertEquals(emptyList(), jagaJuhuslikeksOsadeks(1000, 0, Random(1)))
  }
}
