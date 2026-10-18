import andmed.ÜKSIKKANDIDAADID
import mudel.Kandidaat
import mudel.Valimisringkond
import kotlin.test.Test
import kotlin.test.assertEquals

class MandaadijaotusTest {

  // Ringkond 1: lihtkvoot 100. A 210 häält -> 2 mandaati, B 80 häält -> 0,8 >= 0,75 -> 1 mandaat.
  // Ringkond 2: lihtkvoot 100. A 50 häält -> 0 mandaati, B 150 häält -> 1 mandaat.
  // Kompensatsioon (2 mandaati): A 260 / 3^0,9 = 96.7, B 230 / 3^0,9 = 85.6 -> A;
  // seejärel A 260 / 4^0,9 = 74.7 < B 85.6 -> B.
  private val ringkonnad = listOf(
    Valimisringkond(1, "Esimene", 3),
    Valimisringkond(2, "Teine", 2),
  )
  private val kehtivaidHääli = mapOf(1 to 300, 2 to 200)
  private val kandidaadid = listOf(
    Kandidaat("a1", "A", 1, hääli = 150, jrkNr = 1),
    Kandidaat("a2", "A", 1, hääli = 60, jrkNr = 3),
    Kandidaat("a3", "A", 2, hääli = 50, jrkNr = 2),
    Kandidaat("b1", "B", 1, hääli = 80, jrkNr = 2),
    Kandidaat("b2", "B", 2, hääli = 120, jrkNr = 1),
    Kandidaat("b3", "B", 2, hääli = 30, jrkNr = 3),
  )

