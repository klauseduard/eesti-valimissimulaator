import andmed.ekspordiRk2023
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class Rk2023EksportTest {

  @Test
  fun `hoidlas olev 2023 tulemuse JSON-fail vastab ametlikule XML-failile`() {
    val oodatud = ekspordiRk2023(File("src/jvmTest/resources/rk2023/RESULTS.xml").readText())
    assertEquals(oodatud, File("src/jvmMain/resources/rk2023/tulemus.json").readText(),
      "fail on aegunud; loo see uuesti käsuga ./gradlew ekspordiRk2023")
  }
}
