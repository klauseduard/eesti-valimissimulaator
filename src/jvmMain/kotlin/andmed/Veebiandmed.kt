package andmed

import java.io.File

/**
 * Kirjutab veebilehe jaoks küsitluste toetusprotsendid kompaktsesse JSON-faili. Norstati failid on
 * reitingud.ee lehe täielikud andmed (umbes 2 MB igaüks); veebileht vajab neist ainult 12 × 8 arvu.
 * Erakonnad on andmeallika järjekorras, et sama seemne korral annaks veebileht sama tulemuse kui
 * käsurea programm.
 *
 * Käivitamine: ./gradlew veebiandmed
 */
fun main(args: Array<String>) {
  require(args.size == 1) { "Kasutamine: VeebiandmedKt <väljundkaust>" }
  val kaust = File(args[0]).apply { mkdirs() }
  File(kaust, "toetused.json").writeText(veebiandmed(AndmeFailideLugeja()))
}

/** küsitluste andmed: tunnus, küsitleja, avaldamise kuupäev ja reitingufail (null: Emori andmed failist Andmed.kt) */
val KÜSITLUSED = listOf(
  Triple("norstat-2023-02-20", "Norstat", "reitingud/reitingud-2023-02-20.json"),
  Triple("norstat-2023-02-27", "Norstat", "reitingud/reitingud-2023-02-27.json"),
  Triple("norstat-2023-03-03", "Norstat", "reitingud/reitingud-2023-03-03.json"),
  Triple("emor-2023-03", "Emor", null),
)

fun veebiandmed(lugeja: AndmeFailideLugeja): String {
  val küsitlused = KÜSITLUSED.map { (tunnus, küsitleja, fail) ->
    val toetus = if (fail != null) reitingudToetusprotsentidena(loeReitingud(lugeja.loeRessurss(fail)))
      else erakonnad.associate { it.nimetus to it.toetusprotsentValimisringkonnas }
    val erakonnad = toetus.keys.toList()
    val protsendid = erakonnad.joinToString(",\n      ") { erakond ->
      valimisringkonnad.joinToString(", ", "[", "]") { (toetus[erakond]!![it.nr] ?: 0.0).toString() }
    }
    """    "$tunnus": {
    "küsitleja": "$küsitleja",
    "erakonnad": [${erakonnad.joinToString(", ") { "\"$it\"" }}],
    "protsendid": [
      $protsendid
    ]
  }"""
  }
  return "{\n" + küsitlused.joinToString(",\n") { it.replaceFirst("    \"", "  \"") } + "\n}\n"
}
