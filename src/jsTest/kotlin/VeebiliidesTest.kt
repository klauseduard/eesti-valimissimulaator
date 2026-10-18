import andmed.RK2023_TULEMUSE_FAIL
import andmed.VAIKIMISI_REITINGUFAIL
import andmed.ringkonnaKandidaatideFail
import andmed.ÜLERIIGILISTE_NIMEKIRJADE_FAIL
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VeebiliidesTest {

  private val simulaator = Simulaator(
    (1..12).map { loeProjektiFail("$RESSURSID/${ringkonnaKandidaatideFail(it)}") }.toTypedArray(),
    loeProjektiFail("$RESSURSID/$ÜLERIIGILISTE_NIMEKIRJADE_FAIL"),
    loeProjektiFail("$RESSURSID/$RK2023_TULEMUSE_FAIL"),
  )
  private val norstat = simulaator.norstatiToetus(loeProjektiFail("$RESSURSID/$VAIKIMISI_REITINGUFAIL"))

  @Test
  fun `tegelike häältega jagatakse 2023 ametlikud mandaadid`() {
    val tulemus = simulaator.jaotaTegelikeHäältega()
    val mandaate = tulemus.erakonnad.filter { it.mandaate > 0 }.associate { it.erakond to it.mandaate }
    assertEquals(
      mapOf(
        "Eesti Reformierakond" to 37, "Eesti Konservatiivne Rahvaerakond" to 17, "Eesti Keskerakond" to 16,
        "Erakond Eesti 200" to 14, "Sotsiaaldemokraatlik Erakond" to 9, "ISAMAA Erakond" to 8,
      ),
      mandaate
    )
    assertEquals(101, tulemus.valitud.size)
    val liigid = tulemus.valitud.groupBy { it.mandaat }.mapValues { it.value.size }
    assertEquals<Map<String?, Int>>(mapOf(ISIKUMANDAAT to 9, RINGKONNAMANDAAT to 66, KOMPENSATSIOONIMANDAAT to 26), liigid)
    assertEquals(26, tulemus.kompensatsioon.size)
    assertEquals(12, tulemus.ringkonnad.size)
  }

  @Test
  fun `seemnega simulatsioon annab sama tulemuse kui kuldne fail`() {
    val valitud = simulaator.jaota(norstat, 1, JAOTUS_KALIBREERITUD).valitud.map { it.nimi }.toSet()
    val kuldne = loeProjektiFail("src/commonTest/resources/kuldsed/norstat-seed1.txt").lines()
      .filter { it.isNotBlank() }.map { it.substringAfter("nimi=").substringBefore(", erakond=") }.toSet()
    assertEquals(kuldne, valitud)
  }

  @Test
  fun `erakonnad on kandidaatide koondnimekirja järjekorras`() {
    val erakonnad = simulaator.erakonnad().map { it.nimi }
    assertEquals("Erakond Parempoolsed", erakonnad.first())
    assertEquals(9, erakonnad.size)
  }

  @Test
  fun `üleriigilise toetuse muutmine säilitab ringkondade summad`() {
    val uus = simulaator.muudaÜleriigilistToetust(norstat, "Eesti Reformierakond", 35.0)
    assertTrue(abs(35.0 - simulaator.üleriigilineToetus(uus, "Eesti Reformierakond")) < 1e-9)
    for (j in 0 until 12) {
      assertTrue(abs(norstat.protsendid.sumOf { it[j] } - uus.protsendid.sumOf { it[j] }) < 1e-9, "ringkond ${j + 1}")
    }
  }

  @Test
  fun `üle 100 protsendi toetus ringkonnas on viga`() {
    val vigane = ToetusJs(norstat.erakonnad, norstat.protsendid.mapIndexed { i, p ->
      if (i == 0) p.mapIndexed { j, v -> if (j == 0) v + 50.0 else v }.toTypedArray() else p
    }.toTypedArray())
    assertEquals(1, simulaator.kontrolliToetust(vigane).size)
    assertEquals(0, simulaator.kontrolliToetust(norstat).size)
    assertFailsWith<IllegalArgumentException> { simulaator.jaota(vigane, 1, JAOTUS_KALIBREERITUD) }
  }

  @Test
  fun `korduv simulatsioon osade kaupa annab sama tulemuse kui korraga`() {
    val osadena = simulaator.korduv(norstat, 7, Küsitluseviga.MÕÕDETUD_2023.üleriigiline, Küsitluseviga.MÕÕDETUD_2023.ringkondlik, JAOTUS_KALIBREERITUD).apply { korda(20); korda(30) }.kokkuvõte()
    val korraga = simulaator.korduv(norstat, 7, Küsitluseviga.MÕÕDETUD_2023.üleriigiline, Küsitluseviga.MÕÕDETUD_2023.ringkondlik, JAOTUS_KALIBREERITUD).apply { korda(50) }.kokkuvõte()
    assertEquals(50, osadena.kordi)
    assertEquals(korraga.kandidaadid.map { "${it.nimi} ${it.kordi}" }, osadena.kandidaadid.map { "${it.nimi} ${it.kordi}" })
    assertEquals(korraga.mandaadid.map { it.toList() }, osadena.mandaadid.map { it.toList() })
    assertEquals(12, osadena.ringkondadeMandaadid.size)
  }

  @Test
  fun `küsitluse veaga erinevad erakondade mandaadid korduste vahel`() {
    val veaga = simulaator.korduv(norstat, 1, Küsitluseviga.MÕÕDETUD_2023.üleriigiline, Küsitluseviga.MÕÕDETUD_2023.ringkondlik, JAOTUS_KALIBREERITUD).apply { korda(30) }.kokkuvõte()
    val ilma = simulaator.korduv(norstat, 1, 0.0, 0.0, JAOTUS_KALIBREERITUD).apply { korda(30) }.kokkuvõte()
    val i = veaga.erakonnad.indexOf("Eesti Reformierakond")
    val j = ilma.erakonnad.indexOf("Eesti Reformierakond")
    assertTrue(veaga.mandaadid[i].toSet().size > 3)
    assertEquals(1, ilma.mandaadid[j].toSet().size)
  }
}
