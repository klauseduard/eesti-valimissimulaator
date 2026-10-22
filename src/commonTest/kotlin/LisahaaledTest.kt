import mudel.Kandidaat
import mudel.Valimisringkond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Lisatud hääled muudavad kandidaadi hääli ja ringkonna kehtivate häälte arvu. */
class LisahaaledTest {

  // lihtkvoot 300 / 3 = 100: A 210 häält (2 kohta), B 80 (0,8 kvooti -> 1 koht jäägist)
  private val ringkonnad = listOf(Valimisringkond(1, "Esimene", 3))
  private val kehtivaidHääli = mapOf(1 to 300)
  private val kandidaadid = listOf(
    Kandidaat("a1", "A", 1, hääli = 150, jrkNr = 1, regNr = 101),
    Kandidaat("a2", "A", 1, hääli = 60, jrkNr = 2, regNr = 102),
    Kandidaat("b1", "B", 1, hääli = 80, jrkNr = 1, regNr = 201),
  )

  @Test
  fun `ilma lisadeta on tulemus sama mis tavaline jaotus`() {
    val jaotus = jaotaLisatudHäältega(kandidaadid, ringkonnad, kehtivaidHääli, emptyMap())
    assertEquals(jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli), jaotus)
    assertEquals(setOf(101, 102, 201), valitudRegNrid(jaotus))
  }

  @Test
  fun `lisatud hääled tõstavad lihtkvooti ja võivad võtta jäägikoha`() {
    // 60 häält a1-le: kehtivaid 360, lihtkvoot 120; B 80 / 120 = 0,67 < 0,75, seega B kaotab jäägikoha
    val jaotus = jaotaLisatudHäältega(kandidaadid, ringkonnad, kehtivaidHääli, mapOf(101 to 60))
    assertEquals(120.0, jaotus.ringkonnaArvutused.single().lihtkvoot)
    assertEquals(0, jaotus.ringkonnamandaatideArv[1]!!["B"] ?: 0)
    assertEquals(270, jaotus.ringkonnaArvutused.single().erakonnad.single { it.erakond == "A" }.hääli)
  }

  @Test
  fun `tundmatu kandidaat ja negatiivne lisa on vead`() {
    assertFailsWith<IllegalArgumentException> { jaotaLisatudHäältega(kandidaadid, ringkonnad, kehtivaidHääli, mapOf(999 to 1)) }
    assertFailsWith<IllegalArgumentException> { jaotaLisatudHäältega(kandidaadid, ringkonnad, kehtivaidHääli, mapOf(101 to -1)) }
  }
}
