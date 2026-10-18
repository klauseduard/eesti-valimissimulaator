import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToetuseMuutmiseTest {

  private val kehtivaidHääli = mapOf(1 to 1000, 2 to 3000)
  private val toetus = mapOf(
    "A" to mapOf(1 to 40.0, 2 to 20.0),
    "B" to mapOf(1 to 30.0, 2 to 50.0),
    "C" to mapOf(1 to 10.0, 2 to 10.0),
  )

  private fun lähedal(oodatud: Double, tegelik: Double, sõnum: String = "") =
    assertTrue(abs(oodatud - tegelik) < 1e-9, "$sõnum: $oodatud != $tegelik")

  @Test
  fun `üleriigiline toetus on ringkondade toetuste kaalutud keskmine`() {
    // (40 * 1000 + 20 * 3000) / 4000 = 25
    lähedal(25.0, üleriigilineToetus(toetus["A"]!!, kehtivaidHääli))
  }

  @Test
  fun `üleriigilise toetuse muutmine korrutab erakonna toetuse igas ringkonnas sama teguriga`() {
    val uus = muudaÜleriigilistToetust(toetus, "A", 30.0, kehtivaidHääli)
    lähedal(30.0, üleriigilineToetus(uus["A"]!!, kehtivaidHääli))
    lähedal(48.0, uus["A"]!![1]!!, "ringkond 1")
    lähedal(24.0, uus["A"]!![2]!!, "ringkond 2")
  }

  @Test
  fun `teiste erakondade toetus muutub võrdeliselt ja ringkonna summa ei muutu`() {
    val uus = muudaÜleriigilistToetust(toetus, "A", 30.0, kehtivaidHääli)
    for (ringkond in listOf(1, 2)) {
      lähedal(toetus.values.sumOf { it[ringkond]!! }, uus.values.sumOf { it[ringkond]!! }, "ringkond $ringkond")
    }
    // ringkond 1: teiste summa 40 -> 32, seega B 30 -> 24 ja C 10 -> 8
    lähedal(24.0, uus["B"]!![1]!!)
    lähedal(8.0, uus["C"]!![1]!!)
  }

  @Test
  fun `erakonna toetus ei ületa ringkonna summat`() {
    val uus = muudaÜleriigilistToetust(toetus, "C", 90.0, kehtivaidHääli)
    for (ringkond in listOf(1, 2)) {
      lähedal(80.0, uus["C"]!![ringkond]!!, "ringkond $ringkond")
      lähedal(0.0, uus["A"]!![ringkond]!!, "ringkond $ringkond")
    }
    assertEquals(80.0, üleriigilineToetus(uus["C"]!!, kehtivaidHääli))
  }
}
