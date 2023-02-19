package mudel

data class Erakond(val nimetus: String, val toetusprotsentValimisringkonnas: Map<Int, Double>)

data class Valimisringkond(val nr: Int, val nimetus: String, val mandaatideArv: Int)

data class Kandidaat(val nimi: String, val erakond: String, val valimisringkond: Int, var hääli: Int = 0, var jrkNr: Int = 0)