import andmed.AndmeFailideLugeja
import kandidaadid.kandidaaditest
import kandidaadid.loeNimekirjad
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Kandidaadimudeli aruanne, parameetrid ja koodis olevad konstandid peavad vastama 2023. aasta andmetele. */
class KandidaadimudelFailidTest {

  @Test
  fun `kandidaadimudeli aruanne, parameetrid ja konstandid on ajakohased`() {
    val (aruanne, json) = kandidaaditest(loeNimekirjad(AndmeFailideLugeja()))
    val teade = "fail on aegunud; loo see uuesti käsuga ./gradlew kandidaaditest"
    assertEquals(json, File("src/jvmMain/resources/kandidaadimudel.json").readText(), teade)
    assertEquals(aruanne, File("docs/kandidaatide-mudeli-test.md").readText(), teade)
    val p = Json.parseToJsonElement(json).jsonObject
    val k = Positsioonimudel.KALIBREERITUD_2023
    assertEquals(listOf(k.b, k.c, k.d, k.kappa), listOf("b", "c", "d", "kappa").map { p[it]!!.jsonPrimitive.double })
  }
}
