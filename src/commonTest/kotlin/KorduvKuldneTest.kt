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
      Testiandmed.kandidaadid(), Testiandmed.norstatiToetus(), 40, Random(1), viga = Küsitluseviga.KALIBREERITUD
    )
    val tekst = kokkuvõte.erakondadeMandaadid.entries.sortedBy { it.key }
      .joinToString("\n") { (erakond, mandaadid) -> "$erakond: ${mandaadid.joinToString(",")}" }
    assertEquals(OODATUD, tekst)
  }

  companion object {
    const val OODATUD =
      "Eesti Keskerakond: 21,19,18,15,20,20,20,21,23,21,16,26,21,21,22,17,18,15,26,12,25,14,25,15,19,23,20,16,18,22,16,20,24,17,19,22,18,19,17,17\n" +
      "Eesti Konservatiivne Rahvaerakond: 17,26,24,26,18,23,22,25,20,21,21,22,25,21,23,28,16,28,21,18,19,20,21,30,20,20,22,20,25,23,19,18,23,28,23,29,27,22,24,31\n" +
      "Eesti Reformierakond: 32,28,34,34,34,29,34,31,31,31,42,23,25,26,24,26,42,28,30,40,25,44,26,29,33,34,29,33,36,31,48,40,25,31,29,22,35,28,31,31\n" +
      "Erakond Eesti 200: 13,9,11,13,16,14,11,7,12,13,9,14,12,11,15,14,9,15,11,14,11,10,9,9,11,9,9,15,9,9,8,9,12,9,15,13,8,15,12,7\n" +
      "ISAMAA Erakond: 10,11,8,6,7,8,8,9,7,9,7,9,10,13,9,11,9,7,7,8,9,8,14,10,11,9,13,11,7,10,5,8,11,8,8,7,6,9,9,8\n" +
      "Sotsiaaldemokraatlik Erakond: 8,8,6,7,6,7,6,8,8,6,6,7,8,9,8,5,7,8,6,9,12,5,6,8,7,6,8,6,6,6,5,6,6,8,7,8,7,8,8,7"
  }
}
