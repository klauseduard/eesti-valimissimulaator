package andmed

import mudel.Kandidaat
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Klass, mis laeb tekstifailidest kandidaatide andmed.
 * Failid on salvestatud UTF-8 kodeeringus projekti kausta resources.
 *
 * Failid on salvestatud Riigikogu valimiste veebilehtedelt https://rk2023.valimised.ee/et/candidates
 * ja andmed neis on esitatud järgmisel kujul:
 *
 * erakonna nimi
 * Reg nr 1 Nimi Nimi
 * Reg nr 2 Nimi Nimi
 * Reg nr 3 Nimi Nimi
 *
 * erakonna nimi
 * Reg nr 1 Nimi Nimi
 * Reg nr 2 Nimi Nimi
 * Reg nr 3 Nimi Nimi
 *
 * jne.
 */
class AndmeFailideLugeja {
  /**
   * Loeb failist kandidaatide andmed.
   */
  private fun loeFailistKandidaadid(fileName: String, ringkond: Int): List<Kandidaat> {
    val kandidaadid = mutableListOf<Kandidaat>()

    /* ava fail projekti kaustast resources/ */
    val inputStream = AndmeFailideLugeja::class.java.getResourceAsStream("/$fileName")
    val reader = BufferedReader(InputStreamReader(inputStream))

    var erakond: String? = null

    /* loe failist ükshaaval kõik read ja töötle neid */
    reader.forEachLine { line ->
      when {
        line.startsWith("Reg nr") -> {


          val number = line.substring(7).split(" ").first()
          val nimi = line.substring(7+ number.length).trim()

          // kui erakond on määramata, anna viga
          if (erakond == null) {
            throw RuntimeException("Kandidaadil $nimi puudub erakond")
          }

          kandidaadid.add(Kandidaat(nimi, erakond!!, ringkond ))
        }
        line.startsWith("Kandidaate - ") -> {
          // ignoreeri seda rida
        }
        line.isBlank() -> {
          // ignoreeri seda rida
        }
        else -> {
          // loe erakonna nimi
          erakond = line.trim()
        }
      }
    }
    reader.close()
    return kandidaadid
  }


  private fun loeYleriigiline(kandidaadid : List<Kandidaat>) : Unit {

    val inputStream = AndmeFailideLugeja::class.java.getResourceAsStream("/yleriigiline.txt")
    val reader = BufferedReader(InputStreamReader(inputStream))


    var erakond: String? = null

    var i = 0

    reader.forEachLine {
      line ->
      when {
        line.isBlank() -> {
          // ignoreeri seda rida
        }
        line.startsWith("#") -> {
          erakond = line.substring(1).trim()
          i = 1
        }
        else -> {
          val nimi = line.trim()

          if (erakond == null) {
            throw RuntimeException("Kandidaadil $nimi puudub erakond")
          }

          val kandidaat = kandidaadid.find { it.nimi == nimi && it.erakond == erakond }
          kandidaat!!.jrkNr = i++
        }
      }
    }




  }



  fun loeKandidaadikirjed(): List<Kandidaat> {

    // liida kõik kandidaadid ühte listi
    val kandidaadid =
      loeFailistKandidaadid("ringkond1.txt", 1) +
      loeFailistKandidaadid("ringkond2.txt", 2) +
      loeFailistKandidaadid("ringkond3.txt", 3) +
      loeFailistKandidaadid("ringkond4.txt", 4) +
      loeFailistKandidaadid("ringkond5.txt", 5) +
      loeFailistKandidaadid("ringkond6.txt", 6) +
      loeFailistKandidaadid("ringkond7.txt", 7) +
      loeFailistKandidaadid("ringkond8.txt", 8) +
      loeFailistKandidaadid("ringkond9.txt", 9) +
      loeFailistKandidaadid("ringkond10.txt", 10) +
      loeFailistKandidaadid("ringkond11.txt", 11) +
      loeFailistKandidaadid("ringkond12.txt", 12)

    loeYleriigiline(kandidaadid)

    return kandidaadid
  }


}