import andmed.AndmeFailideLugeja
import andmed.loeKoondmudel
import koond.hajuvusteSuhted2023
import kotlin.test.Test
import kotlin.test.assertTrue

/** Simulatsiooni üleriigiline hajuvus peab vastama tagasitestis kontrollitud ennustavale hälbele (±15%). */
class HajuvusTest {

  @Test
  fun `simuleeritud hajuvus vastab tagasitesti hälbele`() {
    val lugeja = AndmeFailideLugeja()
    val suhted = hajuvusteSuhted2023(lugeja, loeKoondmudel(lugeja))
    for (kood in listOf("REF", "KESK", "EKRE", "E200", "SDE", "ISAMAA")) {
      val suhe = suhted[kood]!!
      assertTrue(suhe in 0.85..1.15, "$kood: simulatsiooni ja tagasitesti hälbe suhe $suhe")
    }
  }
}
