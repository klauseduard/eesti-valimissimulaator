import koond.Hinnang
import koond.Küsitlus
import koond.Meetod
import koond.Mudeliparameetrid
import koond.Valimised
import koond.hinnang
import koond.hindaParameetrid
import koond.küsitlejaHinnang
import koond.loeKüsitlused
import koond.skoor
import koond.valiKüsitlused
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KoondmudelTest {

  private fun päev(s: String) = LocalDate.parse(s)
  private fun küsitlus(küsitleja: String, algus: String?, lõpp: String, a: Double, b: Double, valim: Int? = 1000) =
    Küsitlus(küsitleja, algus?.let(::päev), päev(lõpp), valim, mapOf("A" to a, "B" to b), 100.0)

  private val nullParameetrid = Mudeliparameetrid(0.0, emptyMap(), 0.0)

  @Test
  fun `CSV rida normaliseeritakse summale 100 ja tühjad väljad jäetakse välja`() {
    val k = loeKüsitlused("kusitleja,algus,lopp,valim,A,B,C,MUUD\nNorstat,2023-02-01,2023-02-27,4000,45,45,,20\n").single()
    assertTrue(abs(k.osakaalud["A"]!! - 40.909090909) < 1e-6)
    assertEquals(setOf("A", "B", "MUUD"), k.osakaalud.keys)
    assertEquals(110.0, k.algneSumma)
    assertEquals(päev("2023-02-14"), k.kuupäev)
    assertEquals(2000 / 1.5, k.efektiivneValim)
  }

  @Test
  fun `puuduv muude osakaal on jääk sajani, kuid kõigi vastajate alusel normaliseeritakse`() {
    val k = loeKüsitlused(
      "kusitleja,algus,lopp,valim,A,B,MUUD,alus,markus\n" +
        "Emor,,2019-02-28,1000,40,50,,,\n" +
        "TU,,2011-02,500,30,48,,koik_vastajad,\"märkus, komaga\"\n"
    )
    assertEquals(mapOf("A" to 40.0, "B" to 50.0, "MUUD" to 10.0), k[0].osakaalud)
    assertTrue(abs(k[1].osakaalud["A"]!! - 30 * 100 / 78.0) < 1e-9)
    assertEquals(päev("2011-02-28"), k[1].lõpp)
    assertEquals(päev("2011-02-15"), k[1].kuupäev)
  }

  @Test
  fun `sama küsitleja kattuvatest küsitlustest jääb alles uusim`() {
    val küsitlused = listOf(
      küsitlus("Norstat", "2023-01-31", "2023-02-27", 30.0, 70.0),
      küsitlus("Norstat", "2023-02-07", "2023-03-03", 30.0, 70.0),
      küsitlus("Norstat", "2023-01-01", "2023-01-28", 30.0, 70.0),
      küsitlus("Emor", "2023-02-01", "2023-02-20", 30.0, 70.0),
    )
    val valitud = valiKüsitlused(küsitlused, päev("2023-03-06"))
    assertEquals(setOf(päev("2023-03-03"), päev("2023-01-28"), päev("2023-02-20")), valitud.map { it.lõpp }.toSet())
  }

  @Test
  fun `hiljem kui viivitus lõppenud küsitlust ei kasutata`() {
    val küsitlused = listOf(küsitlus("Norstat", "2023-02-20", "2023-03-04", 30.0, 70.0))
    assertEquals(emptyList(), valiKüsitlused(küsitlused, päev("2023-03-05")))
  }

  @Test
  fun `uuem küsitlus kaalub rohkem`() {
    val küsitlused = listOf(
      küsitlus("Norstat", null, "2023-02-01", 20.0, 80.0),
      küsitlus("Norstat", null, "2023-03-01", 40.0, 60.0),
    )
    val h = assertNotNull(küsitlejaHinnang(küsitlused, "A", päev("2023-03-05")))
    assertTrue(exp(h.logKeskmine) * 100 > 30.0, "uuem küsitlus peab kaaluma rohkem: ${exp(h.logKeskmine) * 100}")
  }

  @Test
  fun `võrdsete kaaludega on iga küsitleja üks hääl, ka kui ühel on rohkem küsitlusi`() {
    // Norstatil kolm mittekattuvat küsitlust A = 20, Emoril üks küsitlus A = 40
    val küsitlused = listOf(
      küsitlus("Norstat", null, "2023-02-25", 20.0, 80.0),
      küsitlus("Norstat", null, "2023-02-18", 20.0, 80.0),
      küsitlus("Norstat", null, "2023-02-11", 20.0, 80.0),
      küsitlus("Emor", null, "2023-02-25", 40.0, 60.0),
    )
    val h = assertNotNull(hinnang(küsitlused, päev("2023-03-01"), Meetod.VÕRDSED_KAALUD, nullParameetrid))
    // logaritmide keskmine: sqrt(0,2 · 0,4) ja sqrt(0,8 · 0,6), normaliseeritud
    val a = kotlin.math.sqrt(0.2 * 0.4)
    val b = kotlin.math.sqrt(0.8 * 0.6)
    assertTrue(abs(h.osakaal("A") - 100 * a / (a + b)) < 1e-9)
  }

  @Test
  fun `usalduskaalud eelistavad väiksema majaefektiga küsitlejat`() {
    val küsitlused = listOf(küsitlus("Norstat", null, "2023-02-25", 20.0, 80.0), küsitlus("Emor", null, "2023-02-25", 40.0, 60.0))
    val p = Mudeliparameetrid(0.01, mapOf("Norstat" to 0.001, "Emor" to 0.05), 0.0)
    val h = assertNotNull(hinnang(küsitlused, päev("2023-03-01"), Meetod.USALDUSKAALUD, p))
    assertTrue(h.osakaal("A") < 25.0, "Norstat peab kaaluma rohkem: ${h.osakaal("A")}")
  }

  @Test
  fun `ühine viga lisandub iga meetodi hälbele`() {
    val küsitlused = listOf(küsitlus("Norstat", null, "2023-02-25", 20.0, 80.0))
    val ilma = assertNotNull(hinnang(küsitlused, päev("2023-03-01"), Meetod.VIIMANE_NORSTAT, nullParameetrid))
    val ühisega = assertNotNull(hinnang(küsitlused, päev("2023-03-01"), Meetod.VIIMANE_NORSTAT, nullParameetrid.copy(ühine = 0.04)))
    assertTrue(abs(ühisega.logSd["A"]!! * ühisega.logSd["A"]!! - ilma.logSd["A"]!! * ilma.logSd["A"]!! - 0.04) < 1e-12)
  }

  @Test
  fun `dispersioonikomponendid leitakse simuleeritud andmetest ligikaudu õigesti`() {
    // tõelised väärtused: ühine viga 0,15², majaefekt 0,08²; valimiviga simuleeritakse sama suurena, nagu
    // mudel seda eeldab (efektiivne valim 2000 / 1,5), sest hinnang lahutab selle maha
    val juhus = Random(1)
    fun normaalne() = kotlin.math.sqrt(-2 * ln(1 - juhus.nextDouble())) * kotlin.math.cos(2 * kotlin.math.PI * juhus.nextDouble())
    val erakonnad = mapOf("A" to 30.0, "B" to 25.0, "C" to 20.0, "D" to 15.0, "E" to 10.0)
    val valimised = (1..40).map { i ->
      val kuupäev = päev("2020-03-01").plusYears(i.toLong())
      val ühine = erakonnad.mapValues { 0.15 * normaalne() }
      val küsitlused = listOf("K1", "K2", "K3").map { küsitleja ->
        val osakaalud = erakonnad.mapValues { (e, t) ->
          val valimiviga = kotlin.math.sqrt(koond.logaritmiDispersioon(t, 2000 / 1.5)) * normaalne()
          t * exp(ühine[e]!! + 0.08 * normaalne() + valimiviga)
        }
        Küsitlus(küsitleja, null, kuupäev.minusDays(10), 2000, osakaalud, 100.0)
      }
      Valimised(2000 + i, kuupäev, erakonnad, küsitlused)
    }
    val p = hindaParameetrid(valimised, 3)
    assertTrue(abs(kotlin.math.sqrt(p.ühine) - 0.15) < 0.03, "ühine ${kotlin.math.sqrt(p.ühine)}")
    assertTrue(abs(kotlin.math.sqrt(p.majaefekt) - 0.08) < 0.02, "majaefekt ${kotlin.math.sqrt(p.majaefekt)}")
  }

  @Test
  fun `skoor loeb kaetud tulemused ja absoluutvea`() {
    val h = Hinnang(mapOf("A" to ln(0.30), "B" to ln(0.70)), mapOf("A" to 0.1, "B" to 0.01))
    val s = skoor(h, mapOf("A" to 31.0, "B" to 60.0))
    assertEquals(2, s.erakondi)
    assertEquals(1, s.kaetud)
    assertTrue(abs(s.absViga - 11.0) < 1e-9)
  }
}
