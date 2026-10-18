import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Küsitluse veaga korduv simulatsioon peab andma sama seemne korral JVM-il ja Node'is sama tulemuse,
 * et veebileht ja käsurea programm ühtiksid. Oodatud väärtus on salvestatud JVM-i käivitusest.
 */
class KorduvKuldneTest {

  @Test
  fun `küsitluse veaga kordused annavad mõlemal platvormil sama tulemuse`() {
    val kokkuvõte = simuleeriKorduvalt(
      Testiandmed.kandidaadid(), Testiandmed.norstatiToetus(), 40, Random(1), viga = Küsitluseviga.MÕÕDETUD_2023
    )
    val tekst = kokkuvõte.erakondadeMandaadid.entries.sortedBy { it.key }
      .joinToString("\n") { (erakond, mandaadid) -> "$erakond: ${mandaadid.joinToString(",")}" }
    assertEquals(OODATUD, tekst)
  }

  companion object {
    const val OODATUD =
      "Eesti Keskerakond: 21,23,16,19,22,19,22,22,25,16,30,23,18,22,15,21,24,15,19,17,19,19,21,15,22,15,17,18,17,19,18,22,16,19,21,18,18,22,16,17\n" +
      "Eesti Konservatiivne Rahvaerakond: 17,24,17,18,24,23,21,20,18,26,19,20,31,22,23,23,24,22,23,31,26,20,23,19,21,20,26,24,28,29,23,22,19,33,19,24,23,16,22,26\n" +
      "Eesti Reformierakond: 32,28,42,33,27,31,35,27,30,37,27,32,26,33,37,28,26,34,31,30,31,34,30,35,34,41,27,34,34,30,36,34,40,30,38,38,34,35,35,32\n" +
      "Erakond Eesti 200: 13,12,9,12,8,13,10,19,13,11,9,9,11,11,9,12,11,11,14,8,12,12,10,18,10,12,11,10,9,11,7,8,10,6,11,10,9,11,10,11\n" +
      "ISAMAA Erakond: 10,7,6,8,12,9,7,7,9,7,9,9,10,6,8,8,9,10,8,9,8,8,10,9,9,6,9,8,7,4,10,9,9,8,7,6,10,10,11,6\n" +
      "Sotsiaaldemokraatlik Erakond: 8,7,11,11,8,6,6,6,6,4,7,8,5,7,9,9,7,9,6,6,5,8,7,5,5,7,11,7,6,8,7,6,7,5,5,5,7,7,7,9"
  }
}
