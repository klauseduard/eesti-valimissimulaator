import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Küsitluse viga logaritmilisel skaalal: erakonna toetus korrutatakse igas korduses teguriga
 * exp(üleriigiline · z + ringkondlik · e), kus z on erakonna ühine juhuslik suurus kõigis ringkondades ja
 * e on ringkonna oma (mõlemad standardse normaaljaotusega). Üleriigiline osa kirjeldab, et küsitlus
 * eksib erakonna toetuses enamasti kõikjal samas suunas; ringkondlik osa kirjeldab ringkondade väiksemaid
 * valimeid.
 */
data class Küsitluseviga(val üleriigiline: Double, val ringkondlik: Double) {
  companion object {
    /**
     * 2023. aasta viimaste küsitluste (Norstat 03.03.2023 ja Emor märts 2023) tegelik viga valimistulemuse
     * suhtes; arvutus on testis KüsitluseVigaTest. See on mõõdetud samadel valimistel, mida simuleeritakse
     * (valimisisene hinnang). Ringkondlikku osa ei saa teiste valimiste põhjal hinnata, sest ringkondade
     * tulemused on avaandmetena olemas ainult 2023. aasta kohta; üleriigilise osa annab küsitluste
     * koondmudeli tagasitestimine (vt docs/kusitluste-koond.md).
     */
    val MÕÕDETUD_2023 = Küsitluseviga(üleriigiline = 0.156, ringkondlik = 0.251)
  }
}

/**
 * Juhuslik toetus küsitluse vea järgi. Iga erakonna toetus korrutatakse teguriga, mille keskväärtus on 1
 * (log-normaaljaotus), seejärel skaleeritakse ringkonna erakondade toetused tagasi esialgse summani:
 * ühe erakonna alahindamine tähendab teiste ülehindamist, ja küsitluses mitteosalenud valijate osa ei muutu.
 */
fun juhuslikToetus(
  toetusprotsendid: Map<String, Map<Int, Double>>,
  viga: Küsitluseviga,
  juhus: Random,
): Map<String, Map<Int, Double>> {
  if (viga.üleriigiline == 0.0 && viga.ringkondlik == 0.0) return toetusprotsendid
  val parandus = (viga.üleriigiline * viga.üleriigiline + viga.ringkondlik * viga.ringkondlik) / 2.0
  val tegurid = toetusprotsendid.mapValues { (_, toetus) ->
    val ühine = viga.üleriigiline * normaalne(juhus)
    toetus.mapValues { exp(ühine + viga.ringkondlik * normaalne(juhus) - parandus) }
  }
  val ringkonnad = toetusprotsendid.values.flatMap { it.keys }.distinct()
  val uued = toetusprotsendid.mapValues { (erakond, toetus) -> toetus.mapValues { (r, p) -> p * tegurid[erakond]!![r]!! } }
  val skaala = ringkonnad.associateWith { r ->
    val uus = uued.values.sumOf { it[r] ?: 0.0 }
    if (uus > 0.0) toetusprotsendid.values.sumOf { it[r] ?: 0.0 } / uus else 1.0
  }
  return uued.mapValues { (_, toetus) -> toetus.mapValues { (r, p) -> p * skaala[r]!! } }
}
