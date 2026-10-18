import andmed.AndmeFailideLugeja
import koond.loeValimised
import koond.tagasitesti
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Hoidlas olev tagasitesti aruanne ja koondmudeli parameetrid peavad vastama küsitluste andmetele. */
class TagasitestFailidTest {

  @Test
  fun `tagasitesti aruanne ja parameetrid on ajakohased`() {
    val lugeja = AndmeFailideLugeja()
    val tulemus = tagasitesti(loeValimised(lugeja), lugeja)
    val teade = "fail on aegunud; loo see uuesti käsuga ./gradlew tagasitest"
    assertEquals(tulemus.parameetridJson, File("src/jvmMain/resources/kusitlused/koondmudel.json").readText(), teade)
    assertEquals(tulemus.aruanne, File("docs/tagasitestimine.md").readText(), teade)
  }
}
