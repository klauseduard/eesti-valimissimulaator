import andmed.kandidaadidTegelikeHäältega
import andmed.valimisringkonnad
import andmed.ÜKSIKKANDIDAADID
import mudel.Kandidaat
import mudel.Valimisringkond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mittevalitavad kandidaadid (RKVS § 62 lg 9): kandidaat suri pärast eelhääletamise algust või tema
 * registreerimine tühistati. Tema hääled jäävad erakonnale, kuid mandaati ta ei saa; mandaadi saab järgmine
 * nõuetele vastav kandidaat § 62 lg 3, 6 ja 7 järgi.
 */
class MittevalitavadTest {

  private fun nimed(kandidaadid: List<Kandidaat>) = kandidaadid.map { it.nimi }.toSet()

  /** Erakondade hääled, künnis ja lihtkvoodi alusel arvutatud mandaatide arv ei sõltu mittevalitavatest. */
  private fun assertSamadErakondadeArvud(ilma: Mandaadijaotus, koos: Mandaadijaotus) {
    assertEquals(ilma.erakondadeHääled, koos.erakondadeHääled)
    assertEquals(ilma.künniseÜletanud, koos.künniseÜletanud)
    fun arvud(j: Mandaadijaotus) = j.ringkonnaArvutused.map { r ->
      Triple(r.ringkond, r.lihtkvoot, r.erakonnad.map { listOf(it.erakond, it.hääli, it.kvoote, it.mandaate, it.lisamandaat) })
    }
    assertEquals(arvud(ilma), arvud(koos))
  }

