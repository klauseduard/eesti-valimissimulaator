import andmed.kehtivaidHääli
import andmed.rk2023Toetusprotsendid
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KüsitluseVigaTest {

  private val toetus = mapOf(
    "A" to mapOf(1 to 40.0, 2 to 20.0),
    "B" to mapOf(1 to 30.0, 2 to 50.0),
    "C" to mapOf(1 to 0.0, 2 to 10.0),
  )
  private val kehtivad = mapOf(1 to 1000, 2 to 3000)

  @Test
  fun `veata jääb toetus samaks`() {
    assertEquals(toetus, juhuslikToetus(toetus, Küsitluseviga(0.0, 0.0), Random(1)))
  }

  @Test
  fun `sama seemnega on tulemus sama`() {
    val viga = Küsitluseviga.KALIBREERITUD
    assertEquals(juhuslikToetus(toetus, viga, Random(5)), juhuslikToetus(toetus, viga, Random(5)))
  }

  @Test
  fun `ringkonna toetuste summa ei muutu ja null jääb nulliks`() {
    val juhus = Random(2)
    repeat(100) {
      val uus = juhuslikToetus(toetus, Küsitluseviga.KALIBREERITUD, juhus)
      for (ringkond in listOf(1, 2)) {
        val vana = toetus.values.sumOf { it[ringkond]!! }
        assertTrue(abs(vana - uus.values.sumOf { it[ringkond]!! }) < 1e-9, "ringkond $ringkond")
      }
      assertEquals(0.0, uus["C"]!![1])
      assertTrue(uus.values.all { erakond -> erakond.values.all { it >= 0.0 } })
    }
  }

  @Test
  fun `toetus kõigub ümber küsitluse väärtuse`() {
    val juhus = Random(3)
    val kordi = 4000
    var summa = 0.0
    repeat(kordi) { summa += üleriigilineToetus(juhuslikToetus(toetus, Küsitluseviga.KALIBREERITUD, juhus)["A"]!!, kehtivad) }
    val oodatud = üleriigilineToetus(toetus["A"]!!, kehtivad)
    assertTrue(abs(summa / kordi - oodatud) / oodatud < 0.03, "keskmine ${summa / kordi}, küsitlus $oodatud")
  }

  /**
   * Kalibreerimine: 2023. aasta viimaste küsitluste (Norstat 03.03 ja Emor) viga tegeliku tulemuse suhtes.
   * Üleriigiline viga on erakondade üleriigilise toetuse logaritmilise suhte (tulemus / küsitlus)
   * ruutkeskmine, ringkondlik viga on ringkondade logaritmiliste suhete hälve üleriigilisest suhtest.
   * Arvesse lähevad erakonnad, kelle küsitluse toetus oli vähemalt 3%, ja ringkonnad, kus nii küsitluse
   * kui ka tulemuse toetus oli üle 0,5%. Kahe küsitluse ruutkeskmised liidetakse ühte.
   */
  @Test
  fun `kalibreeritud viga vastab 2023 küsitluste tegelikule veale`() {
    val tegelik = rk2023Toetusprotsendid(Testiandmed.rk2023)
    val üleriigilised = mutableListOf<Double>()
    val ringkondlikud = mutableListOf<Double>()
    for (küsitlus in listOf(Testiandmed.norstatiToetus(), Testiandmed.emoriToetus)) {
      for ((erakond, protsendid) in küsitlus) {
        val küsitluses = üleriigilineToetus(protsendid, kehtivaidHääli)
        val tulemuses = üleriigilineToetus(tegelik[erakond]!!, kehtivaidHääli)
        if (küsitluses < 3.0) continue
        val suhe = ln(tulemuses / küsitluses)
        üleriigilised.add(suhe)
        for ((ringkond, p) in protsendid) {
          val t = tegelik[erakond]!![ringkond]!!
          if (p > 0.5 && t > 0.5) ringkondlikud.add(ln(t / p) - suhe)
        }
      }
    }
    fun ruutkeskmine(väärtused: List<Double>) = sqrt(väärtused.sumOf { it * it } / väärtused.size)
    fun kolmeKohaga(x: Double) = round(x * 1000) / 1000
    assertEquals(kolmeKohaga(ruutkeskmine(üleriigilised)), Küsitluseviga.KALIBREERITUD.üleriigiline)
    assertEquals(kolmeKohaga(ruutkeskmine(ringkondlikud)), Küsitluseviga.KALIBREERITUD.ringkondlik)
  }

  @Test
  fun `küsitluse veaga erinevad erakondade mandaadid korduste vahel`() {
    val ilmaVeata = simuleeriKorduvalt(Testiandmed.kandidaadid(), Testiandmed.norstatiToetus(), 30, Random(1))
    val veaga = simuleeriKorduvalt(Testiandmed.kandidaadid(), Testiandmed.norstatiToetus(), 30, Random(1), viga = Küsitluseviga.KALIBREERITUD)
    val reform = "Eesti Reformierakond"
    assertEquals(1, ilmaVeata.erakondadeMandaadid[reform]!!.toSet().size)
    assertTrue(veaga.erakondadeMandaadid[reform]!!.toSet().size > 3)
    assertTrue(veaga.erakondadeMandaadid.values.let { m -> (0 until 30).all { k -> m.sumOf { it[k] } == 101 } })
  }
}
