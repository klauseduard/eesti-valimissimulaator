import mudel.Kandidaat
import kotlin.test.Test
import kotlin.test.assertEquals

class KompensatsioonimandaadidTest {

  private val kandidaadid = listOf(
    Kandidaat("A2", "A", 1, hääli = 50, jrkNr = 2),
    Kandidaat("A1", "A", 1, hääli = 50, jrkNr = 1),
    Kandidaat("A3", "A", 2, hääli = 50, jrkNr = 3),
    Kandidaat("B1", "B", 2, hääli = 50, jrkNr = 1),
    Kandidaat("B2", "B", 1, hääli = 50, jrkNr = 2),
  )
  private val lihtkvoodid = mapOf(1 to 100.0, 2 to 100.0)
  private val kohtKoondnimekirjas = mapOf("A" to 101, "B" to 201)

  @Test
  fun `mandaadid jagatakse jagajatega 1, 2^0,9, 3^0,9 üleriigilise nimekirja järjekorras`() {
    // A: 1000, 535.9, 372.0; B: 600, 321.5 -> A, B, A
    val mandaadid = arvutaKompensatsioonimandaadid(
      kandidaadid,
      jagamataMandaate = 3,
      jagatudMandaadidErakondadeKaupa = mapOf("A" to 0, "B" to 0),
      erakondadeHääledKoguRiigis = mapOf("A" to 1000, "B" to 600),
      lihtkvoodid,
      kohtKoondnimekirjas
    )
    assertEquals(listOf("A1", "B1", "A2"), mandaadid.map { it.nimi })
  }

  @Test
  fun `ringkonnas saadud mandaadid jäetakse jagajate jadast vahele`() {
    // A-l on juba üks mandaat, seega A võrdlusarv on 1000 / 2^0,9 = 535.9 < 600
    val mandaadid = arvutaKompensatsioonimandaadid(
      kandidaadid,
      jagamataMandaate = 1,
      jagatudMandaadidErakondadeKaupa = mapOf("A" to 1, "B" to 0),
      erakondadeHääledKoguRiigis = mapOf("A" to 1000, "B" to 600),
      lihtkvoodid,
      kohtKoondnimekirjas
    )
    assertEquals(listOf("B1"), mandaadid.map { it.nimi })
  }
}