  @Test
  fun `isikumandaadi saanuks surnud kandidaadi koha saab ringkonnas järgmine häälte arvu järgi`() {
    // lihtkvoot 100. A 270 häält = 2,7 kvooti -> 2 ringkonnamandaati; B 30 häält -> 0. Kompensatsioon (1): A.
    // a1 (150 häält) saaks isikumandaadi. Kui ta suri, on A hääled endiselt 270, seega saab A ikka kaks
    // ringkonnamandaati: a2 ja a3 häälte järgi (a4 on üleriigilises nimekirjas eespool, kuid sai vähem hääli).
    // Kompensatsioonimandaadi saab üleriigilises nimekirjas järgmine: a4.
    val ringkonnad = listOf(Valimisringkond(1, "Esimene", 3))
    val kehtivaidHääli = mapOf(1 to 300)
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 150, jrkNr = 1, regNr = 101),
      Kandidaat("a2", "A", 1, hääli = 60, jrkNr = 3, regNr = 102),
      Kandidaat("a3", "A", 1, hääli = 40, jrkNr = 4, regNr = 103),
      Kandidaat("a4", "A", 1, hääli = 20, jrkNr = 2, regNr = 104),
      Kandidaat("b1", "B", 1, hääli = 30, jrkNr = 1, regNr = 201),
    )
    val ilma = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 3)
    assertEquals(setOf("a1", "a2"), nimed(ilma.ringkonnamandaadid))
    assertEquals(listOf("a4"), ilma.kompensatsioonimandaadid.map { it.nimi })

    val koos = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 3, mittevalitavad = setOf(101))
    assertEquals(listOf("a2", "a3"), koos.ringkonnamandaadid.map { it.nimi })
    assertEquals(listOf("a4"), koos.kompensatsioonimandaadid.map { it.nimi })
    assertTrue(koos.ringkonnaArvutused.single().isikumandaadid.isEmpty())
    assertEquals(mapOf("A" to 2), koos.ringkonnamandaatideArv[1])
    assertSamadErakondadeArvud(ilma, koos)
  }

  @Test
  fun `künnist mitte ületanud nimekirja surnud kandidaadi isikumandaat läheb kompensatsioonimandaatide hulka`() {
    // Ringkond 1 (lihtkvoot 50): D 55 häält, isikumandaat; A 45 = 0,9 kvooti -> 1 mandaat.
    // Ringkond 2 (lihtkvoot 550): A 550 -> 1, B 550 -> 1. D üle riigi 55 / 1200 = 4,6% < 5%.
    // Kui d1 suri, jääb üks koht täitmata ja kompensatsioonis saab selle B (550 / 2^0,9 = 294,8 > A 595 / 3^0,9 = 221,4).
    val ringkonnad = listOf(Valimisringkond(1, "Esimene", 2), Valimisringkond(2, "Teine", 2))
    val kehtivaidHääli = mapOf(1 to 100, 2 to 1100)
    val kandidaadid = listOf(
      Kandidaat("d1", "D", 1, hääli = 55, jrkNr = 1, regNr = 301),
      Kandidaat("a1", "A", 1, hääli = 45, jrkNr = 1, regNr = 101),
      Kandidaat("a2", "A", 2, hääli = 300, jrkNr = 2, regNr = 102),
      Kandidaat("a3", "A", 2, hääli = 250, jrkNr = 3, regNr = 103),
      Kandidaat("b1", "B", 2, hääli = 300, jrkNr = 1, regNr = 201),
      Kandidaat("b2", "B", 2, hääli = 250, jrkNr = 2, regNr = 202),
    )
    val ilma = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 4)
    assertEquals(setOf("d1", "a1", "a2", "b1"), nimed(ilma.ringkonnamandaadid))
    assertTrue(ilma.kompensatsioonimandaadid.isEmpty())

    val koos = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 4, mittevalitavad = setOf(301))
    assertEquals(setOf("a1", "a2", "b1"), nimed(koos.ringkonnamandaadid))
    assertEquals(listOf("b2"), koos.kompensatsioonimandaadid.map { it.nimi })
    assertFalse("D" in koos.künniseÜletanud)
    assertSamadErakondadeArvud(ilma, koos)
  }

  @Test
  fun `ringkonnamandaadi saanuks surnud kandidaadi koha saab järgmine, kellel on vähemalt 10 protsenti lihtkvoodist`() {
    // lihtkvoot 100. A 195 häält = 1,95 kvooti -> 2 mandaati (jääk 0,95 >= 0,75); B 105 -> 1.
    val ringkonnad = listOf(Valimisringkond(1, "Esimene", 3))
    val kehtivaidHääli = mapOf(1 to 300)
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 90, jrkNr = 1, regNr = 101),
      Kandidaat("a2", "A", 1, hääli = 70, jrkNr = 2, regNr = 102),
      Kandidaat("a3", "A", 1, hääli = 30, jrkNr = 3, regNr = 103),
      Kandidaat("a4", "A", 1, hääli = 5, jrkNr = 4, regNr = 104),
      Kandidaat("b1", "B", 1, hääli = 80, jrkNr = 1, regNr = 201),
      Kandidaat("b2", "B", 1, hääli = 25, jrkNr = 2, regNr = 202),
    )
    val ilma = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 3)
    assertEquals(setOf("a1", "a2", "b1"), nimed(ilma.ringkonnamandaadid))

    // a2 suri: tema koha saab a3
    val koos = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 3, mittevalitavad = setOf(102))
    assertEquals(setOf("a1", "a3", "b1"), nimed(koos.ringkonnamandaadid))
    assertTrue(koos.kompensatsioonimandaadid.isEmpty())
    assertSamadErakondadeArvud(ilma, koos)

    // a2 ja a3 surid: a4 sai alla 10% lihtkvoodist, seega jääb A teine ringkonnamandaat täitmata ja läheb
    // kompensatsioonimandaatide hulka; seal piisab 5% nõudest ja mandaadi saab a4 (A 195 / 2^0,9 > B 105 / 2^0,9)
    val kaks = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 3, mittevalitavad = setOf(102, 103))
    assertEquals(setOf("a1", "b1"), nimed(kaks.ringkonnamandaadid))
    assertEquals(listOf("a4"), kaks.kompensatsioonimandaadid.map { it.nimi })
    assertEquals(listOf("a4"), kaks.ringkonnaArvutused.single().erakonnad.single { it.erakond == "A" }.alla10Protsendi.map { it.nimi })
    assertSamadErakondadeArvud(ilma, kaks)
  }

  @Test
  fun `kompensatsioonimandaadi saanuks surnud kandidaadi koha saab üleriigilises nimekirjas järgmine`() {
    // Ringkond 1 (lihtkvoot 100): A 210 -> 2 (a1, a2), B 90 -> 1 (b1).
    // Ringkond 2 (lihtkvoot 100): A 70 -> 0, B 130 -> 1 (b2).
    // Kompensatsioon (2): A 280 / 3^0,9 = 104,2 > B 220 / 3^0,9 = 81,8 -> A;
    // seejärel A 280 / 4^0,9 = 80,4 < B 81,8 -> B.
    val ringkonnad = listOf(Valimisringkond(1, "Esimene", 3), Valimisringkond(2, "Teine", 2))
    val kehtivaidHääli = mapOf(1 to 300, 2 to 200)
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 150, jrkNr = 1, regNr = 101),
      Kandidaat("a2", "A", 1, hääli = 60, jrkNr = 4, regNr = 102),
      Kandidaat("a3", "A", 2, hääli = 40, jrkNr = 2, regNr = 103),
      Kandidaat("a4", "A", 2, hääli = 30, jrkNr = 3, regNr = 104),
      Kandidaat("b1", "B", 1, hääli = 90, jrkNr = 1, regNr = 201),
      Kandidaat("b2", "B", 2, hääli = 100, jrkNr = 2, regNr = 202),
      Kandidaat("b3", "B", 2, hääli = 30, jrkNr = 3, regNr = 203),
    )
    val ilma = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 6)
    assertEquals(setOf("a1", "a2", "b1", "b2"), nimed(ilma.ringkonnamandaadid))
    assertEquals(listOf("a3", "b3"), ilma.kompensatsioonimandaadid.map { it.nimi })

    // a3 suri: A saab ikka esimese kompensatsioonimandaadi samade võrdlusarvudega, selle saab a4. Seejärel pole
    // A nimekirjas enam kandidaate, seega osaleb teises sammus ainult B (§ 62 lg 8).
    val koos = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 6, mittevalitavad = setOf(103))
    assertEquals(setOf("a1", "a2", "b1", "b2"), nimed(koos.ringkonnamandaadid))
    assertEquals(listOf("a4", "b3"), koos.kompensatsioonimandaadid.map { it.nimi })
    assertEquals(ilma.kompensatsiooniSammud.first().võrdlusarvud, koos.kompensatsiooniSammud.first().võrdlusarvud)
    assertEquals(setOf("B"), koos.kompensatsiooniSammud[1].võrdlusarvud.keys)
    assertTrue(koos.kompensatsiooniSammud.first().vahelejäetud.isEmpty())
    assertSamadErakondadeArvud(ilma, koos)
  }

  @Test
  fun `surnud kandidaadi hääled loetakse erakonna künnise hulka`() {
    // lihtkvoot 250. C 65 häält = 6,5% kehtivatest häältest; ilma c1 häälteta oleks C alla 5% künnise.
    // Kompensatsioonimandaadi (1) saab C, sest A ja B nimekirjas pole enam kandidaate (§ 62 lg 8). c2 sai alla
    // 5% lihtkvoodist, seega saab ta mandaadi varureegli järgi (§ 62 lg 7).
    val ringkonnad = listOf(Valimisringkond(1, "Esimene", 4))
    val kehtivaidHääli = mapOf(1 to 1000)
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 600, jrkNr = 1, regNr = 101),
      Kandidaat("a2", "A", 1, hääli = 50, jrkNr = 2, regNr = 102),
      Kandidaat("b1", "B", 1, hääli = 285, jrkNr = 1, regNr = 201),
      Kandidaat("c1", "C", 1, hääli = 60, jrkNr = 1, regNr = 301),
      Kandidaat("c2", "C", 1, hääli = 5, jrkNr = 2, regNr = 302),
    )
    val ilma = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 4)
    val koos = jaotaMandaadid(kandidaadid, ringkonnad, kehtivaidHääli, mandaateKokku = 4, mittevalitavad = setOf(301))
    assertEquals(65, koos.erakondadeHääled["C"])
    assertTrue("C" in koos.künniseÜletanud)
    assertEquals(listOf("c1"), ilma.kompensatsioonimandaadid.map { it.nimi })
    assertEquals(listOf("c2"), koos.kompensatsioonimandaadid.map { it.nimi })
    assertTrue(koos.kompensatsiooniSammud.single().varureegel)
    assertSamadErakondadeArvud(ilma, koos)
  }

  @Test
  fun `tühi hulk ei muuda 2023 aasta tegelike häälte jaotust`() {
    val tulemus = Testiandmed.rk2023
    val kandidaadid = kandidaadidTegelikeHäältega(Testiandmed.kandidaadid(), tulemus)
    val ringkonnad = valimisringkonnad.toList()
    val tavaline = jaotaMandaadid(kandidaadid, ringkonnad, tulemus.kehtivaidHääli)
    val tühjaga = jaotaMandaadid(kandidaadid, ringkonnad, tulemus.kehtivaidHääli, mittevalitavad = emptySet())
    assertEquals(tavaline, tühjaga)
    assertEquals(mandaadidTekstina(tavaline), mandaadidTekstina(tühjaga))
    assertEquals(tavaline, jaotaLisatudHäältega(kandidaadid, ringkonnad, tulemus.kehtivaidHääli, emptyMap(), emptySet()))
  }

  @Test
  fun `2023 aasta häältega jääb surnud kandidaadi mandaat erakonnale`() {
    val tulemus = Testiandmed.rk2023
    val kandidaadid = kandidaadidTegelikeHäältega(Testiandmed.kandidaadid(), tulemus)
    val ringkonnad = valimisringkonnad.toList()
    val tavaline = jaotaMandaadid(kandidaadid, ringkonnad, tulemus.kehtivaidHääli)
    fun erakonniti(j: Mandaadijaotus) = j.kõikMandaadid().groupingBy { it.erakond }.eachCount()

    // enim hääli saanud isikumandaadi saaja ja esimene kompensatsioonimandaadi saaja
    val isikumandaat = tavaline.ringkonnaArvutused.flatMap { it.isikumandaadid }.maxBy { it.hääli }
    val kompensatsioonimandaat = tavaline.kompensatsioonimandaadid.first()
    for (surnu in listOf(isikumandaat, kompensatsioonimandaat)) {
      val koos = jaotaMandaadid(kandidaadid, ringkonnad, tulemus.kehtivaidHääli, mittevalitavad = setOf(surnu.regNr))
      assertEquals(101, koos.kõikMandaadid().size)
      assertFalse(surnu.regNr in valitudRegNrid(koos), surnu.nimi)
      assertEquals(erakonniti(tavaline), erakonniti(koos), surnu.nimi)
      assertSamadErakondadeArvud(tavaline, koos)
      // ainult üks valitu vahetub: surnu asemel üks sama erakonna kandidaat
      val lisandunud = valitudRegNrid(koos) - valitudRegNrid(tavaline)
      assertEquals(1, lisandunud.size, surnu.nimi)
      assertEquals(surnu.erakond, kandidaadid.single { it.regNr == lisandunud.single() }.erakond)
    }
  }

  @Test
  fun `tundmatu kandidaat ja üksikkandidaat on vead`() {
    val ringkonnad = listOf(Valimisringkond(1, "Esimene", 1))
    val kandidaadid = listOf(
      Kandidaat("a1", "A", 1, hääli = 60, jrkNr = 1, regNr = 101),
      Kandidaat("i1", ÜKSIKKANDIDAADID, 1, hääli = 40, regNr = 401),
    )
    assertFailsWith<IllegalArgumentException> {
      jaotaMandaadid(kandidaadid, ringkonnad, mapOf(1 to 100), mandaateKokku = 1, mittevalitavad = setOf(999))
    }
    assertFailsWith<IllegalArgumentException> {
      jaotaMandaadid(kandidaadid, ringkonnad, mapOf(1 to 100), mandaateKokku = 1, mittevalitavad = setOf(401))
    }
  }
}
