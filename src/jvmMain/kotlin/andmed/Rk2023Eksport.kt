package andmed

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Teisendab 2023. aasta Riigikogu valimiste ametliku tulemuse faili (RESULTS.xml, skeem
 * election-result/rk/v2) kompaktseks JSON-failiks, mida loeb [loeRk2023Tulemus].
 *
 * Käivitamine: ./gradlew ekspordiRk2023
 */
fun main(args: Array<String>) {
  require(args.size == 2) { "Kasutamine: Rk2023EksportKt <RESULTS.xml> <väljundfail.json>" }
  File(args[1]).writeText(ekspordiRk2023(File(args[0]).readText()))
}

/** Tagastab ametliku tulemuse JSON-tekstina; kandidaadid on registreerimisnumbrite järjekorras. */
fun ekspordiRk2023(xml: String): String {
  val dokument = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
    .newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray()))
  val koond = dokument.documentElement.esimene("data").esimene("electionResult")

  val kandidaadid = mutableListOf<Rk2023Kandidaat>()
  for (nimekiri in koond.esimene("votesAndMandates").lapsed()) {
    val erakond = when (nimekiri.localName) {
      "party" -> nimekiri.tekst("name")
      "independentCandidates" -> ÜKSIKKANDIDAADID
      else -> continue
    }
    for (k in nimekiri.esimene("candidates").lapsed("candidate")) {
      kandidaadid.add(
        Rk2023Kandidaat(
          regNr = k.tekst("registrationNumber").toInt(),
          nimi = k.tekst("forename") + " " + k.tekst("surname"),
          erakond = erakond,
          ringkond = k.tekst("districtNumber").toInt(),
          hääli = k.tekst("votes").toInt(),
          mandaat = k.lapsed("mandateType").firstOrNull()?.textContent?.trim(),
        )
      )
    }
  }
  val kehtivaidHääli = koond.esimene("districts").lapsed("district").associate {
    it.tekst("number").toInt() to it.esimene("statistics").tekst("votes").toInt()
  }

  val read = mutableListOf<String>()
  read.add("{")
  read.add("  \"allikas\": ${jsonTekst(RK2023_ALLIKAS)},")
  read.add("  \"kehtivaidHääli\": {" +
    kehtivaidHääli.entries.sortedBy { it.key }.joinToString(", ") { "\"${it.key}\": ${it.value}" } + "},")
  read.add("  \"kandidaadid\": [")
  read.add(kandidaadid.sortedBy { it.regNr }.joinToString(",\n") { k ->
    "    {\"regNr\": ${k.regNr}, \"nimi\": ${jsonTekst(k.nimi)}, \"erakond\": ${jsonTekst(k.erakond)}, " +
      "\"ringkond\": ${k.ringkond}, \"hääli\": ${k.hääli}, \"mandaat\": ${k.mandaat?.let { jsonTekst(it) } ?: "null"}}"
  })
  read.add("  ]")
  read.add("}")
  return read.joinToString("\n") + "\n"
}

private fun jsonTekst(tekst: String): String =
  "\"" + tekst.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun Element.lapsed(nimi: String? = null): List<Element> =
  (0 until childNodes.length).map { childNodes.item(it) }
    .filterIsInstance<Element>()
    .filter { nimi == null || it.localName == nimi }

private fun Element.esimene(nimi: String): Element = lapsed(nimi).first()

private fun Element.tekst(nimi: String): String = esimene(nimi).textContent.trim()
