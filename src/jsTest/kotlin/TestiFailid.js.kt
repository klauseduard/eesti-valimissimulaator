// Node'i testid käivitatakse Gradle'i ehituskaustas, seega otsitakse projekti kausta
// praegusest kaustast ülespoole faili settings.gradle.kts järgi.
private val fs: dynamic = js("process.getBuiltinModule('fs')")
private val path: dynamic = js("process.getBuiltinModule('path')")

private val projektiKaust: String by lazy {
  var kaust: String = js("process.cwd()")
  while (!(fs.existsSync(path.join(kaust, "settings.gradle.kts")) as Boolean)) {
    val ülem: String = path.dirname(kaust)
    check(ülem != kaust) { "Projekti kausta ei leitud" }
    kaust = ülem
  }
  kaust
}

actual fun loeProjektiFail(tee: String): String = fs.readFileSync(path.join(projektiKaust, tee), "utf8") as String