  @Test
  fun `ringkonnamandaadid saavad erakonna enim hääli saanud kandidaadid`() {
    val jaotus = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 5)
    assertEquals(setOf("a1", "a2", "b1", "b2"), jaotus.ringkonnamandaadid.map { it.nimi }.toSet())
    assertEquals(mapOf("A" to 2, "B" to 1), jaotus.ringkonnamandaatideArv[1]!!.filterValues { it > 0 })
    assertEquals(mapOf("B" to 1), jaotus.ringkonnamandaatideArv[2]!!.filterValues { it > 0 })
  }

  @Test
  fun `ülejäänud mandaadid jagatakse kompensatsioonimandaatidena`() {
    val jaotus = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 6)
    assertEquals(listOf("a3", "b3"), jaotus.kompensatsioonimandaadid.map { it.nimi })
    assertEquals(6, jaotus.kõikMandaadid().size)
  }

  @Test
  fun `mandaatide jagamine ei muuda sisendiks antud kandidaatide loendit`() {
    val sisend = kandidaadid.map { it.copy() }.toMutableList()
    jaotaMandaadid(sisend, ringkonnad, kehtivaidHääli, mandaateKokku = 6)
    assertEquals(kandidaadid, sisend)
  }

  @Test
  fun `üksikkandidaate ei käsitleta erakonna nimekirjana`() {
    // Kui üksikkandidaadid oleksid nimekiri, annaks 90 häält 0,9 >= 0,75 tõttu ringkonnamandaadi.
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 100, jrkNr = 1),
      Kandidaat("a2", "A", 1, hääli = 10, jrkNr = 2),
      Kandidaat("i1", ÜKSIKKANDIDAADID, 1, hääli = 90),
    )
    val jaotus = jaotaMandaadid(
      kandidaadid, listOf(Valimisringkond(1, "Esimene", 2)), mapOf(1 to 200), mandaateKokku = 2
    )
    assertEquals(listOf("a1"), jaotus.ringkonnamandaadid.map { it.nimi })
    assertEquals(listOf("a2"), jaotus.kompensatsioonimandaadid.map { it.nimi })
  }

  @Test
  fun `ringkonnamandaati ei saa kandidaat, kellel on alla 10 protsendi lihtkvoodist`() {
    // Lihtkvoot 100. A 205 häält -> 2 mandaati, kuid a2 5 häält < 10. B 95 häält -> 0,95 >= 0,75 -> 1 mandaat.
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 200, jrkNr = 1),
      Kandidaat("a2", "A", 1, hääli = 5, jrkNr = 2),
      Kandidaat("b1", "B", 1, hääli = 95, jrkNr = 1),
    )
    val jaotus = jaotaMandaadid(
      kandidaadid, listOf(Valimisringkond(1, "Esimene", 3)), mapOf(1 to 300), mandaateKokku = 3
    )
    assertEquals(setOf("a1", "b1"), jaotus.ringkonnamandaadid.map { it.nimi }.toSet())
    // täitmata jäänud ringkonnamandaat jagatakse kompensatsioonimandaadina; a2 5 häält = 5% lihtkvoodist
    assertEquals(listOf("a2"), jaotus.kompensatsioonimandaadid.map { it.nimi })
  }

  @Test
  fun `kompensatsioonimandaati ei saa kandidaat, kellel on alla 5 protsendi lihtkvoodist`() {
    // Lihtkvoot 100. a2 on üleriigilises nimekirjas ees, kuid 4 häält < 5, seega mandaadi saab a3.
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 100, jrkNr = 2),
      Kandidaat("a2", "A", 1, hääli = 4, jrkNr = 1),
      Kandidaat("a3", "A", 1, hääli = 20, jrkNr = 3),
    )
    val jaotus = jaotaMandaadid(
      kandidaadid, listOf(Valimisringkond(1, "Esimene", 2)), mapOf(1 to 200), mandaateKokku = 2
    )
    assertEquals(listOf("a3"), jaotus.kompensatsioonimandaadid.map { it.nimi })
  }

  @Test
  fun `alla 5 protsendi üleriigilistest häältest saanud erakond ringkonna- ega kompensatsioonimandaate ei saa`() {
    // Ringkond 1 (lihtkvoot 100): B 80 häält annaks 0,75 reegli järgi mandaadi, kuid B-l on 80 / 2100 = 3,8% häältest.
    // Ringkond 2 (lihtkvoot 2000): A 2000 häält -> 1 mandaat. Kompensatsioonimandaat läheb A-le.
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 20, jrkNr = 1),
      Kandidaat("a2", "A", 2, hääli = 2000, jrkNr = 2),
      Kandidaat("b1", "B", 1, hääli = 80, jrkNr = 1),
    )
    val ringkonnad = listOf(Valimisringkond(1, "Esimene", 1), Valimisringkond(2, "Teine", 1))
    val jaotus = jaotaMandaadid(kandidaadid, ringkonnad, mapOf(1 to 100, 2 to 2000), mandaateKokku = 2)
    assertEquals(listOf("a2"), jaotus.ringkonnamandaadid.map { it.nimi })
    assertEquals(listOf("a1"), jaotus.kompensatsioonimandaadid.map { it.nimi })
  }

  @Test
  fun `lihtkvoodi täitnud kandidaat saab isikumandaadi ka siis, kui tema nimekiri jääb alla künnise`() {
    // Ringkond 1 (lihtkvoot 100): x1 100 häält = lihtkvoot. x1 nimekirjal on 100 / 3100 = 3,2% häältest.
    for (nimekiri in listOf("B", ÜKSIKKANDIDAADID)) {
      val kandidaadid = listOf(
        Kandidaat("a1", "A", 2, hääli = 3000, jrkNr = 1),
        Kandidaat("x1", nimekiri, 1, hääli = 100, jrkNr = 1),
      )
      val ringkonnad = listOf(Valimisringkond(1, "Esimene", 1), Valimisringkond(2, "Teine", 1))
      val jaotus = jaotaMandaadid(kandidaadid, ringkonnad, mapOf(1 to 100, 2 to 3000), mandaateKokku = 2)
      assertEquals(setOf("a1", "x1"), jaotus.ringkonnamandaadid.map { it.nimi }.toSet(), nimekiri)
      assertEquals(emptyList(), jaotus.kompensatsioonimandaadid, nimekiri)
    }
  }

  @Test
  fun `kui nimekirjas pole 5 protsendi nõudele vastavaid kandidaate, saab mandaadi suurima lihtkvoodi osakaaluga kandidaat`() {
    // Lihtkvoot 100. A 287 häält -> 2 + 1 (0,87) mandaati, kuid ainult a1 täidab 10% nõude.
    // Kaks kompensatsioonimandaati: a2 (4%) ja a3 (3%) jäävad alla 5%, seega järjekord on osakaalu järgi.
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 280, jrkNr = 1),
      Kandidaat("a3", "A", 1, hääli = 3, jrkNr = 2),
      Kandidaat("a2", "A", 1, hääli = 4, jrkNr = 3),
    )
    val jaotus = jaotaMandaadid(
      kandidaadid, listOf(Valimisringkond(1, "Esimene", 3)), mapOf(1 to 300), mandaateKokku = 3
    )
    assertEquals(listOf("a1"), jaotus.ringkonnamandaadid.map { it.nimi })
    assertEquals(listOf("a2", "a3"), jaotus.kompensatsioonimandaadid.map { it.nimi })
  }

  @Test
  fun `võrdse häälte arvu korral saab ringkonnamandaadi üleriigilises nimekirjas eespool olev kandidaat`() {
    // RKVS § 62 lg 3. Lihtkvoot 100, A 100 häält -> 1 mandaat; a1 ja a2 said kumbki 50 häält.
    val kandidaadid = listOf(
      Kandidaat("a2", "A", 1, hääli = 50, jrkNr = 2),
      Kandidaat("a1", "A", 1, hääli = 50, jrkNr = 1),
    )
    // tulemus ei tohi sõltuda kandidaatide järjekorrast sisendis
    for (sisend in listOf(kandidaadid, kandidaadid.reversed())) {
      val jaotus = jaotaMandaadid(
        sisend, listOf(Valimisringkond(1, "Esimene", 1)), mapOf(1 to 100), mandaateKokku = 1
      )
      assertEquals(listOf("a1"), jaotus.ringkonnamandaadid.map { it.nimi }, "sisend ${sisend.map { it.nimi }}")
    }
  }

  @Test
  fun `võrdse lihtkvoodi osakaalu korral saab varureegli järgi mandaadi nimekirjas eespool olev kandidaat`() {
    // RKVS § 62 lg 7. Lihtkvoot 100, a1 täidab lihtkvoodi; a2 ja a3 said kumbki 4 häält (alla 5%).
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 192, jrkNr = 1),
      Kandidaat("a3", "A", 1, hääli = 4, jrkNr = 3),
      Kandidaat("a2", "A", 1, hääli = 4, jrkNr = 2),
    )
    val jaotus = jaotaMandaadid(
      kandidaadid, listOf(Valimisringkond(1, "Esimene", 2)), mapOf(1 to 200), mandaateKokku = 2
    )
    assertEquals(listOf("a2"), jaotus.kompensatsioonimandaadid.map { it.nimi })
  }

  @Test
  fun `võrdsete võrdlusarvude korral saab mandaadi erakond, kelle kandidaadid on koondnimekirjas tagapool`() {
    // RKVS § 62 lg 5 ja § 33 lg 2: koondnimekirjas on kandidaadid registreerimisnumbrite järjekorras.
    // Lihtkvoot 100, A ja B said kumbki 50 häält, ringkonnamandaate ei saa kumbki; B registreerimisnumbrid on suuremad.
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 50, jrkNr = 1, regNr = 101),
      Kandidaat("b1", "B", 1, hääli = 50, jrkNr = 1, regNr = 201),
    )
    val jaotus = jaotaMandaadid(
      kandidaadid, listOf(Valimisringkond(1, "Esimene", 1)), mapOf(1 to 100), mandaateKokku = 1
    )
    assertEquals(listOf("b1"), jaotus.kompensatsioonimandaadid.map { it.nimi })
  }
}
