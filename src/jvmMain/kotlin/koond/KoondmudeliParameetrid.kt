package koond

import Küsitluseviga
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlin.math.sqrt

/** küsitluste failide erakonnakoodid ja simulaatori erakondade nimed */
val ERAKONNA_KOODID = mapOf(
  "REF" to "Eesti Reformierakond",
  "KESK" to "Eesti Keskerakond",
  "EKRE" to "Eesti Konservatiivne Rahvaerakond",
  "ISAMAA" to "ISAMAA Erakond",
  "SDE" to "Sotsiaaldemokraatlik Erakond",
  "E200" to "Erakond Eesti 200",
  "ROH" to "Erakond Eestimaa Rohelised",
  "PP" to "Erakond Parempoolsed",
  "EUVP" to "EESTIMAA ÜHENDATUD VASAKPARTEI",
)

/** Tagasitestimise lõplikud parameetrid (fail src/jvmMain/resources/kusitlused/koondmudel.json). */
data class Koondmudel(
  val meetod: Meetod,
  val veamudelKehtib: Boolean,
  /** horisont päevades -> parameetrid */
  val parameetrid: Map<Long, Mudeliparameetrid>,
) {
  /** parameetrid horisondil: lineaarne interpolatsioon dispersioonides lähimate horisontide vahel */
  fun parameetrid(horisontPäevi: Long): Mudeliparameetrid {
    val horisondid = parameetrid.keys.sorted()
    val h = horisontPäevi.coerceIn(horisondid.first(), horisondid.last())
    val ülem = horisondid.first { it >= h }
    val alam = horisondid.last { it <= h }
    if (ülem == alam) return parameetrid[ülem]!!
    val w = (h - alam).toDouble() / (ülem - alam)
    val a = parameetrid[alam]!!
    val b = parameetrid[ülem]!!
    fun seg(x: Double, y: Double) = x + w * (y - x)
    return Mudeliparameetrid(
      majaefekt = seg(a.majaefekt, b.majaefekt),
      küsitlejateMajaefekt = (a.küsitlejateMajaefekt.keys + b.küsitlejateMajaefekt.keys).associateWith {
        seg(a.majaefekt(it), b.majaefekt(it))
      },
      ühine = seg(a.ühine, b.ühine),
    )
  }

  /**
   * Küsitluse viga simulatsiooni jaoks: üleriigiline osa on ühise vea ja küsitlejate majaefektide summa
   * (ühe küsitleja korral tema majaefekt, koondi korral majaefektide keskmise dispersioon), ringkondlik osa
   * on 2023. aasta põhjal mõõdetud.
   *
   * @param küsitlejad - küsitlejad, kelle küsitlused hinnangus on; üks küsitleja tähendab üksikküsitlust
   */
  fun viga(küsitlejad: List<String>, horisontPäevi: Long): Küsitluseviga {
    val p = parameetrid(horisontPäevi)
    val majaefekt = küsitlejad.sumOf { p.majaefekt(it) } / (küsitlejad.size * küsitlejad.size)
    val mõõdetud = Küsitluseviga.MÕÕDETUD_2023
    val üleriigiline = sqrt(p.ühine + majaefekt)
    // kui veamudel ei läbinud tagasitesti, kasutatakse suuremat kahest väärtusest (docs/kusitluste-koond.md)
    return Küsitluseviga(
      üleriigiline = if (veamudelKehtib) üleriigiline else maxOf(üleriigiline, mõõdetud.üleriigiline),
      ringkondlik = mõõdetud.ringkondlik,
    )
  }
}

fun loeKoondmudel(json: String): Koondmudel {
  val juur = Json.parseToJsonElement(json).jsonObject
  val horisondid = juur["horisondid"]!!.jsonObject.entries.associate { (h, väärtus) ->
    val o = väärtus.jsonObject
    h.toLong() to Mudeliparameetrid(
      majaefekt = o["majaefekt"]!!.jsonPrimitive.double,
      küsitlejateMajaefekt = o["küsitlejad"]!!.jsonObject.mapValues { it.value.jsonPrimitive.double },
      ühine = o["ühine"]!!.jsonPrimitive.double,
    )
  }
  return Koondmudel(
    meetod = Meetod.valueOf(juur["meetod"]!!.jsonPrimitive.content),
    veamudelKehtib = juur["veamudelKehtib"]!!.jsonPrimitive.boolean,
    parameetrid = horisondid,
  )
}
