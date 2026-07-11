package raport

import andmed.MANDAATE_KOKKU
import andmed.valimisringkonnad
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Koondab kausta runs/ salvestatud simulatsioonitulemused üheks HTML-raportiks
 * (raport.html), mida saab avada otse brauseris. Raport näitab, kuhu korduvate
 * käivituste tõenäosused koonduvad: erakondade kohtade vahemikud, koalitsioonide
 * enamuse tõenäosused ja kandidaatide valituks osutumise sagedused.
 */

private data class ValitudKandidaat(
  val nimi: String,
  val erakond: String,
  val ringkond: Int,
  val mandaadiliik: String,
)

/** Üks käivitus = ühe runs/ faili sisu. */
private data class Jooks(val fail: String, val valitud: List<ValitudKandidaat>)

// vanades failides mandaadiliigi välja ei ole, seetõttu on see mustris valikuline
private val KANDIDAADIREA_MUSTER = Regex(
  """Kandidaat\(nimi=(.+?), erakond=(.+?), valimisringkond=(\d+), hääli=\d+, jrkNr=\d+(?:, mandaadiliik=(\w*))?\)""")

fun genereeriRaport() {
  val failid = File("runs").listFiles { f -> f.name.endsWith(".txt") }?.sortedBy { it.name } ?: emptyList()
  if (failid.isEmpty()) {
    println("Kaustas runs/ ei ole ühtegi tulemusfaili. Käivita enne simulatsioon (nt run-n-times.sh).")
    return
  }

  val jooksud = failid.mapNotNull { fail ->
    val valitud = fail.readLines().mapNotNull { rida ->
      KANDIDAADIREA_MUSTER.find(rida)?.let {
        ValitudKandidaat(it.groupValues[1], it.groupValues[2], it.groupValues[3].toInt(), it.groupValues[4])
      }
    }
    if (valitud.isEmpty()) null else Jooks(fail.name, valitud)
  }

  val html = koostaHtml(jooksud)
  File("raport.html").writeText(html)
  println("Raport kirjutatud faili raport.html (${jooksud.size} käivitust, ${failid.size} faili)")
}

/** erakondade enamlevinud värvid meediagraafikas (ligikaudsed) */
private val ERAKONDADE_VÄRVID = mapOf(
  "Eesti Reformierakond" to "#ffde00",
  "Eesti Keskerakond" to "#00805a",
  "Eesti Konservatiivne Rahvaerakond" to "#2b2b2b",
  "Sotsiaaldemokraatlik Erakond" to "#d71920",
  "ISAMAA Erakond" to "#00aeef",
  "Erakond Eesti 200" to "#173f4e",
)

private fun värv(erakond: String) = ERAKONDADE_VÄRVID[erakond] ?: "#888888"

