import kotlin.test.Test
import kotlin.test.assertEquals

class RingkonnamandaadidTest {

  @Test
  fun `lihtkvoot on kehtivate häälte arv jagatud ringkonna mandaatide arvuga`() {
    assertEquals(6161.0, lihtkvoot(kehtivaidHääli = 61610, mandaatideArv = 10))
  }

  @Test
  fun `erakond saab mandaadi iga täis lihtkvoodi eest`() {
    val mandaadid = arvutaRingkonnaMandaadid(mapOf("A" to 350), kehtivaidHääli = 1000, mandaatideArv = 10)
    assertEquals(3, mandaadid["A"])
  }

  @Test
  fun `jääk vähemalt 0,75 lihtkvoodist annab lisamandaadi`() {
    val mandaadid = arvutaRingkonnaMandaadid(mapOf("B" to 275), kehtivaidHääli = 1000, mandaatideArv = 10)
    assertEquals(3, mandaadid["B"])
  }

  @Test
  fun `jääk alla 0,75 lihtkvoodi lisamandaati ei anna`() {
    val mandaadid = arvutaRingkonnaMandaadid(mapOf("C" to 74), kehtivaidHääli = 1000, mandaatideArv = 10)
    assertEquals(0, mandaadid["C"])
  }
}
