package andmed

import mudel.Kandidaat
import java.io.File

/**
 * Laeb andmefailid JVM-i ressurssidest (kaust src/jvmMain/resources) ja annab need ühistele lugejatele
 * (vt Lugejad.kt). Failid on salvestatud UTF-8 kodeeringus.
 */
class AndmeFailideLugeja {

  /** Loeb ressursi teksti, nt "ringkond1.txt" või "reitingud/reitingud-2023-03-03.json". */
  fun loeRessurss(nimi: String): String =
    (AndmeFailideLugeja::class.java.getResourceAsStream("/$nimi") ?: throw IllegalArgumentException("Faili $nimi ei leitud"))
      .use { it.readBytes().decodeToString() }

  /** Määrab kandidaatidele järjekorranumbri erakonna üleriigilises nimekirjas (fail yleriigiline.txt). */
  fun määraÜleriigilisedJärjekorranumbrid(kandidaadid: List<Kandidaat>) {
    määraÜleriigilisedJärjekorranumbrid(kandidaadid, loeRessurss(ÜLERIIGILISTE_NIMEKIRJADE_FAIL))
  }

  fun loeKandidaadikirjed(): List<Kandidaat> =
    loeKandidaadid(
      valimisringkonnad.associate { it.nr to loeRessurss(ringkonnaKandidaatideFail(it.nr)) },
      loeRessurss(ÜLERIIGILISTE_NIMEKIRJADE_FAIL)
    )

  /**
   * Loeb reitingud.ee JSON-failist erakondade toetusprotsendid ringkondades.
   *
   * @param fail - JSON-faili tee; null tähendab programmiga kaasas olevat faili [VAIKIMISI_REITINGUFAIL]
   */
  fun loeJSONiFailistReitingud(fail: String? = null): List<Reiting> =
    loeReitingud(if (fail != null) File(fail).readText() else loeRessurss(VAIKIMISI_REITINGUFAIL))
}