private fun jsonString(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun koostaHtml(jooksud: List<Jooks>): String {
  val käivitusi = jooksud.size

  // erakondade kohtade arv igas jooksus
  val erakonnad = jooksud.flatMap { j -> j.valitud.map { it.erakond } }.distinct()
    .sortedByDescending { e -> jooksud.sumOf { j -> j.valitud.count { it.erakond == e } } }
  val kohadJooksudes: Map<String, List<Int>> = erakonnad.associateWith { e ->
    jooksud.map { j -> j.valitud.count { it.erakond == e } }
  }

  // koalitsioonide (kuni 3 erakonda) enamuse tõenäosused
  val enamus = MANDAATE_KOKKU / 2 + 1
  val kombinatsioonid = mutableListOf<List<String>>()
  for (i in erakonnad.indices) {
    kombinatsioonid.add(listOf(erakonnad[i]))
    for (j in i + 1 until erakonnad.size) {
      kombinatsioonid.add(listOf(erakonnad[i], erakonnad[j]))
      for (k in j + 1 until erakonnad.size) {
        kombinatsioonid.add(listOf(erakonnad[i], erakonnad[j], erakonnad[k]))
      }
    }
  }
  val koalitsioonid = kombinatsioonid.mapNotNull { kombinatsioon ->
    val õnnestumisi = jooksud.indices.count { r ->
      kombinatsioon.sumOf { e -> kohadJooksudes[e]!![r] } >= enamus
    }
    if (õnnestumisi > 0) kombinatsioon to õnnestumisi.toDouble() / käivitusi else null
  }
  // jäta välja koalitsioonid, mille mõni alamhulk saavutab enamuse sama kindlalt --
  // näiteks kolmik on huvitav ainult siis, kui ükski tema paaridest sama hästi ei toimi
  val huvitavadKoalitsioonid = koalitsioonid.filter { (parteid, tõenäosus) ->
    koalitsioonid.none { (teised, teineTõenäosus) ->
      teised.size < parteid.size && parteid.containsAll(teised) && teineTõenäosus >= tõenäosus
    }
  }.sortedByDescending { it.second }

  // kandidaatide valituks osutumise sagedused
  data class KandidaadiStatistika(
    val nimi: String, val erakond: String, val ringkond: Int,
    var valitud: Int = 0, var ringkonnamandaate: Int = 0, var kompensatsioone: Int = 0)
  val kandidaadid = linkedMapOf<String, KandidaadiStatistika>()
  for (jooks in jooksud) {
    for (v in jooks.valitud) {
      val stat = kandidaadid.getOrPut("${v.nimi}|${v.erakond}|${v.ringkond}") {
        KandidaadiStatistika(v.nimi, v.erakond, v.ringkond)
      }
      stat.valitud++
      when (v.mandaadiliik) {
        "ringkond" -> stat.ringkonnamandaate++
        "kompensatsioon" -> stat.kompensatsioone++
      }
    }
  }
  val kandidaadidSorteeritult = kandidaadid.values.sortedWith(
    compareBy({ erakonnad.indexOf(it.erakond) }, { -it.valitud }))

  // "kaalukausil" = valituks osutumine ei ole ei kindel ega välistatud
  val kaalukausil = kandidaadidSorteeritult
    .filter { it.valitud in 1 until käivitusi }
    .map { it to it.valitud.toDouble() / käivitusi }
    .filter { it.second in 0.05..0.95 }
    .sortedByDescending { it.second }
    .take(30)

  // ringkondade kaupa: erakonna keskmine kohtade arv ringkonnas
  val ringkonnaKeskmised = valimisringkonnad.associate { rk ->
    rk.nr to erakonnad.associateWith { e ->
      jooksud.sumOf { j -> j.valitud.count { it.erakond == e && it.ringkond == rk.nr } }.toDouble() / käivitusi
    }
  }

  fun protsent(x: Double) = String.format("%.0f%%", x * 100)
  fun keskmine(l: List<Int>) = l.average()
  fun mediaan(l: List<Int>): Int { val s = l.sorted(); return s[s.size / 2] }

  val erakondadeNimedJs = erakonnad.joinToString(", ") { jsonString(it) }
  val värvidJs = erakonnad.joinToString(", ") { jsonString(värv(it)) }
  val vahemikudJs = erakonnad.joinToString(", ") { e ->
    val l = kohadJooksudes[e]!!; "[${l.min()}, ${l.max()}]"
  }
  val keskmisedJs = erakonnad.joinToString(", ") { e -> String.format("%.1f", keskmine(kohadJooksudes[e]!!)) }

  val kaalukausilNimedJs = kaalukausil.joinToString(", ") { (k, _) -> jsonString("${k.nimi} (${k.erakond.lühend()}, rk ${k.ringkond})") }
  val kaalukausilVäärtusedJs = kaalukausil.joinToString(", ") { (_, p) -> String.format("%.1f", p * 100) }
  val kaalukausilVärvidJs = kaalukausil.joinToString(", ") { (k, _) -> jsonString(värv(k.erakond)) }

  val erakondadeTabel = erakonnad.joinToString("\n") { e ->
    val l = kohadJooksudes[e]!!
    "<tr><td><span class=\"täpp\" style=\"background:${värv(e)}\"></span>$e</td>" +
      "<td>${String.format("%.1f", keskmine(l))}</td><td>${mediaan(l)}</td><td>${l.min()}–${l.max()}</td>" +
      "<td>${jaotuseRida(l)}</td></tr>"
  }

  val koalitsioonideTabel = huvitavadKoalitsioonid.take(15).joinToString("\n") { (parteid, p) ->
    val kohti = parteid.sumOf { keskmine(kohadJooksudes[it]!!) }
    "<tr><td>${parteid.joinToString(" + ") { it.lühend() }}</td>" +
      "<td>${String.format("%.1f", kohti)}</td><td>${protsent(p)}</td></tr>"
  }

  val kandidaatideTabelid = erakonnad.joinToString("\n") { e ->
    val read = kandidaadidSorteeritult.filter { it.erakond == e && it.valitud > 0 }.joinToString("\n") { k ->
      val p = k.valitud.toDouble() / käivitusi
      "<tr><td>${k.nimi}</td><td>${k.ringkond}</td><td>${protsent(p)}</td>" +
        "<td>${protsent(k.ringkonnamandaate.toDouble() / käivitusi)}</td>" +
        "<td>${protsent(k.kompensatsioone.toDouble() / käivitusi)}</td></tr>"
    }
    """
    <details><summary><span class="täpp" style="background:${värv(e)}"></span>$e</summary>
    <table><thead><tr><th>Kandidaat</th><th>Ringkond</th><th>Valitud</th><th>..ringkonnast</th><th>..kompensatsioonina</th></tr></thead>
    <tbody>$read</tbody></table></details>"""
  }

  val ringkondadeTabel = valimisringkonnad.joinToString("\n") { rk ->
    val rakud = erakonnad.joinToString("") { e ->
      "<td>${String.format("%.1f", ringkonnaKeskmised[rk.nr]!![e]!!)}</td>"
    }
    "<tr><td>${rk.nr}. ${rk.nimetus}</td><td>${rk.mandaatideArv}</td>$rakud</tr>"
  }
  val ringkonnaPäis = erakonnad.joinToString("") { "<th>${it.lühend()}</th>" }

  val genereeritud = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date())

  return """<!DOCTYPE html>
<html lang="et">
<head>
<meta charset="UTF-8">
<title>Valimissimulaatori koondraport</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<style>
  body { font-family: "Segoe UI", Arial, sans-serif; margin: 2em auto; max-width: 1000px; color: #222; }
  h1 { font-size: 1.6em; } h2 { font-size: 1.2em; margin-top: 2em; }
  .selgitus { color: #555; font-size: 0.92em; }
  table { border-collapse: collapse; margin: 1em 0; width: 100%; }
  th, td { border: 1px solid #ddd; padding: 0.4em 0.7em; text-align: left; font-size: 0.92em; }
  th { background: #f2f2f2; }
  .täpp { display: inline-block; width: 0.8em; height: 0.8em; border-radius: 50%; margin-right: 0.5em; }
  details { margin: 0.5em 0; } summary { cursor: pointer; font-weight: 600; padding: 0.3em 0; }
  canvas { max-height: 420px; }
</style>
</head>
<body>
<h1>Valimissimulaatori koondraport</h1>
<p class="selgitus">Koostatud $genereeritud, aluseks $käivitusi simulatsioonikäivitust kaustas runs/.
Tulemused on tõenäosuslikud: iga käivitus loosib küsitlusvea ja häälte jaotuse kandidaatide vahel uuesti.</p>

<h2>Erakondade mandaadid käivituste lõikes</h2>
<p class="selgitus">Tulp näitab kohtade vahemikku (miinimum–maksimum) üle kõigi käivituste, punkt keskmist.</p>
<canvas id="kohadChart"></canvas>
<table>
<thead><tr><th>Erakond</th><th>Keskmine</th><th>Mediaan</th><th>Vahemik</th><th>Jaotus (kohti: mitmes käivituses)</th></tr></thead>
<tbody>
$erakondadeTabel
</tbody>
</table>

<h2>Koalitsioonide enamuse tõenäosus (≥ $enamus kohta)</h2>
<p class="selgitus">Kuni kolme erakonna kombinatsioonid; loetletud pole kombinatsioone, mille mõni väiksem alamhulk
saavutab enamuse sama kindlalt.</p>
<table>
<thead><tr><th>Koalitsioon</th><th>Kohti keskmiselt</th><th>Enamus käivitustest</th></tr></thead>
<tbody>
$koalitsioonideTabel
</tbody>
</table>

<h2>Kaalukausil kandidaadid</h2>
<p class="selgitus">Kandidaadid, kelle valituks osutumine ei ole ei kindel ega välistatud
(valituks osutumise sagedus 5–95%). Just nende saatust võib üksikute häälte taktikaline liikumine mõjutada.</p>
<canvas id="kaalukausilChart"></canvas>

<h2>Kandidaatide valituks osutumise sagedus erakondade kaupa</h2>
$kandidaatideTabelid

<h2>Ringkondade lõikes (erakonna keskmine kohtade arv)</h2>
<table>
<thead><tr><th>Ringkond</th><th>Mandaate</th>$ringkonnaPäis</tr></thead>
<tbody>
$ringkondadeTabel
</tbody>
</table>

<script>
const erakonnad = [$erakondadeNimedJs];
const värvid = [$värvidJs];
new Chart(document.getElementById('kohadChart'), {
  data: {
    labels: erakonnad,
    datasets: [
      { type: 'bar', label: 'Kohtade vahemik (min–max)', data: [$vahemikudJs],
        backgroundColor: värvid.map(v => v + 'aa'), borderColor: värvid, borderWidth: 1 },
      { type: 'scatter', label: 'Keskmine', data: [$keskmisedJs],
        backgroundColor: '#000', pointRadius: 5, pointStyle: 'rectRot' }
    ]
  },
  options: { scales: { y: { beginAtZero: true, title: { display: true, text: 'mandaate' } } } }
});
new Chart(document.getElementById('kaalukausilChart'), {
  type: 'bar',
  data: {
    labels: [$kaalukausilNimedJs],
    datasets: [{ label: 'Valituks osutumise sagedus (%)', data: [$kaalukausilVäärtusedJs],
                 backgroundColor: [$kaalukausilVärvidJs] }]
  },
  options: { indexAxis: 'y', scales: { x: { min: 0, max: 100 } },
             plugins: { legend: { display: false } } }
});
</script>
</body>
</html>
"""
}

/** kohtade arvu jaotus kujul "31: 12x, 32: 45x, ..." */
private fun jaotuseRida(kohad: List<Int>): String =
  kohad.groupingBy { it }.eachCount().toSortedMap()
    .entries.joinToString(", ") { (kohti, kordi) -> "$kohti: ${kordi}x" }

private fun String.lühend(): String = when (this) {
  "Eesti Reformierakond" -> "REF"
  "Eesti Keskerakond" -> "KESK"
  "Eesti Konservatiivne Rahvaerakond" -> "EKRE"
  "Sotsiaaldemokraatlik Erakond" -> "SDE"
  "ISAMAA Erakond" -> "ISAMAA"
  "Erakond Eesti 200" -> "E200"
  else -> this
}
