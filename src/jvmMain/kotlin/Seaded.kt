/** Käsurealt antud seaded. */
data class Seaded(
  val küsitleja: Küsitleja,
  /** juhuslikkuse seeme; null tähendab juhuslikku seemet */
  val seeme: Long?,
  /** mitu korda simulatsiooni korrata; null tähendab üht simulatsiooni ilma sageduste failita */
  val kordi: Int?,
  /** Norstati reitingute JSON-fail; null tähendab programmiga kaasas olevat viimast faili */
  val reitingufail: String?,
)

const val KASUTUSJUHEND = """Kasutamine: ./gradlew run --args='[--pollster=emor] [--seed=N] [--runs=N] [--reitingud=FAIL]'
  --pollster=emor   kasuta Emori andmeid failist Andmed.kt (vaikimisi Norstati andmed)
  --seed=N          juhuslikkuse seeme, et tulemus oleks korratav
  --runs=N          korda simulatsiooni N korda ja kirjuta kandidaatide valituks osutumise sagedused faili
  --reitingud=FAIL  Norstati reitingute JSON-fail reitingud.ee lehelt"""

/** Loeb käsurea argumendid; tundmatu või vigase argumendi korral viskab IllegalArgumentException'i. */
fun loeSeaded(args: Array<String>): Seaded {
  var küsitleja = Küsitleja.NORSTAT
  var seeme: Long? = null
  var kordi: Int? = null
  var reitingufail: String? = null

  for (argument in args) {
    when {
      argument == "--pollster=emor" -> küsitleja = Küsitleja.EMOR
      argument == "--pollster=norstat" -> küsitleja = Küsitleja.NORSTAT
      argument.startsWith("--seed=") -> seeme = argument.removePrefix("--seed=").toLongOrNull()
        ?: throw IllegalArgumentException("Seeme peab olema täisarv: $argument")
      argument.startsWith("--runs=") -> kordi = argument.removePrefix("--runs=").toIntOrNull()?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("Korduste arv peab olema positiivne täisarv: $argument")
      argument.startsWith("--reitingud=") -> reitingufail = argument.removePrefix("--reitingud=")
      else -> throw IllegalArgumentException("Tundmatu argument: $argument")
    }
  }
  return Seaded(küsitleja, seeme, kordi, reitingufail)
}
