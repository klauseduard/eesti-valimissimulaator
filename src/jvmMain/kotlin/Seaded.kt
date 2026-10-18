/** Käsurealt antud seaded. */
data class Seaded(
  val küsitleja: Küsitleja,
  /** juhuslikkuse seeme; null tähendab juhuslikku seemet */
  val seeme: Long?,
  /** mitu korda simulatsiooni korrata; null tähendab üht simulatsiooni ilma sageduste failita */
  val kordi: Int?,
  /** Norstati reitingute JSON-fail; null tähendab programmiga kaasas olevat viimast faili */
  val reitingufail: String?,
  /** kas korduvates simulatsioonides arvestatakse küsitluse viga (vt Küsitluseviga) */
  val arvestaViga: Boolean = true,
  /** häälte jaotus kandidaatide vahel: JAOTUS_KALIBREERITUD või JAOTUS_LIHTNE */
  val jaotus: String = JAOTUS_KALIBREERITUD,
)

const val KASUTUSJUHEND = """Kasutamine: ./gradlew run --args='[--pollster=emor] [--seed=N] [--runs=N] [--reitingud=FAIL]'
  --pollster=emor   kasuta Emori andmeid failist Andmed.kt (vaikimisi Norstati andmed)
  --seed=N          juhuslikkuse seeme, et tulemus oleks korratav
  --runs=N          korda simulatsiooni N korda ja kirjuta kandidaatide valituks osutumise sagedused faili
  --reitingud=FAIL  Norstati reitingute JSON-fail reitingud.ee lehelt
  --ilma-veata      ära arvesta korduvates simulatsioonides küsitluse viga
  --jaotus=lihtne   jaga erakonna hääled kandidaatide vahel lihtsa juhusliku jaotusega
                    (vaikimisi 2023. aasta järgi kalibreeritud jaotus, vt docs/kandidaatide-mudel.md)"""

/** Loeb käsurea argumendid; tundmatu või vigase argumendi korral viskab IllegalArgumentException'i. */
fun loeSeaded(args: Array<String>): Seaded {
  var küsitleja = Küsitleja.NORSTAT
  var seeme: Long? = null
  var kordi: Int? = null
  var reitingufail: String? = null
  var arvestaViga = true
  var jaotus = JAOTUS_KALIBREERITUD

  for (argument in args) {
    when {
      argument == "--pollster=emor" -> küsitleja = Küsitleja.EMOR
      argument == "--pollster=norstat" -> küsitleja = Küsitleja.NORSTAT
      argument.startsWith("--seed=") -> seeme = argument.removePrefix("--seed=").toLongOrNull()
        ?: throw IllegalArgumentException("Seeme peab olema täisarv: $argument")
      argument.startsWith("--runs=") -> kordi = argument.removePrefix("--runs=").toIntOrNull()?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("Korduste arv peab olema positiivne täisarv: $argument")
      argument.startsWith("--reitingud=") -> reitingufail = argument.removePrefix("--reitingud=")
      argument == "--ilma-veata" -> arvestaViga = false
      argument.startsWith("--jaotus=") -> jaotus = argument.removePrefix("--jaotus=").also {
        if (it != JAOTUS_KALIBREERITUD && it != JAOTUS_LIHTNE) throw IllegalArgumentException("Tundmatu häälte jaotus: $argument")
      }
      else -> throw IllegalArgumentException("Tundmatu argument: $argument")
    }
  }
  return Seaded(küsitleja, seeme, kordi, reitingufail, arvestaViga, jaotus)
}
