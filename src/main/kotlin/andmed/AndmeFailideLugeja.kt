package andmed

import com.google.gson.Gson
import mudel.Erakond
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

  fun loeJSONiFailistReitingud(): MutableList<Reiting> {
    // Algoritmi kirjeldus:
    // JSON-fail reitingud/reitingud-2023-02-20.json sisaldab erakondade reitinguid ringkondades.
    // 1. ringkonna andmed on elemendis .props.chartData.data[16][-1], kus -1 osutab tagantpoolt esimesele elemendile.
    // 2. ringkonna andmed on elemendis .props.chartData.data[17][-1] jne.
    // Viimaks, 12. ringkonna andmed on elemendis .props.chartData.data[27][-1]
    // Iga ringkonna andmed on struktuuris
    // [
    //  "24.01-20.02.2023",
    //  "14.8%",
    //  "32.5%",
    //  "32.4%",
    //  "7.5%",
    //  "3.4%",
    //  "1.3%",
    //  "7.3%",
    //  "0.4%"
    // ],
    // kus esimesel real on kuupäevavahemik ja järgmisel kaheksal real kaheksa erakonna reitingud protsentides.
    // Loe andmed sellest failist andmestruktuuri.

    // Algoritm:
    // 1. loe failist andmed
    // 2. loe andmed ringkondade kaupa
    // 3. loe andmed erakondade kaupa
    // 4. salvesta andmed andmebaasi

    val fileName = "reitingud/reitingud-2023-03-03.json"
    val inputStream = AndmeFailideLugeja::class.java.getResourceAsStream("/$fileName")
    val reader = BufferedReader(InputStreamReader(inputStream))

    val gson = Gson()

    val reitingud = gson.fromJson(reader, Reitingud::class.java)

    val reitingudList = mutableListOf<Reiting>()

    for (i in 0..11) {
      val ringkonnaReitingud = reitingud.props.chartData.data[i+16].last()
      // example value of ringkonnaReitingud:
      // [
      //  "24.01-20.02.2023",
      //  "14.8%",
      //  "32.5%",
      //  "32.4%",
      //  "7.5%",
      //  "3.4%",
      //  "1.3%",
      //  "7.3%",
      //  "0.4%"
      // ]


      for (j in 0..7) {
        val erakonnaNimi = reitingud.props.chartData.labels[j]

        val pikkus = ringkonnaReitingud[j+1].length
        // kui pikkus on null, on reiting 0.0
        if (pikkus == 0) {
          reitingudList.add(Reiting(erakonnaNimi, 0.0, i+1))
        } else {
          val reiting = ringkonnaReitingud[j + 1].substring(0, ringkonnaReitingud[j + 1].length - 1).toDouble()
          println("$i-$j : $erakonnaNimi $reiting")
          reitingudList.add(Reiting(erakonnaNimi, reiting, i + 1))
        }
      }
    }

    reitingudList.forEach {
      println("${it.erakonnaNimi}, ${it.reiting}, ${it.ringkond}")
    }

    return reitingudList


  }


}

class Reitingud {
  val props = Props()
}

class Props {
  val chartData = ChartData()
}

class ChartData {
  val data = mutableListOf<Array<Array<String>>>()
  var labels = mutableListOf<String>(
    KESKERAKOND,
    KONSERVATIIVID,
    REFORMIERAKOND,
    ISAMAA_ERAKOND,
    SDE,
    ROHELISED,
    E200,
    PAREMPOOLSED)
}

data class Reiting(val erakonnaNimi: String, val reiting: Double, val ringkond: Int)

