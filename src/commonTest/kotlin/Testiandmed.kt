import andmed.RK2023_TULEMUSE_FAIL
import andmed.VAIKIMISI_REITINGUFAIL
import andmed.erakonnad
import andmed.loeKandidaadid
import andmed.loeReitingud
import andmed.loeRk2023Tulemus
import andmed.reitingudToetusprotsentidena
import andmed.ringkonnaKandidaatideFail
import andmed.valimisringkonnad
import andmed.ÜLERIIGILISTE_NIMEKIRJADE_FAIL
import mudel.Kandidaat

/** Loeb faili teksti; tee on suhteline projekti kausta suhtes. Iga platvorm realiseerib selle ise. */
expect fun loeProjektiFail(tee: String): String

/** JVM-i ressursside kaust, kus on programmi andmefailid */
const val RESSURSID = "src/jvmMain/resources"

/** testide ühised andmed, loetud samadest failidest kui programmis */
object Testiandmed {
  val kandidaadid: List<Kandidaat> by lazy {
    loeKandidaadid(
      valimisringkonnad.associate { it.nr to loeProjektiFail("$RESSURSID/${ringkonnaKandidaatideFail(it.nr)}") },
      loeProjektiFail("$RESSURSID/$ÜLERIIGILISTE_NIMEKIRJADE_FAIL")
    )
  }

  /** kandidaatide koopiad, et testid ei mõjutaks üksteist */
  fun kandidaadid(): List<Kandidaat> = kandidaadid.map { it.copy() }

  fun norstatiToetus(fail: String = VAIKIMISI_REITINGUFAIL): Map<String, Map<Int, Double>> =
    reitingudToetusprotsentidena(loeReitingud(loeProjektiFail("$RESSURSID/$fail")))

  val emoriToetus: Map<String, Map<Int, Double>> get() = erakonnad.associate { it.nimetus to it.toetusprotsentValimisringkonnas }

  val rk2023 by lazy { loeRk2023Tulemus(loeProjektiFail("$RESSURSID/$RK2023_TULEMUSE_FAIL")) }
}
