import andmed.ÜKSIKKANDIDAADID
import mudel.Kandidaat
import mudel.Valimisringkond
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Mandaatide jagamise vahesammud, mida veebileht näitab selgitusena. */
class SelgitusTest {

  // Sama näide mis MandaadijaotusTest: ringkond 1 (lihtkvoot 100) A 210, B 80; ringkond 2 (lihtkvoot 100) A 50, B 150.
  private val ringkonnad = listOf(Valimisringkond(1, "Esimene", 3), Valimisringkond(2, "Teine", 2))
  private val kehtivaidHääli = mapOf(1 to 300, 2 to 200)
  private val kandidaadid = listOf(
    Kandidaat("a1", "A", 1, hääli = 150, jrkNr = 1, regNr = 101),
    Kandidaat("a2", "A", 1, hääli = 60, jrkNr = 3, regNr = 102),
    Kandidaat("a3", "A", 2, hääli = 50, jrkNr = 2, regNr = 103),
    Kandidaat("b1", "B", 1, hääli = 80, jrkNr = 2, regNr = 201),
    Kandidaat("b2", "B", 2, hääli = 120, jrkNr = 1, regNr = 202),
    Kandidaat("b3", "B", 2, hääli = 30, jrkNr = 3, regNr = 203),
  )

  private fun lähedal(oodatud: Double, tegelik: Double) = assertTrue(abs(oodatud - tegelik) < 0.05, "$oodatud != $tegelik")

  @Test
  fun `ringkonna arvutus näitab lihtkvooti, erakondade hääli ja 0,75 reeglit`() {
    val arvutus = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 6).ringkonnaArvutused.first()
    assertEquals(1, arvutus.ringkond)
    assertEquals(100.0, arvutus.lihtkvoot)
    val a = arvutus.erakonnad.single { it.erakond == "A" }
    assertEquals(210, a.hääli)
    lähedal(2.1, a.kvoote)
    assertEquals(2, a.mandaate)
    assertEquals(false, a.lisamandaat)
    assertEquals(listOf("a1", "a2"), a.valitud.map { it.nimi })
    val b = arvutus.erakonnad.single { it.erakond == "B" }
    assertEquals(1, b.mandaate)
    assertEquals(true, b.lisamandaat)
  }

  @Test
  fun `kompensatsioonimandaatide sammud näitavad võrdlusarve ja valitud kandidaati`() {
    val sammud = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 6).kompensatsiooniSammud
    assertEquals(listOf("A" to "a3", "B" to "b3"), sammud.map { it.erakond to it.kandidaat.nimi })
    // A: 260 / 3^0,9 = 96,7 ja B: 230 / 3^0,9 = 85,6
    lähedal(96.7, sammud[0].võrdlusarvud["A"]!!)
    lähedal(85.6, sammud[0].võrdlusarvud["B"]!!)
    assertEquals(2, sammud[0].jubaMandaate["A"])
    assertEquals(false, sammud[0].viik)
    assertEquals(false, sammud[0].varureegel)
  }

  @Test
  fun `kandidaadid, kes jäid 10 protsendi nõude tõttu ringkonnamandaadita, on selgituses`() {
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 200, jrkNr = 1),
      Kandidaat("a2", "A", 1, hääli = 5, jrkNr = 2),
      Kandidaat("b1", "B", 1, hääli = 95, jrkNr = 1),
    )
    val jaotus = jaotaMandaadid(kandidaadid, listOf(Valimisringkond(1, "Esimene", 3)), mapOf(1 to 300), mandaateKokku = 3)
    val a = jaotus.ringkonnaArvutused.single().erakonnad.single { it.erakond == "A" }
    assertEquals(2, a.mandaate)
    assertEquals(listOf("a1"), a.valitud.map { it.nimi })
    assertEquals(listOf("a2"), a.alla10Protsendi.map { it.nimi })
  }

  @Test
  fun `isikumandaadid ja künnis on selgituses`() {
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 2, hääli = 3000, jrkNr = 1),
      Kandidaat("x1", ÜKSIKKANDIDAADID, 1, hääli = 100),
    )
    val ringkonnad = listOf(Valimisringkond(1, "Esimene", 1), Valimisringkond(2, "Teine", 1))
    val jaotus = jaotaMandaadid(kandidaadid, ringkonnad, mapOf(1 to 100, 2 to 3000), mandaateKokku = 2)
    assertEquals(listOf("x1"), jaotus.ringkonnaArvutused.first { it.ringkond == 1 }.isikumandaadid.map { it.nimi })
    assertEquals(listOf("a1"), jaotus.ringkonnaArvutused.first { it.ringkond == 2 }.isikumandaadid.map { it.nimi })
    assertEquals(setOf("A"), jaotus.künniseÜletanud)
    assertEquals(3000, jaotus.erakondadeHääled["A"])
  }

  @Test
  fun `varureegel ja viik on sammudes märgitud`() {
    // varureegel: kõik vabad kandidaadid jäävad alla 5% lihtkvoodist (vt MandaadijaotusTest)
    val varu = jaotaMandaadid(
      listOf(
        Kandidaat("a1", "A", 1, hääli = 280, jrkNr = 1),
        Kandidaat("a3", "A", 1, hääli = 3, jrkNr = 2),
        Kandidaat("a2", "A", 1, hääli = 4, jrkNr = 3),
      ),
      listOf(Valimisringkond(1, "Esimene", 3)), mapOf(1 to 300), mandaateKokku = 3
    ).kompensatsiooniSammud
    assertEquals(listOf(true, true), varu.map { it.varureegel })

    // viik: A ja B võrdlusarvud on võrdsed
    val viik = jaotaMandaadid(
      listOf(
        Kandidaat("a1", "A", 1, hääli = 50, jrkNr = 1, regNr = 101),
        Kandidaat("b1", "B", 1, hääli = 50, jrkNr = 1, regNr = 201),
      ),
      listOf(Valimisringkond(1, "Esimene", 1)), mapOf(1 to 100), mandaateKokku = 1
    ).kompensatsiooniSammud
    assertEquals(true, viik.single().viik)
    assertEquals("B", viik.single().erakond)
  }
}
